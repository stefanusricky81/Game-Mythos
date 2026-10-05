import { readFileSync } from "node:fs";
import { after, before, describe, test } from "node:test";
import assert from "node:assert/strict";
import { deleteApp, initializeApp } from "firebase/app";
import { connectAuthEmulator, getAuth, signInAnonymously, signOut } from "firebase/auth";
import { connectFunctionsEmulator, getFunctions, httpsCallable } from "firebase/functions";

/**
 * Phase 12 end-to-end tests through the REAL Firebase client SDK against the Auth + Functions + Firestore
 * emulators: the actual onCall wrappers, verified auth context, HttpsError mapping, transactions and the
 * reward outbox. Run:  npm test   (inside e2e/, needs JDK 21+ for the Firestore emulator).
 *
 * Time-dependent parts (World Boss / event windows come from the catalog) are skipped, not failed, if the
 * real date is outside the catalog window. The 16s wait is the server's minimum battle time.
 */
const catalog = JSON.parse(readFileSync(new URL("../functions/src/game/gameCatalog.json", import.meta.url), "utf8"));
const DECK = { heroId: catalog.starter.heroId, cardIds: catalog.starter.deck };
const MIN_BATTLE_WAIT_MS = 16_000;
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const run = Math.random().toString(36).slice(2, 8);
let counter = 0;
const rid = () => `e2e-${run}-${String(++counter).padStart(6, "0")}`;

const makeClient = (name) => {
  const app = initializeApp({ projectId: "demo-mythos", apiKey: "demo-key" }, `${name}-${run}`);
  const auth = getAuth(app);
  connectAuthEmulator(auth, "http://127.0.0.1:9099", { disableWarnings: true });
  const functions = getFunctions(app, "asia-southeast1");
  connectFunctionsEmulator(functions, "127.0.0.1", 5001);
  return {
    app, auth,
    call: async (fn, data) => (await httpsCallable(functions, fn)(data)).data,
    signIn: async () => (await signInAnonymously(auth)).user.uid,
  };
};

const codeOf = async (promise) => {
  try { await promise; return null; } catch (e) { return e.code; }
};

let alice;
let bob;
let aliceUid;
let bobUid;

before(() => {
  alice = makeClient("alice");
  bob = makeClient("bob");
});

after(async () => {
  await deleteApp(alice.app);
  await deleteApp(bob.app);
});

describe("identity and request security", () => {
  test("an unauthenticated request is rejected", async () => {
    await signOut(alice.auth);
    assert.equal(await codeOf(alice.call("getArenaState", {})), "functions/unauthenticated");
  });

  test("Firebase Anonymous Auth gives a stable uid that the server derives from the auth context", async () => {
    aliceUid = await alice.signIn();
    bobUid = await bob.signIn();
    assert.notEqual(aliceUid, bobUid);

    const first = await alice.call("ensurePlayerProfile", {});
    assert.equal(first.created, true);
    assert.equal(first.profile.uid, aliceUid);
    const second = await alice.call("ensurePlayerProfile", {});
    assert.equal(second.created, false);
    assert.equal(second.profile.uid, aliceUid);
  });

  test("a payload uid that is not the caller's is rejected; their own is tolerated", async () => {
    assert.equal(await codeOf(alice.call("getArenaState", { uid: bobUid })), "functions/permission-denied");
    assert.equal((await alice.call("getArenaState", { uid: aliceUid })).success, true);
  });

  test("unknown and forged fields are rejected, not ignored", async () => {
    assert.equal(await codeOf(alice.call("getArenaState", { gold: 999999 })), "functions/invalid-argument");
    const start = await alice.call("startArenaMatch", { requestId: rid(), ...DECK });
    for (const forged of [{ gold: 1500 }, { reward: { gold: 1500 } }, { opponentRating: 100 }, { ratingAfter: 3000 }]) {
      const payload = { matchId: start.matchId, outcome: "VICTORY", turns: 5, damageDealt: 10, damageTaken: 1, ...forged };
      assert.equal(await codeOf(alice.call("submitArenaResult", payload)), "functions/invalid-argument");
    }
  });

  test("a fabricated match id is rejected and another player's match cannot be submitted", async () => {
    assert.equal(
      await codeOf(alice.call("submitArenaResult", { matchId: "arena_invented", outcome: "VICTORY", turns: 5, damageDealt: 10, damageTaken: 1 })),
      "functions/not-found"
    );
    const bobsMatch = await bob.call("startArenaMatch", { requestId: rid(), ...DECK });
    assert.equal(
      await codeOf(alice.call("submitArenaResult", { matchId: bobsMatch.matchId, outcome: "VICTORY", turns: 5, damageDealt: 10, damageTaken: 1 })),
      "functions/permission-denied"
    );
  });

  test("a repeated request id is idempotent", async () => {
    const requestId = rid();
    const a = await bob.call("startArenaMatch", { requestId, ...DECK });
    const b = await bob.call("startArenaMatch", { requestId, ...DECK });
    assert.equal(b.idempotentReplay, true);
    assert.equal(b.matchId, a.matchId);
  });
});

