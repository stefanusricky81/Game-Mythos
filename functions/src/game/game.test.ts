import { CATALOG, eloDelta, findOpponent } from "./catalog";
import { Env, handleCall } from "./handlers";
import { MemoryGameDb } from "./memoryDb";
import { P } from "./paths";
import { seasonOf, startOfDateKey } from "./time";

const T0 = Date.UTC(2026, 9, 2, 5, 0, 0); // 2026-10-02 12:00 WIB: season_2, World Boss Kronos active
const SEC = 1000;
const DAY = 24 * 60 * 60 * 1000;

let counter = 0;
const rid = () => `req-${String(++counter).padStart(10, "0")}`;

const makeEnv = (attest = true): Env & { db: MemoryGameDb } => ({ db: new MemoryGameDb(), collectionAttestationAllowed: attest });
const call = (env: Env, name: string, uid: string | null, data: unknown = {}, now = T0) =>
  handleCall(env, name, uid === null ? null : { uid }, data, now) as Promise<any>;
const rejects = async (p: Promise<unknown>, code: string) => { await expect(p).rejects.toMatchObject({ code }); };

const DECK = { heroId: CATALOG.starter.heroId, cardIds: CATALOG.starter.deck };
const startArena = (env: Env, uid: string, now = T0, deck = DECK) => call(env, "startArenaMatch", uid, { requestId: rid(), ...deck }, now);
const finishArena = (env: Env, uid: string, matchId: string, outcome: string, now = T0 + 20 * SEC) =>
  call(env, "submitArenaResult", uid, { matchId, outcome, turns: 8, damageDealt: 1200, damageTaken: 300 }, now);
const playArena = async (env: Env, uid: string, outcome: string, now = T0) => {
  const m = await startArena(env, uid, now);
  return finishArena(env, uid, m.matchId, outcome, now + 20 * SEC);
};

describe("authentication, identity and request hygiene", () => {
  test("unauthenticated request is rejected", async () => {
    await rejects(call(makeEnv(), "getArenaState", null), "unauthenticated");
  });

  test("malformed uid is rejected", async () => {
    await rejects(call(makeEnv(), "getArenaState", "bad/uid"), "permission-denied");
  });

  test("payload uid that is not the authenticated uid is rejected (impersonation)", async () => {
    await rejects(call(makeEnv(), "startArenaMatch", "alice", { requestId: rid(), ...DECK, uid: "bob" }), "permission-denied");
    await expect(call(makeEnv(), "getArenaState", "alice", { uid: "alice" })).resolves.toMatchObject({ success: true });
  });

  test("unknown function and unknown fields are rejected", async () => {
    await rejects(call(makeEnv(), "grantMeGold", "alice"), "not-found");
    await rejects(call(makeEnv(), "getArenaState", "alice", { gold: 999999 }), "invalid-argument");
  });

  test("server-side rate limit blocks bursts and recovers after the window", async () => {
    const env = makeEnv();
    for (let i = 0; i < 30; i++) await call(env, "getArenaState", "alice");
    await rejects(call(env, "getArenaState", "alice"), "resource-exhausted");
    await expect(call(env, "getArenaState", "bob")).resolves.toMatchObject({ success: true });
    await expect(call(env, "getArenaState", "alice", {}, T0 + 61 * SEC)).resolves.toMatchObject({ success: true });
  });

  test("the in-memory database enforces Firestore's reads-before-writes rule", async () => {
    const db = new MemoryGameDb();
    await expect(db.runTransaction(async (tx) => { tx.set("a/b", { x: 1 }); await tx.get("a/b"); })).rejects.toThrow(/reads/);
  });
});

