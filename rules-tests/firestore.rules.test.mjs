import { readFileSync } from "node:fs";
import { after, before, beforeEach, describe, test } from "node:test";
import { assertFails, assertSucceeds, initializeTestEnvironment } from "@firebase/rules-unit-testing";
import { doc, getDoc, setDoc, updateDoc, deleteDoc, collection, getDocs, addDoc } from "firebase/firestore";

/**
 * Runs against the Firestore emulator (npm test in this folder). Seeds data with rules disabled, then
 * asserts what a signed-in or anonymous CLIENT may and may not do.
 */
let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: "demo-mythos",
    firestore: { rules: readFileSync(new URL("../firestore.rules", import.meta.url), "utf8") },
  });
});

after(async () => { await env.cleanup(); });

beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, "players/alice"), { uid: "alice", eventTokens: 5 });
    await setDoc(doc(db, "players/alice/arena/state"), { rating: 1000 });
    await setDoc(doc(db, "players/alice/collection/state"), { counts: { c_x: 1 } });
    await setDoc(doc(db, "players/alice/rewards/r1"), { status: "PENDING" });
    await setDoc(doc(db, "players/bob"), { uid: "bob" });
    await setDoc(doc(db, "leaderboards/season_2/entries/alice"), { rating: 1000 });
    await setDoc(doc(db, "worldBosses/world_boss_kronos"), { currentHp: 1 });
    await setDoc(doc(db, "worldBosses/world_boss_kronos/contributions/alice"), { totalDamage: 9 });
    await setDoc(doc(db, "alliances/al_1"), { name: "Guild", memberCount: 1 });
    await setDoc(doc(db, "alliances/al_1/members/alice"), { role: "LEADER" });
    await setDoc(doc(db, "purchases/hash1"), { uid: "alice", productId: "gem_small" });
    await setDoc(doc(db, "gameSessions/s1"), { uid: "alice" });
    await setDoc(doc(db, "idempotency/k1"), { uid: "alice" });
    await setDoc(doc(db, "rateLimits/alice:read"), { count: 1 });
  });
});

const as = (uid) => env.authenticatedContext(uid).firestore();
const anon = () => env.unauthenticatedContext().firestore();

describe("player-owned data: readable by the owner only", () => {
  test("owner reads profile and every subcollection", async () => {
    const db = as("alice");
    for (const path of ["players/alice", "players/alice/arena/state", "players/alice/collection/state", "players/alice/rewards/r1"]) {
      await assertSucceeds(getDoc(doc(db, path)));
    }
    await assertSucceeds(getDocs(collection(db, "players/alice/rewards")));
  });

  test("another player and anonymous users cannot read it", async () => {
    for (const db of [as("bob"), anon()]) {
      await assertFails(getDoc(doc(db, "players/alice")));
      await assertFails(getDoc(doc(db, "players/alice/arena/state")));
      await assertFails(getDocs(collection(db, "players/alice/rewards")));
    }
  });
});

