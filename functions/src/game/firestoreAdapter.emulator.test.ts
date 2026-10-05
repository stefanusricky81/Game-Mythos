import * as admin from "firebase-admin";
import { CATALOG } from "./catalog";
import { FirestoreGameDb } from "./db";
import { Env, handleCall } from "./handlers";

/**
 * Runs the real FirestoreGameDb (production adapter) against the Firestore emulator. Skipped unless the
 * emulator is running, so plain `npm test` stays hermetic:
 *
 *   firebase emulators:exec --only firestore --project demo-mythos "npx jest src/game/firestoreAdapter"
 *
 * It exists because the in-memory double used by game.test.ts cannot prove the adapter itself: real
 * transaction retries, query/orderBy/count semantics, merge behaviour and Firestore's rejection of
 * `undefined` values only show up against real Firestore code.
 */
const emulator = !!process.env.FIRESTORE_EMULATOR_HOST;
const suite = emulator ? describe : describe.skip;
jest.setTimeout(90_000); // first call pays emulator/gRPC warm-up

const T0 = Date.UTC(2026, 9, 2, 5, 0, 0);
const SEC = 1000;
const DECK = { heroId: CATALOG.starter.heroId, cardIds: CATALOG.starter.deck };

suite("FirestoreGameDb against the Firestore emulator", () => {
  let env: Env;
  const run = Math.random().toString(36).slice(2, 8);
  const uid = (name: string) => `${name}${run}`;
  let n = 0;
  const rid = () => `emu-${run}-${String(++n).padStart(6, "0")}`;
  const call = (name: string, who: string, data: unknown = {}, now = T0) =>
    handleCall(env, name, { uid: who }, data, now) as Promise<any>;

  beforeAll(() => {
    if (admin.apps.length === 0) admin.initializeApp({ projectId: "demo-mythos" });
    env = { db: new FirestoreGameDb(), collectionAttestationAllowed: true };
  });

  test("arena match lifecycle with real transactions", async () => {
    const alice = uid("alice");
    const m = await call("startArenaMatch", alice, { requestId: rid(), ...DECK });
    expect(m.remainingAttempts).toBe(4);
    const r = await call("submitArenaResult", alice, { matchId: m.matchId, outcome: "VICTORY", turns: 7, damageDealt: 900, damageTaken: 100 }, T0 + 20 * SEC);
    expect(r.rewards.gold).toBe(CATALOG.arena.rewards.firstWin.gold);
    const state = await call("getArenaState", alice);
    expect(state.state).toMatchObject({ wins: 1, dailyAttempts: 4 });
    expect(state.history).toHaveLength(1);
  });

  test("concurrent duplicate submissions grant exactly one reward", async () => {
    const bob = uid("bob");
    const m = await call("startArenaMatch", bob, { requestId: rid(), ...DECK });
    const submit = () => call("submitArenaResult", bob, { matchId: m.matchId, outcome: "VICTORY", turns: 5, damageDealt: 10, damageTaken: 1 }, T0 + 20 * SEC);
    const results = await Promise.all([submit(), submit(), submit(), submit(), submit()]);
    expect(new Set(results.map((x) => x.ratingAfter)).size).toBe(1);
    const profile = await env.db.get(`players/${bob}`);
    expect(profile!.xp).toBe(CATALOG.arena.rewards.firstWin.xp);
    expect(await env.db.count(`players/${bob}/rewards`)).toBe(1);
  });

  test("leaderboard ordering and the rank count query work on real Firestore", async () => {
    const [a, b, c] = [uid("lbA"), uid("lbB"), uid("lbC")];
    for (const [who, outcome] of [[a, "VICTORY"], [b, "DEFEAT"], [c, "VICTORY"]] as const) {
      const m = await call("startArenaMatch", who, { requestId: rid(), ...DECK }, T0 + 100 * SEC);
      await call("submitArenaResult", who, { matchId: m.matchId, outcome, turns: 5, damageDealt: 10, damageTaken: 1 }, T0 + 130 * SEC);
    }
    const board = await call("getLeaderboard", b, { limit: 100 }, T0 + 140 * SEC);
    const mine = board.entries.find((e: any) => e.uid === b);
    expect(mine.rank).toBe(board.me.rank);
    expect(board.entries.map((e: any) => e.rating)).toEqual([...board.entries.map((e: any) => e.rating)].sort((x: number, y: number) => y - x));
  });

  test("alliance create/join/cap and the open-alliance range query", async () => {
    const leader = uid("leader");
    const { allianceId } = await call("allianceAction", leader, { requestId: rid(), action: "CREATE", name: `Emu ${run}` });
    for (let i = 1; i < 20; i++) await call("allianceAction", uid(`m${i}`), { requestId: rid(), action: "JOIN", allianceId });
    await expect(call("allianceAction", uid("late"), { requestId: rid(), action: "JOIN", allianceId })).rejects.toMatchObject({ code: "failed-precondition" });
    const view = await call("syncAlliance", leader);
    expect(view.alliance.memberCount).toBe(20);
    expect(view.openAlliances.every((a: any) => a.memberCount < 20)).toBe(true);
  });

  test("raid, world boss and event shop flows run on real Firestore", async () => {
    const carol = uid("carol");
    await call("allianceAction", carol, { requestId: rid(), action: "CREATE", name: `Raiders ${run}` });
    const raid = await call("startEndgameAttempt", carol, { requestId: rid(), kind: "RAID", targetId: "raid_olympus", stage: 1, difficulty: "normal" });
    const cleared = await call("submitRaidContribution", carol, { sessionId: raid.sessionId, damage: raid.enemyHp, cleared: true }, T0 + 20 * SEC);
    expect(cleared.progress).toBe(1);

    const boss = await call("startEndgameAttempt", carol, { requestId: rid(), kind: "WORLD_BOSS", targetId: "world_boss_kronos" });
    const hit = await call("submitWorldBossContribution", carol, { sessionId: boss.sessionId, damage: 30_000, victory: false }, T0 + 20 * SEC);
    expect(hit.bossHpRemaining).toBeLessThan(1_000_000);

    // Raid + boss participation gave the player event tokens; spend some through the server wallet.
    const state = await call("getEndgameState", carol);
    expect(state.wallet.eventTokens).toBeGreaterThanOrEqual(30);
    const claim = await call("claimEventReward", carol, { requestId: rid(), eventId: "event_wrath_of_olympus", itemId: "shop_gold_bounty" });
    expect(claim.success).toBe(true);

    const pending = await call("getPendingRewards", carol);
    // Raid clear (gold/xp) and the shop purchase (gold) are client-applicable; the zero-gold boss participation
    // reward (event tokens only) is held on the server and is born APPLIED, so it is not in the outbox.
    expect(pending.rewards.map((r: any) => r.source).sort()).toEqual(["EVENT_SHOP", "RAID_STAGE"]);
    const ack = await call("ackRewards", carol, { claimIds: pending.rewards.map((r: any) => r.claimId) });
    expect(ack.acknowledged).toBe(pending.rewards.length);
  });

  test("the server-side rate limit works against real Firestore", async () => {
    const dave = uid("dave");
    for (let i = 0; i < 30; i++) await call("getArenaState", dave);
    await expect(call("getArenaState", dave)).rejects.toMatchObject({ code: "resource-exhausted" });
  });
});