describe("deck validation", () => {
  test("starter deck is valid", async () => {
    await expect(call(makeEnv(), "validateDeck", "alice", DECK)).resolves.toMatchObject({ valid: true, size: 20 });
  });

  test("a deck that is not exactly 20 cards is rejected", async () => {
    const env = makeEnv();
    const short = await call(env, "validateDeck", "alice", { ...DECK, cardIds: DECK.cardIds.slice(0, 19) });
    const long = await call(env, "validateDeck", "alice", { ...DECK, cardIds: [...DECK.cardIds, DECK.cardIds[0]] });
    expect(short.valid).toBe(false);
    expect(short.issues.map((i: any) => i.type)).toContain("INVALID_DECK_SIZE");
    expect(long.valid).toBe(false);
  });

  test("more than 2 copies of a card is rejected", async () => {
    const card = DECK.cardIds[0];
    const cheating = [card, card, card, ...DECK.cardIds.filter((c) => c !== card).slice(0, 17)];
    const result = await call(makeEnv(), "validateDeck", "alice", { ...DECK, cardIds: cheating });
    expect(result.issues.map((i: any) => i.type)).toContain("EXCEEDED_DUPLICATE_LIMIT");
  });

  test("a card the player does not own is rejected", async () => {
    const unowned = CATALOG.cards.find((c) => !(c in CATALOG.starter.ownedCardCounts))!;
    const result = await call(makeEnv(), "validateDeck", "alice", { ...DECK, cardIds: [...DECK.cardIds.slice(0, 19), unowned] });
    expect(result.valid).toBe(false);
    expect(result.issues.map((i: any) => i.type)).toContain("UNOWNED_CARD");
  });

  test("forged card IDs and unknown heroes are rejected", async () => {
    const result = await call(makeEnv(), "validateDeck", "alice", { heroId: "hero_forged", cardIds: [...DECK.cardIds.slice(0, 19), "c_infinite_gold"] });
    const types = result.issues.map((i: any) => i.type);
    expect(types).toEqual(expect.arrayContaining(["UNKNOWN_CARD_ID", "UNKNOWN_HERO_ID"]));
  });

  test("oversized or malformed deck payloads are rejected before any work is done", async () => {
    await rejects(call(makeEnv(), "validateDeck", "alice", { heroId: DECK.heroId, cardIds: new Array(500).fill("c_x") }), "invalid-argument");
    await rejects(call(makeEnv(), "validateDeck", "alice", { heroId: DECK.heroId, cardIds: "nope" }), "invalid-argument");
  });

  test("collection attestation only adds catalog cards, never lowers counts, and can be disabled", async () => {
    const env = makeEnv();
    const unowned = CATALOG.cards.find((c) => !(c in CATALOG.starter.ownedCardCounts))!;
    const deck = { ...DECK, cardIds: [...DECK.cardIds.slice(0, 19), unowned] };
    expect((await call(env, "validateDeck", "alice", deck)).valid).toBe(false);

    await call(env, "syncCollection", "alice", { requestId: rid(), ownedCardCounts: { [unowned]: 1, [DECK.cardIds[0]]: 0 } });
    expect((await call(env, "validateDeck", "alice", deck)).valid).toBe(true);
    expect(env.db.peek(P.collection("alice"))!.counts[DECK.cardIds[0]]).toBeGreaterThan(0);

    await rejects(call(env, "syncCollection", "alice", { requestId: rid(), ownedCardCounts: { c_forged: 5 } }), "invalid-argument");
    await rejects(call(makeEnv(false), "syncCollection", "alice", { requestId: rid(), ownedCardCounts: {} }), "failed-precondition");
  });
});