describe("gameplay flows (shared 16s battle wait)", () => {
  test("alliance, arena, boss, raid and event rewards end to end", async (t) => {
    // --- alliance setup (membership flows) ---------------------------------------------------------
    const created = await alice.call("allianceAction", { requestId: rid(), action: "CREATE", name: `E2E Guild ${run}` });
    await bob.call("allianceAction", { requestId: rid(), action: "JOIN", allianceId: created.allianceId });
    let view = await alice.call("syncAlliance", {});
    assert.equal(view.alliance.memberCount, 2);
    assert.equal(view.myRole, "LEADER");

    assert.equal(await codeOf(bob.call("allianceAction", { requestId: rid(), action: "PROMOTE", targetUid: aliceUid })), "functions/permission-denied");
    await alice.call("allianceAction", { requestId: rid(), action: "PROMOTE", targetUid: bobUid });
    assert.equal((await bob.call("syncAlliance", {})).myRole, "OFFICER");
    await alice.call("allianceAction", { requestId: rid(), action: "DEMOTE", targetUid: bobUid });

    // --- start battles that must wait out the minimum battle time ------------------------------------
    const arena = await alice.call("startArenaMatch", { requestId: rid(), ...DECK });
    assert.equal(await codeOf(alice.call("submitArenaResult", { matchId: arena.matchId, outcome: "VICTORY", turns: 6, damageDealt: 800, damageTaken: 100 })), "functions/failed-precondition");

    const endgame = await alice.call("getEndgameState", {});
    const boss = endgame.worldBosses.find((b) => b.active && !b.defeated);
    const eventActive = endgame.activeEvents.length > 0;
    const bossSession = boss ? await alice.call("startEndgameAttempt", { requestId: rid(), kind: "WORLD_BOSS", targetId: boss.bossId }) : null;
    const raidSession = await alice.call("startEndgameAttempt", { requestId: rid(), kind: "RAID", targetId: "raid_olympus", stage: 1, difficulty: "normal" });
    assert.equal(await codeOf(alice.call("startEndgameAttempt", { requestId: rid(), kind: "RAID", targetId: "raid_olympus", stage: 3, difficulty: "normal" })), "functions/failed-precondition");

    await sleep(MIN_BATTLE_WAIT_MS);

    // --- arena result: server rating, server reward, replay ---------------------------------------
    const result = await alice.call("submitArenaResult", { matchId: arena.matchId, outcome: "VICTORY", turns: 6, damageDealt: 800, damageTaken: 100 });
    assert.equal(result.success, true);
    assert.equal(result.ratingBefore, 1000);
    assert.ok(result.ratingAfter > 1000);
    assert.equal(result.rewards.gold, catalog.arena.rewards.firstWin.gold);
    const replay = await alice.call("submitArenaResult", { matchId: arena.matchId, outcome: "DEFEAT", turns: 6, damageDealt: 1, damageTaken: 999 });
    assert.equal(replay.idempotentReplay, true);
    assert.equal(replay.outcome, "VICTORY");
    assert.equal(replay.ratingAfter, result.ratingAfter);

    const board = await alice.call("getLeaderboard", {});
    assert.equal(board.me.uid, aliceUid);
    assert.equal(board.me.rating, result.ratingAfter);

    // --- raid: server-scaled HP, stage unlock, alliance contribution ------------------------------
    assert.equal(await codeOf(alice.call("submitRaidContribution", { sessionId: raidSession.sessionId, damage: 5, cleared: true })), "functions/invalid-argument");
    const raid = await alice.call("submitRaidContribution", { sessionId: raidSession.sessionId, damage: raidSession.enemyHp, cleared: true });
    assert.equal(raid.cleared, true);
    assert.equal(raid.progress, 1);
    assert.equal((await alice.call("submitRaidContribution", { sessionId: raidSession.sessionId, damage: raidSession.enemyHp, cleared: true })).idempotentReplay, true);
    view = await alice.call("syncAlliance", {});
    assert.ok(view.alliance.members.find((m) => m.uid === aliceUid).contribution > 0);

    // --- world boss ---------------------------------------------------------------------------------
    if (boss) {
      assert.equal(await codeOf(alice.call("submitWorldBossContribution", { sessionId: bossSession.sessionId, damage: 500000, victory: false })), "functions/invalid-argument");
      const hit = await alice.call("submitWorldBossContribution", { sessionId: bossSession.sessionId, damage: 20000, victory: false });
      assert.ok(hit.bossHpRemaining < boss.currentHp);
      assert.equal((await alice.call("submitWorldBossContribution", { sessionId: bossSession.sessionId, damage: 20000, victory: false })).idempotentReplay, true);
      assert.equal(await codeOf(alice.call("claimWorldBossReward", { requestId: rid(), bossId: boss.bossId })), "functions/failed-precondition");
    } else {
      t.diagnostic("No active World Boss in the catalog window today; boss assertions skipped.");
    }

    // --- event shop: server wallet, limits, duplicate claim ------------------------------------------
    if (eventActive) {
      const eventId = endgame.activeEvents[0].eventId;
      const claimId = rid();
      const claim = await alice.call("claimEventReward", { requestId: claimId, eventId, itemId: "shop_gold_bounty" });
      assert.equal(claim.success, true);
      assert.equal((await alice.call("claimEventReward", { requestId: claimId, eventId, itemId: "shop_gold_bounty" })).idempotentReplay, true);
      assert.equal(await codeOf(alice.call("claimEventReward", { requestId: rid(), eventId, itemId: "shop_infinite_gold" })), "functions/not-found");
    } else {
      t.diagnostic("No active event in the catalog window today; event assertions skipped.");
    }

    // --- reward outbox: pending once, acknowledged once ----------------------------------------------
    const pending = await alice.call("getPendingRewards", {});
    assert.ok(pending.rewards.some((r) => r.claimId === `arena_${arena.matchId}`));
    const ids = pending.rewards.map((r) => r.claimId);
    assert.equal((await alice.call("ackRewards", { claimIds: ids })).acknowledged, ids.length);
    assert.equal((await alice.call("ackRewards", { claimIds: ids })).acknowledged, 0);
    assert.equal((await alice.call("getPendingRewards", {})).rewards.length, 0);

    // --- alliance exit flows -------------------------------------------------------------------------
    await alice.call("allianceAction", { requestId: rid(), action: "TRANSFER", targetUid: bobUid });
    assert.equal((await bob.call("syncAlliance", {})).myRole, "LEADER");
    await alice.call("allianceAction", { requestId: rid(), action: "LEAVE" });
    assert.equal((await alice.call("syncAlliance", {})).alliance, null);
  });
});