describe("clients can never write authoritative data", () => {
  test("a player cannot forge their own economy, ownership, arena state or rewards", async () => {
    const db = as("alice");
    await assertFails(setDoc(doc(db, "players/alice"), { uid: "alice", playerLevel: 99, createdAt: 1, gold: 999999999 }));
    await assertFails(updateDoc(doc(db, "players/alice"), { eventTokens: 1000000 }));
    await assertFails(updateDoc(doc(db, "players/alice"), { xp: 1000000 }));
    await assertFails(setDoc(doc(db, "players/alice/collection/state"), { counts: { c_zeus_thunderstone: 99 } }));
    await assertFails(setDoc(doc(db, "players/alice/arena/state"), { rating: 9999 }));
    await assertFails(setDoc(doc(db, "players/alice/arenaMatches/m1"), { outcome: "VICTORY" }));
    await assertFails(updateDoc(doc(db, "players/alice/rewards/r1"), { status: "APPLIED" }));
    await assertFails(setDoc(doc(db, "players/alice/rewards/r2"), { gold: 1000000, status: "PENDING" }));
    await assertFails(setDoc(doc(db, "players/alice/decks/active"), { cardIds: new Array(20).fill("c_x") }));
    await assertFails(setDoc(doc(db, "players/alice/quests/q1"), { progress: 99 }));
    await assertFails(deleteDoc(doc(db, "players/alice")));
    await assertFails(addDoc(collection(db, "players/alice/events"), { tokens: 99 }));
  });

  test("a player cannot write another player's data", async () => {
    await assertFails(setDoc(doc(as("alice"), "players/bob"), { uid: "bob", gold: 1 }));
    await assertFails(setDoc(doc(as("bob"), "players/bob/arena/state"), { rating: 5000 }));
  });

  test("leaderboard, boss, contribution, alliance and member documents are not client-writable", async () => {
    const db = as("alice");
    await assertFails(setDoc(doc(db, "leaderboards/season_2/entries/alice"), { rating: 99999 }));
    await assertFails(setDoc(doc(db, "worldBosses/world_boss_kronos"), { currentHp: 0, status: "DEFEATED" }));
    await assertFails(setDoc(doc(db, "worldBosses/world_boss_kronos/contributions/alice"), { totalDamage: 99999999 }));
    await assertFails(updateDoc(doc(db, "alliances/al_1"), { leaderUid: "alice", memberCount: 1 }));
    await assertFails(updateDoc(doc(db, "alliances/al_1/members/alice"), { role: "LEADER" }));
    await assertFails(setDoc(doc(db, "alliances/al_2"), { name: "Mine" }));
    await assertFails(setDoc(doc(db, "events/event_x"), { reward: 1 }));
    await assertFails(setDoc(doc(db, "raids/raid_olympus"), { stage: 3 }));
  });

  test("unauthenticated users cannot write anything", async () => {
    await assertFails(setDoc(doc(anon(), "players/alice"), { uid: "alice" }));
    await assertFails(setDoc(doc(anon(), "leaderboards/season_2/entries/x"), { rating: 1 }));
  });
});

describe("payment ledger and server-only collections stay closed (deny-all)", () => {
  test("purchases: no read or write, even for the owning uid", async () => {
    for (const db of [as("alice"), as("bob"), anon()]) {
      await assertFails(getDoc(doc(db, "purchases/hash1")));
      await assertFails(getDocs(collection(db, "purchases")));
      await assertFails(setDoc(doc(db, "purchases/hash2"), { uid: "alice", productId: "gem_ultimate" }));
      await assertFails(updateDoc(doc(db, "purchases/hash1"), { uid: "bob" }));
      await assertFails(deleteDoc(doc(db, "purchases/hash1")));
    }
  });

  test("sessions, idempotency keys, rate limits, alliance names and raid aggregates are server-only", async () => {
    const db = as("alice");
    for (const path of ["gameSessions/s1", "idempotency/k1", "rateLimits/alice:read", "allianceNames/guild", "allianceRaids/al_1__raid_olympus"]) {
      await assertFails(getDoc(doc(db, path)));
      await assertFails(setDoc(doc(db, path), { uid: "alice" }));
    }
  });

  test("any unlisted collection is closed", async () => {
    await assertFails(getDoc(doc(as("alice"), "somethingNew/doc1")));
    await assertFails(setDoc(doc(as("alice"), "somethingNew/doc1"), { x: 1 }));
  });
});

describe("public read-only data", () => {
  test("signed-in players can read leaderboards, bosses and alliance headers; anonymous callers cannot", async () => {
    for (const path of ["leaderboards/season_2/entries/alice", "worldBosses/world_boss_kronos", "alliances/al_1"]) {
      await assertSucceeds(getDoc(doc(as("bob"), path)));
      await assertFails(getDoc(doc(anon(), path)));
    }
    await assertSucceeds(getDocs(collection(as("bob"), "leaderboards/season_2/entries")));
  });

  test("boss contributions are readable by their owner only", async () => {
    await assertSucceeds(getDoc(doc(as("alice"), "worldBosses/world_boss_kronos/contributions/alice")));
    await assertFails(getDoc(doc(as("bob"), "worldBosses/world_boss_kronos/contributions/alice")));
  });

  test("alliance member lists are readable by members only", async () => {
    await assertSucceeds(getDoc(doc(as("alice"), "alliances/al_1/members/alice")));
    await assertFails(getDoc(doc(as("bob"), "alliances/al_1/members/alice")));
    await assertFails(getDoc(doc(anon(), "alliances/al_1/members/alice")));
  });
});