describe("arena: server-authoritative matches", () => {
  test("starting a match consumes exactly one attempt and the server picks the opponent", async () => {
    const env = makeEnv();
    const m = await startArena(env, "alice");
    expect(m).toMatchObject({ success: true, resumed: false, remainingAttempts: 4, ratingBefore: 1000 });
    expect(m.opponent.rating).toBe(findOpponent(1000).rating);
    expect(m.opponent.deck).toHaveLength(20);
    expect(m.matchId).toMatch(/^arena_/);
  });

  test("duplicate start requests never charge a second attempt", async () => {
    const env = makeEnv();
    const requestId = rid();
    const a = await call(env, "startArenaMatch", "alice", { requestId, ...DECK });
    const b = await call(env, "startArenaMatch", "alice", { requestId, ...DECK });
    expect(b.idempotentReplay).toBe(true);
    expect(b.matchId).toBe(a.matchId);
    const c = await startArena(env, "alice"); // a different request while a match is active resumes it
    expect(c).toMatchObject({ resumed: true, matchId: a.matchId, remainingAttempts: 4 });
  });

  test("an invalid deck cannot start a match and costs no attempt", async () => {
    const env = makeEnv();
    await rejects(startArena(env, "alice", T0, { ...DECK, cardIds: DECK.cardIds.slice(1) }), "invalid-argument");
    expect((await call(env, "getArenaState", "alice")).state.dailyAttempts).toBe(5);
  });

  test("a fabricated match ID is rejected", async () => {
    await rejects(finishArena(makeEnv(), "alice", "arena_made_up_by_client", "VICTORY"), "not-found");
  });

  test("another player's match cannot be submitted", async () => {
    const env = makeEnv();
    const m = await startArena(env, "alice");
    await rejects(finishArena(env, "mallory", m.matchId, "VICTORY"), "permission-denied");
  });

  test("a result submitted faster than a real battle is rejected", async () => {
    const env = makeEnv();
    const m = await startArena(env, "alice");
    await rejects(finishArena(env, "alice", m.matchId, "VICTORY", T0 + 2 * SEC), "failed-precondition");
  });

  test("rating, rewards and history are computed by the server", async () => {
    const env = makeEnv();
    const m = await startArena(env, "alice");
    const r = await finishArena(env, "alice", m.matchId, "VICTORY");

    const expectedDelta = eloDelta(1000, m.opponent.rating, "VICTORY");
    expect(r).toMatchObject({ success: true, ratingBefore: 1000, ratingAfter: 1000 + expectedDelta, ratingDelta: expectedDelta });
    expect(r.rewards).toMatchObject({ gold: CATALOG.arena.rewards.firstWin.gold, xp: CATALOG.arena.rewards.firstWin.xp, isFirstWin: true });

    const state = await call(env, "getArenaState", "alice");
    expect(state.state).toMatchObject({ rating: 1000 + expectedDelta, wins: 1, losses: 0, dailyAttempts: 4 });
    expect(state.history).toHaveLength(1);

    const board = await call(env, "getLeaderboard", "alice");
    expect(board.me).toMatchObject({ uid: "alice", rank: 1, rating: 1000 + expectedDelta });

    const profile = env.db.peek(P.profile("alice"))!;
    expect(profile.arenaPoints).toBe(CATALOG.arena.rewards.firstWin.arenaPoints);
    expect(profile.lifetime).toMatchObject({ battles: 1, wins: 1 });
  });

  test("a duplicate result submission returns the original result and never double-rewards", async () => {
    const env = makeEnv();
    const m = await startArena(env, "alice");
    const first = await finishArena(env, "alice", m.matchId, "VICTORY");
    const second = await finishArena(env, "alice", m.matchId, "VICTORY", T0 + 40 * SEC);

    expect(second.idempotentReplay).toBe(true);
    expect(second.ratingAfter).toBe(first.ratingAfter);
    expect(env.db.peek(P.profile("alice"))!.xp).toBe(CATALOG.arena.rewards.firstWin.xp);
    expect(env.db.paths(`players/alice/rewards/`)).toHaveLength(1);
    expect((await call(env, "getArenaState", "alice")).state.wins).toBe(1);
  });

  test("a replay cannot flip a defeat into a victory", async () => {
    const env = makeEnv();
    const m = await startArena(env, "alice");
    const loss = await finishArena(env, "alice", m.matchId, "DEFEAT");
    const flipped = await finishArena(env, "alice", m.matchId, "VICTORY", T0 + 40 * SEC);
    expect(flipped.outcome).toBe("DEFEAT");
    expect(flipped.ratingAfter).toBe(loss.ratingAfter);
  });

  test("concurrent duplicate submissions grant exactly one reward", async () => {
    const env = makeEnv();
    const m = await startArena(env, "alice");
    await Promise.all([finishArena(env, "alice", m.matchId, "VICTORY"), finishArena(env, "alice", m.matchId, "VICTORY"), finishArena(env, "alice", m.matchId, "VICTORY")]);
    expect(env.db.peek(P.profile("alice"))!.xp).toBe(CATALOG.arena.rewards.firstWin.xp);
    expect(env.db.paths(`players/alice/arenaMatches/`)).toHaveLength(1);
  });

  test("client-supplied rewards, ratings, opponents or ids are rejected, not ignored", async () => {
    const env = makeEnv();
    const m = await startArena(env, "alice");
    const base = { matchId: m.matchId, outcome: "VICTORY", turns: 8, damageDealt: 100, damageTaken: 1 };
    for (const forged of [{ gold: 1500 }, { reward: { gold: 1500 } }, { opponentRating: 100 }, { ratingAfter: 3000 }, { opponent: { rating: 1 } }]) {
      await rejects(call(env, "submitArenaResult", "alice", { ...base, ...forged }, T0 + 20 * SEC), "invalid-argument");
    }
    await rejects(call(env, "submitArenaResult", "alice", { ...base, outcome: "FLAWLESS" }, T0 + 20 * SEC), "invalid-argument");
    await rejects(call(env, "submitArenaResult", "alice", { ...base, damageDealt: -5 }, T0 + 20 * SEC), "invalid-argument");
    await rejects(call(env, "submitArenaResult", "alice", { ...base, turns: 0 }, T0 + 20 * SEC), "invalid-argument");
    expect(env.db.peek(P.profile("alice"))!.xp).toBe(0);
    expect(env.db.paths("players/alice/rewards/")).toHaveLength(0);
    expect(env.db.paths("players/alice/arenaMatches/")).toHaveLength(0);
  });

  test("daily attempts are capped at 5 and reset on the next server day", async () => {
    const env = makeEnv();
    for (let i = 0; i < 5; i++) await playArena(env, "alice", "DEFEAT", T0 + i * 100 * SEC);
    await rejects(startArena(env, "alice", T0 + 600 * SEC), "failed-precondition");
    const tomorrow = startOfDateKey("2026-10-03") + 10 * SEC;
    await expect(startArena(env, "alice", tomorrow)).resolves.toMatchObject({ success: true, remainingAttempts: 4 });
  });

  test("first-win bonus applies once per day and win streaks add the streak bonus", async () => {
    const env = makeEnv();
    const w1 = await playArena(env, "alice", "VICTORY", T0);
    const w2 = await playArena(env, "alice", "VICTORY", T0 + 100 * SEC);
    const w3 = await playArena(env, "alice", "VICTORY", T0 + 200 * SEC);
    const { firstWin, normalWin, streak } = CATALOG.arena.rewards;
    expect(w1.rewards.gold).toBe(firstWin.gold);
    expect(w2.rewards.gold).toBe(normalWin.gold);
    expect(w3.rewards).toMatchObject({ gold: normalWin.gold + streak["3"].gold, streak: 3, streakBonusPoints: streak["3"].arenaPoints });
  });

  test("an abandoned (expired) match is closed on the next start and its attempt is not refunded", async () => {
    const env = makeEnv();
    const first = await startArena(env, "alice");
    const later = T0 + 3 * 60 * 60 * 1000;
    const second = await startArena(env, "alice", later);
    expect(second.resumed).toBe(false);
    expect(second.matchId).not.toBe(first.matchId);
    expect(second.remainingAttempts).toBe(3);
    await rejects(finishArena(env, "alice", first.matchId, "VICTORY", later + 20 * SEC), "failed-precondition");
  });
});

describe("arena: seasons, rewards and leaderboard", () => {
  const season2 = seasonOf(T0, CATALOG.arena.seasonDays);
  const inSeason3 = season2.endMs + 5 * SEC;

  test("a finished season can be claimed exactly once", async () => {
    const env = makeEnv();
    await playArena(env, "alice", "VICTORY");
    const requestId = rid();
    const claim = await call(env, "claimArenaReward", "alice", { requestId, seasonId: "season_2" }, inSeason3);
    expect(claim).toMatchObject({ success: true, alreadyClaimed: false, seasonId: "season_2" });
    expect(claim.reward.gold).toBe(CATALOG.arena.seasonRewards[claim.tier].gold);

    const replay = await call(env, "claimArenaReward", "alice", { requestId, seasonId: "season_2" }, inSeason3);
    expect(replay).toMatchObject({ idempotentReplay: true, claimId: claim.claimId });
    await rejects(call(env, "claimArenaReward", "alice", { requestId: rid(), seasonId: "season_2" }, inSeason3), "already-exists");
    expect(env.db.paths("players/alice/rewards/arena_season")).toHaveLength(1);
  });

  test("the running season, unknown seasons and never-played seasons cannot be claimed", async () => {
    const env = makeEnv();
    await playArena(env, "alice", "VICTORY");
    await rejects(call(env, "claimArenaReward", "alice", { requestId: rid(), seasonId: "season_2" }, T0 + 1000 * SEC), "failed-precondition");
    await rejects(call(env, "claimArenaReward", "alice", { requestId: rid(), seasonId: "season_1" }, inSeason3), "not-found");
    await rejects(call(env, "claimArenaReward", "alice", { requestId: rid(), seasonId: "season_999" }, inSeason3), "failed-precondition");
    await rejects(call(env, "claimArenaReward", "alice", { requestId: rid(), seasonId: "../season_2" }, inSeason3), "invalid-argument");
  });

  test("a new season soft-resets the rating", async () => {
    const env = makeEnv();
    const win = await playArena(env, "alice", "VICTORY");
    const state = await call(env, "getArenaState", "alice", {}, inSeason3);
    expect(state.state.seasonId).toBe("season_3");
    expect(state.state.rating).toBeGreaterThanOrEqual(1000);
    expect(state.state.rating).toBeLessThan(win.ratingAfter + 1);
    expect(state.claimableSeasons.map((s: any) => s.seasonId)).toContain("season_2");
  });

  test("the leaderboard is ordered by rating with server-computed ranks and a bounded limit", async () => {
    const env = makeEnv();
    await playArena(env, "alice", "VICTORY");
    await playArena(env, "bob", "DEFEAT");
    await playArena(env, "carol", "VICTORY", T0 + 100 * SEC);
    const board = await call(env, "getLeaderboard", "bob", { limit: 10 });
    expect(board.entries.map((e: any) => e.rank)).toEqual([1, 2, 3]);
    expect(board.entries[2].uid).toBe("bob");
    expect(board.me.rank).toBe(3);
    await rejects(call(env, "getLeaderboard", "bob", { limit: 5000 }), "invalid-argument");
    await rejects(call(env, "getLeaderboard", "bob", { seasonId: "x/y" }), "invalid-argument");
  });
});

describe("alliance authority", () => {
  const create = (env: Env, uid: string, name = "Titans Bane") => call(env, "allianceAction", uid, { requestId: rid(), action: "CREATE", name });
  const act = (env: Env, uid: string, body: Record<string, unknown>) => call(env, "allianceAction", uid, { requestId: rid(), ...body });

  test("create, join and sync membership from the server", async () => {
    const env = makeEnv();
    const { allianceId } = await create(env, "leader");
    await act(env, "bob", { action: "JOIN", allianceId });
    const view = await call(env, "syncAlliance", "bob");
    expect(view.myRole).toBe("MEMBER");
    expect(view.alliance.memberCount).toBe(2);
    expect(view.alliance.members.map((m: any) => m.uid).sort()).toEqual(["bob", "leader"]);
  });

  test("names must be unique and valid; one alliance per player", async () => {
    const env = makeEnv();
    await create(env, "a");
    await rejects(create(env, "b"), "already-exists");
    await rejects(create(env, "c", "x"), "invalid-argument");
    await rejects(create(env, "c", "<script>"), "invalid-argument");
    await rejects(create(env, "a", "Another One"), "failed-precondition");
  });

  test("the 20-member cap is enforced atomically", async () => {
    const env = makeEnv();
    const { allianceId } = await create(env, "leader");
    for (let i = 1; i < 20; i++) await act(env, `member${i}`, { action: "JOIN", allianceId });
    await rejects(act(env, "latecomer", { action: "JOIN", allianceId }), "failed-precondition");
    expect((await call(env, "syncAlliance", "leader")).alliance.memberCount).toBe(20);
  });

  test("role permissions: only the leader promotes/demotes/transfers; officers kick members but not officers", async () => {
    const env = makeEnv();
    const { allianceId } = await create(env, "leader");
    for (const u of ["off1", "off2", "mem1", "mem2"]) await act(env, u, { action: "JOIN", allianceId });

    await rejects(act(env, "mem1", { action: "PROMOTE", targetUid: "mem2" }), "permission-denied");
    await rejects(act(env, "off1", { action: "PROMOTE", targetUid: "mem2" }), "permission-denied");
    await act(env, "leader", { action: "PROMOTE", targetUid: "off1" });
    await act(env, "leader", { action: "PROMOTE", targetUid: "off2" });

    await rejects(act(env, "mem1", { action: "KICK", targetUid: "mem2" }), "permission-denied");
    await rejects(act(env, "off1", { action: "KICK", targetUid: "off2" }), "permission-denied");
    await rejects(act(env, "off1", { action: "KICK", targetUid: "leader" }), "failed-precondition");
    await rejects(act(env, "off1", { action: "TRANSFER", targetUid: "mem1" }), "permission-denied");
    await act(env, "off1", { action: "KICK", targetUid: "mem2" });
    expect((await call(env, "syncAlliance", "leader")).alliance.memberCount).toBe(4);
    expect((await call(env, "syncAlliance", "mem2")).alliance).toBeNull();

    await act(env, "leader", { action: "DEMOTE", targetUid: "off2" });
    await rejects(act(env, "leader", { action: "DEMOTE", targetUid: "mem1" }), "failed-precondition");
  });

  test("operations by non-members and on foreign targets are rejected", async () => {
    const env = makeEnv();
    const { allianceId } = await create(env, "leader");
    await act(env, "mem", { action: "JOIN", allianceId });
    await create(env, "other", "Other Guild");
    await rejects(act(env, "outsider", { action: "KICK", targetUid: "mem" }), "failed-precondition");
    await rejects(act(env, "other", { action: "KICK", targetUid: "mem" }), "not-found");
    await rejects(act(env, "leader", { action: "KICK", targetUid: "leader" }), "invalid-argument");
    await rejects(act(env, "leader", { action: "JOIN", allianceId: "nope" }), "failed-precondition");
    await rejects(act(env, "mem", { action: "LEAVE", targetUid: "leader" }), "invalid-argument");
  });

  test("leadership transfer and leader leaving keep the alliance consistent", async () => {
    const env = makeEnv();
    const { allianceId } = await create(env, "leader");
    await act(env, "mem", { action: "JOIN", allianceId });
    await act(env, "leader", { action: "TRANSFER", targetUid: "mem" });
    let view = await call(env, "syncAlliance", "mem");
    expect(view.myRole).toBe("LEADER");
    expect((await call(env, "syncAlliance", "leader")).myRole).toBe("OFFICER");

    await act(env, "mem", { action: "LEAVE" });
    view = await call(env, "syncAlliance", "leader");
    expect(view.myRole).toBe("LEADER");
    expect(view.alliance.leaderUid).toBe("leader");

    const last = await act(env, "leader", { action: "LEAVE" });
    expect(last.disbanded).toBe(true);
    expect(env.db.peek(P.alliance(allianceId))).toBeUndefined();
    await create(env, "newbie"); // name is free again
  });

  test("a repeated request is a no-op replay, not a second membership change", async () => {
    const env = makeEnv();
    const requestId = rid();
    const body = { requestId, action: "CREATE", name: "Replay Guild" };
    const a = await call(env, "allianceAction", "leader", body);
    const b = await call(env, "allianceAction", "leader", body);
    expect(b).toMatchObject({ idempotentReplay: true, allianceId: a.allianceId });
  });
});

describe("raids", () => {
  const raidId = "raid_olympus";
  const setupAlliance = async (env: Env, uid = "alice") =>
    (await call(env, "allianceAction", uid, { requestId: rid(), action: "CREATE", name: `Guild ${uid}` })).allianceId;
  const startRaid = (env: Env, uid: string, stage: number, difficulty = "normal", now = T0) =>
    call(env, "startEndgameAttempt", uid, { requestId: rid(), kind: "RAID", targetId: raidId, stage, difficulty }, now);
  const submit = (env: Env, uid: string, body: Record<string, unknown>, now = T0 + 20 * SEC) => call(env, "submitRaidContribution", uid, body, now);

  test("raids require alliance membership", async () => {
    await rejects(startRaid(makeEnv(), "alice", 1), "failed-precondition");
  });

  test("stages unlock in order, rewards come from the catalog and duplicates do not double-pay", async () => {
    const env = makeEnv();
    const allianceId = await setupAlliance(env);
    await rejects(startRaid(env, "alice", 3), "failed-precondition");

    const s1 = await startRaid(env, "alice", 1);
    expect(s1.enemyHp).toBe(CATALOG.raids[0].stages[0].hp);
    const done = await submit(env, "alice", { sessionId: s1.sessionId, damage: s1.enemyHp, cleared: true });
    expect(done.rewards).toMatchObject({ gold: CATALOG.raids[0].stages[0].gold, eventTokens: CATALOG.raids[0].stages[0].eventTokens });
    expect(done.progress).toBe(1);

    const replay = await submit(env, "alice", { sessionId: s1.sessionId, damage: s1.enemyHp, cleared: true }, T0 + 60 * SEC);
    expect(replay.idempotentReplay).toBe(true);
    expect(env.db.peek(P.profile("alice"))!.eventTokens).toBe(CATALOG.raids[0].stages[0].eventTokens);

    await expect(startRaid(env, "alice", 2)).resolves.toMatchObject({ stage: 2 });
    const member = env.db.peek(P.allianceMember(allianceId, "alice"))!;
    expect(member.contribution).toBeGreaterThan(0);
  });

  test("difficulty scales enemy HP and rewards on the server", async () => {
    const env = makeEnv();
    await setupAlliance(env);
    const hard = CATALOG.difficulties.find((d) => d.id === "hard")!;
    const s = await startRaid(env, "alice", 1, "hard");
    expect(s.enemyHp).toBe(Math.floor(CATALOG.raids[0].stages[0].hp * hard.hpMultiplier));
    const done = await submit(env, "alice", { sessionId: s.sessionId, damage: s.enemyHp, cleared: true });
    expect(done.rewards.gold).toBe(Math.floor(CATALOG.raids[0].stages[0].gold * hard.rewardMultiplier));
  });

  test("invalid raid contributions are rejected", async () => {
    const env = makeEnv();
    await setupAlliance(env);
    const s = await startRaid(env, "alice", 1);
    await rejects(submit(env, "alice", { sessionId: "raid_forged", damage: 100, cleared: false }), "not-found");
    await rejects(submit(env, "alice", { sessionId: s.sessionId, damage: 100, cleared: true }), "invalid-argument");
    await rejects(submit(env, "alice", { sessionId: s.sessionId, damage: s.enemyHp * 10, cleared: true }), "invalid-argument");
    await rejects(submit(env, "alice", { sessionId: s.sessionId, damage: -1, cleared: false }), "invalid-argument");
    await rejects(submit(env, "alice", { sessionId: s.sessionId, damage: 5, cleared: false, gold: 99999 }), "invalid-argument");
    await rejects(submit(env, "alice", { sessionId: s.sessionId, damage: 5, cleared: false }, T0 + 3 * SEC), "failed-precondition");
    await rejects(submit(env, "bob", { sessionId: s.sessionId, damage: 5, cleared: false }), "permission-denied");
    await rejects(call(env, "startEndgameAttempt", "alice", { requestId: rid(), kind: "RAID", targetId: raidId, stage: 1, difficulty: "godmode" }), "invalid-argument");
    await rejects(call(env, "startEndgameAttempt", "alice", { requestId: rid(), kind: "RAID", targetId: "raid_nope", stage: 1, difficulty: "normal" }), "not-found");
  });

  test("daily raid attempts are capped at 3", async () => {
    const env = makeEnv();
    await setupAlliance(env);
    for (let i = 0; i < 3; i++) await startRaid(env, "alice", 1);
    await rejects(startRaid(env, "alice", 1), "failed-precondition");
    await expect(startRaid(env, "alice", 1, "normal", startOfDateKey("2026-10-03") + SEC)).resolves.toMatchObject({ success: true });
  });

  test("a player who left the alliance cannot submit for the old alliance", async () => {
    const env = makeEnv();
    await setupAlliance(env);
    const s = await startRaid(env, "alice", 1);
    await call(env, "allianceAction", "alice", { requestId: rid(), action: "LEAVE" });
    await rejects(submit(env, "alice", { sessionId: s.sessionId, damage: s.enemyHp, cleared: true }), "failed-precondition");
  });
});

describe("world boss", () => {
  const bossId = "world_boss_kronos";
  const start = (env: Env, uid: string, now = T0) => call(env, "startEndgameAttempt", uid, { requestId: rid(), kind: "WORLD_BOSS", targetId: bossId }, now);
  const submit = (env: Env, uid: string, body: Record<string, unknown>, now = T0 + 20 * SEC) => call(env, "submitWorldBossContribution", uid, body, now);

  test("damage is applied to the shared boss and rewards are server-defined", async () => {
    const env = makeEnv();
    const s = await start(env, "alice");
    expect(s.enemyHp).toBe(35_000);
    const r = await submit(env, "alice", { sessionId: s.sessionId, damage: 30_000, victory: false });
    expect(r).toMatchObject({ bossHpRemaining: 970_000, bossDefeated: false });
    expect(r.rewards).toMatchObject({ gold: 0, eventTokens: 25 });

    const s2 = await start(env, "bob");
    await submit(env, "bob", { sessionId: s2.sessionId, damage: 35_000, victory: true });
    const state = await call(env, "getEndgameState", "bob");
    expect(state.worldBosses[0]).toMatchObject({ currentHp: 935_000, mine: { totalDamage: 35_000, victories: 1 } });
    expect(env.db.peek(P.profile("bob"))).toMatchObject({ eventTokens: 25 });
  });

  test("impossible damage, unearned victories and forged sessions are rejected", async () => {
    const env = makeEnv();
    const s = await start(env, "alice");
    await rejects(submit(env, "alice", { sessionId: s.sessionId, damage: 500_000, victory: false }), "invalid-argument");
    await rejects(submit(env, "alice", { sessionId: s.sessionId, damage: 1_000, victory: true }), "invalid-argument");
    await rejects(submit(env, "alice", { sessionId: "world_boss_forged", damage: 1_000, victory: false }), "not-found");
    await rejects(submit(env, "alice", { sessionId: s.sessionId, damage: 1_000, victory: false, gold: 1 }), "invalid-argument");
    await rejects(submit(env, "bob", { sessionId: s.sessionId, damage: 1_000, victory: false }), "permission-denied");
    await rejects(call(env, "startEndgameAttempt", "alice", { requestId: rid(), kind: "WORLD_BOSS", targetId: bossId, stage: 3 }), "invalid-argument");
    await rejects(call(env, "startEndgameAttempt", "alice", { requestId: rid(), kind: "WORLD_BOSS", targetId: "boss_nope" }), "not-found");
  });

  test("a duplicate boss submission is replayed, not applied twice", async () => {
    const env = makeEnv();
    const s = await start(env, "alice");
    await submit(env, "alice", { sessionId: s.sessionId, damage: 20_000, victory: false });
    const replay = await submit(env, "alice", { sessionId: s.sessionId, damage: 20_000, victory: false }, T0 + 60 * SEC);
    expect(replay.idempotentReplay).toBe(true);
    expect(env.db.peek(P.boss(bossId))!.currentHp).toBe(980_000);
    expect(env.db.peek(P.profile("alice"))!.eventTokens).toBe(25);
  });

  test("daily attempts are capped at 5", async () => {
    const env = makeEnv();
    for (let i = 0; i < 5; i++) await start(env, "alice");
    await rejects(start(env, "alice"), "failed-precondition");
  });

  test("a defeated boss accepts no more attempts; rewards unlock once and only once", async () => {
    const env = makeEnv();
    env.db.seed(P.boss(bossId), { bossId, maxHp: 1_000_000, currentHp: 20_000, totalDamage: 0, participants: 0, status: "ACTIVE", createdAt: T0, updatedAt: T0, defeatedAt: null });
    const s = await start(env, "alice");
    expect(s.enemyHp).toBe(20_000);

    await rejects(call(env, "claimWorldBossReward", "alice", { requestId: rid(), bossId }), "failed-precondition");
    const kill = await submit(env, "alice", { sessionId: s.sessionId, damage: 20_000, victory: true });
    expect(kill.bossDefeated).toBe(true);
    await rejects(start(env, "bob"), "failed-precondition");

    const requestId = rid();
    const claim = await call(env, "claimWorldBossReward", "alice", { requestId, bossId });
    expect(claim).toMatchObject({ success: true, tier: "PARTICIPATION" });
    expect((await call(env, "claimWorldBossReward", "alice", { requestId, bossId })).idempotentReplay).toBe(true);
    await rejects(call(env, "claimWorldBossReward", "alice", { requestId: rid(), bossId }), "already-exists");
    await rejects(call(env, "claimWorldBossReward", "bob", { requestId: rid(), bossId }), "failed-precondition");
  });

  test("reward tier follows total contributed damage after the event ends", async () => {
    const env = makeEnv();
    env.db.seed(P.bossContribution(bossId, "alice"), { uid: "alice", bossId, totalDamage: 600_000, battles: 12, victories: 3, highestHit: 60_000, claimed: false, claimedTier: null });
    const afterEnd = startOfDateKey("2026-10-16") + SEC;
    await expect(call(env, "claimWorldBossReward", "alice", { requestId: rid(), bossId }, afterEnd)).resolves.toMatchObject({ tier: "TOP_5_PERCENT" });
  });

  test("a boss outside its event window cannot be fought", async () => {
    await rejects(start(makeEnv(), "alice", startOfDateKey("2026-10-20")), "failed-precondition");
  });
});

describe("events and the reward outbox", () => {
  const eventId = "event_wrath_of_olympus";
  const claim = (env: Env, uid: string, itemId: string, id = eventId, now = T0) =>
    call(env, "claimEventReward", uid, { requestId: rid(), eventId: id, itemId }, now);
  const withTokens = (env: MemoryGameDb, uid: string, eventTokens: number) =>
    env.seed(P.profile(uid), {
      uid, displayName: "x", createdAt: T0, updatedAt: T0, level: 1, xp: 0, lifetime: { battles: 0, wins: 0, losses: 0, damage: 0 },
      arenaPoints: 0, cardShards: 0, eventTokens, frames: [], cosmetics: [], titles: [],
    });

  test("rewards are paid from a server-held token balance and respect purchase limits", async () => {
    const env = makeEnv();
    withTokens(env.db, "alice", 130);
    const first = await claim(env, "alice", "shop_gold_bounty");
    expect(first).toMatchObject({ success: true, tokensRemaining: 100, purchased: 1 });
    expect(env.db.peek(P.reward("alice", first.claimId))).toMatchObject({ status: "PENDING", gold: 10_000 });

    await claim(env, "alice", "shop_aegis_frame");
    await rejects(claim(env, "alice", "shop_aegis_frame"), "already-exists");
    expect((await call(env, "getEndgameState", "alice")).wallet.eventTokens).toBe(0);
  });

  test("a duplicate request does not pay twice", async () => {
    const env = makeEnv();
    withTokens(env.db, "alice", 100);
    const requestId = rid();
    const body = { requestId, eventId, itemId: "shop_gold_bounty" };
    await call(env, "claimEventReward", "alice", body);
    const replay = await call(env, "claimEventReward", "alice", body);
    expect(replay.idempotentReplay).toBe(true);
    expect(env.db.peek(P.profile("alice"))!.eventTokens).toBe(70);
  });

  test("insufficient tokens, unknown or inactive events and forged items are rejected", async () => {
    const env = makeEnv();
    await rejects(claim(env, "alice", "shop_gold_bounty"), "failed-precondition");
    withTokens(env.db, "alice", 500);
    await rejects(claim(env, "alice", "shop_infinite_gold"), "not-found");
    await rejects(claim(env, "alice", "shop_gold_bounty", "event_fake"), "not-found");
    await rejects(claim(env, "alice", "shop_gold_bounty", "event_ares_trial"), "failed-precondition");
    await rejects(claim(env, "alice", "shop_gold_bounty", eventId, startOfDateKey("2027-01-01")), "failed-precondition");
    await rejects(call(env, "claimEventReward", "alice", { requestId: rid(), eventId, itemId: "shop_gold_bounty", tokens: 0 }), "invalid-argument");
  });

  test("an exclusive card purchase grants ownership on the server", async () => {
    const env = makeEnv();
    withTokens(env.db, "alice", 500);
    await call(env, "ensurePlayerProfile", "bob");
    await claim(env, "alice", "shop_exclusive_card");
    const item = CATALOG.eventShop.find((i) => i.itemId === "shop_exclusive_card")!;
    const before = CATALOG.starter.ownedCardCounts[item.cardId] ?? 0;
    expect(env.db.peek(P.collection("alice"))!.counts[item.cardId]).toBe(before + 1);
  });

  test("pending rewards are listed once and acknowledged only by their owner", async () => {
    const env = makeEnv();
    const m = await startArena(env, "alice");
    const done = await finishArena(env, "alice", m.matchId, "VICTORY");

    const pending = await call(env, "getPendingRewards", "alice");
    expect(pending.rewards).toHaveLength(1);
    expect(pending.rewards[0]).toMatchObject({ claimId: done.claimId, gold: CATALOG.arena.rewards.firstWin.gold });

    expect((await call(env, "ackRewards", "mallory", { claimIds: [done.claimId] })).acknowledged).toBe(0);
    expect((await call(env, "getPendingRewards", "alice")).rewards).toHaveLength(1);

    expect((await call(env, "ackRewards", "alice", { claimIds: [done.claimId, "nonexistent"] })).acknowledged).toBe(1);
    expect((await call(env, "ackRewards", "alice", { claimIds: [done.claimId] })).acknowledged).toBe(0);
    expect((await call(env, "getPendingRewards", "alice")).rewards).toHaveLength(0);
    await rejects(call(env, "ackRewards", "alice", { claimIds: [] }), "invalid-argument");
    await rejects(call(env, "ackRewards", "alice", { claimIds: ["../other"] }), "invalid-argument");
  });
});
