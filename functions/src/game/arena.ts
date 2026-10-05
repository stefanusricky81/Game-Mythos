import {
  CATALOG, Outcome, OUTCOMES, applyRating, eloDelta, findOpponent, seasonResetRating, tierOf,
} from "./catalog";
import { DocData, GameDb, Tx } from "./db";
import { fail } from "./errors";
import { LIMITS } from "./limits";
import { P } from "./paths";
import {
  Grant, applyGrant, checkDeck, emptyGrant, loadCollection, loadPlayer, parseDeck, rewardDoc, savePlayer,
} from "./player";
import { ID_PATTERN, asPayload, int, oneOf, reqStr, requestId } from "./schema";
import { Caller, idempotent, sha } from "./security";
import { dateKey, seasonOf } from "./time";

/**
 * AUTHORITY BOUNDARY (read this before trusting any number here):
 *  - Server-authoritative: match creation and ID, opponent choice, daily attempt consumption, rating
 *    before/after, reward amounts, streaks, history, seasons, leaderboard, replay protection.
 *  - NOT server-authoritative: the battle itself. The card battle runs on the client, so the server
 *    receives the claimed outcome. It is bounded (a match must have been issued to this player, be
 *    ACTIVE, un-replayed, not submitted faster than a real battle could finish, and attempts are
 *    capped at 5/day) but a modified client can still claim VICTORY for matches it actually plays.
 *    Closing that gap needs a deterministic server-side battle simulation or replay verification.
 */
export interface ArenaState {
  rating: number;
  peakRating: number;
  wins: number;
  losses: number;
  streak: number;
  bestStreak: number;
  dailyAttempts: number;
  attemptsDate: string;
  firstWinDate: string;
  seasonId: string;
  activeMatchId: string | null;
  updatedAt: number;
}

export interface SeasonArchive {
  seasonId: string;
  finalRating: number;
  peakRating: number;
  wins: number;
  losses: number;
  tier: string;
  claimed: boolean;
  archivedAt: number;
}

const defaultArena = (now: number): ArenaState => ({
  rating: CATALOG.arena.startingRating,
  peakRating: CATALOG.arena.startingRating,
  wins: 0,
  losses: 0,
  streak: 0,
  bestStreak: 0,
  dailyAttempts: CATALOG.arena.maxDailyAttempts,
  attemptsDate: dateKey(now),
  firstWinDate: "",
  seasonId: seasonOf(now, CATALOG.arena.seasonDays).id,
  activeMatchId: null,
  updatedAt: now,
});

/** Applies the daily-attempt reset and the season rollover (pure; the caller persists the result). */
export const normalizeArena = (stored: DocData | undefined, now: number): { state: ArenaState; archive?: SeasonArchive } => {
  const base: ArenaState = stored ? ({ ...(stored as ArenaState) }) : defaultArena(now);
  const season = seasonOf(now, CATALOG.arena.seasonDays);
  let archive: SeasonArchive | undefined;

  if (stored && base.seasonId !== season.id) {
    archive = {
      seasonId: base.seasonId,
      finalRating: base.rating,
      peakRating: base.peakRating,
      wins: base.wins,
      losses: base.losses,
      tier: tierOf(base.rating).name,
      claimed: false,
      archivedAt: now,
    };
    const reset = seasonResetRating(base.rating);
    base.rating = reset;
    base.peakRating = reset;
    base.wins = 0;
    base.losses = 0;
    base.streak = 0;
    base.firstWinDate = "";
    base.seasonId = season.id;
  }

  const today = dateKey(now);
  if (base.attemptsDate !== today) {
    base.dailyAttempts = CATALOG.arena.maxDailyAttempts;
    base.attemptsDate = today;
  }
  return { state: base, archive };
};

const loadArena = async (tx: Tx, uid: string, now: number) =>
  normalizeArena(await tx.get(P.arena(uid)), now);

const stateView = (s: ArenaState, now: number) => {
  const season = seasonOf(now, CATALOG.arena.seasonDays);
  return {
    rating: s.rating,
    peakRating: s.peakRating,
    tier: tierOf(s.rating).name,
    wins: s.wins,
    losses: s.losses,
    streak: s.streak,
    bestStreak: s.bestStreak,
    dailyAttempts: s.dailyAttempts,
    maxDailyAttempts: CATALOG.arena.maxDailyAttempts,
    firstWinAvailable: s.firstWinDate !== dateKey(now),
    seasonId: s.seasonId,
    seasonEndsAt: season.endMs,
    activeMatchId: s.activeMatchId,
  };
};

const persistArena = (tx: Tx, uid: string, state: ArenaState, archive: SeasonArchive | undefined, now: number): void => {
  state.updatedAt = now;
  tx.set(P.arena(uid), state as unknown as DocData);
  if (archive) tx.set(P.arenaSeason(uid, archive.seasonId), archive as unknown as DocData);
};

const opponentView = (o: ReturnType<typeof findOpponent>) => ({
  id: o.id, name: o.name, heroId: o.heroId, heroName: o.heroName, archetype: o.archetype,
  title: o.title, rating: o.rating, combatPower: o.combatPower, deck: o.deck,
});

// ---------------------------------------------------------------------------------------------
export const startArenaMatch = async (db: GameDb, caller: Caller, data: unknown) => {
  const payload = asPayload(data, ["requestId", "heroId", "cardIds"]);
  const reqId = requestId(payload);
  const deck = parseDeck(payload);

  return db.runTransaction((tx) =>
    idempotent(tx, caller, "startArenaMatch", reqId, async () => {
      const player = await loadPlayer(tx, caller.uid, caller.now);
      const owned = await loadCollection(tx, caller.uid);
      const { state, archive } = await loadArena(tx, caller.uid, caller.now);
      const activeSession = state.activeMatchId ? await tx.get(P.session(state.activeMatchId)) : undefined;

      // Resume an in-progress match instead of charging a second attempt (also makes double taps safe).
      if (activeSession && activeSession.status === "ACTIVE" && caller.now <= activeSession.expiresAt) {
        return {
          success: true,
          resumed: true,
          matchId: activeSession.sessionId as string,
          opponent: activeSession.opponent,
          ratingBefore: activeSession.ratingBefore as number,
          remainingAttempts: state.dailyAttempts,
          expiresAt: activeSession.expiresAt as number,
        };
      }

      const issues = checkDeck(deck.heroId, deck.cardIds, owned);
      if (issues.length > 0) fail("invalid-argument", "Deck is not valid for Arena.", { issues });
      if (state.dailyAttempts <= 0) {
        fail("failed-precondition", "No daily Arena attempts remaining.", { resetsAt: seasonOf(caller.now, 1).endMs });
      }

      if (activeSession && activeSession.status === "ACTIVE") {
        tx.merge(P.session(state.activeMatchId as string), { status: "EXPIRED", closedAt: caller.now });
      }

      const matchId = `arena_${sha(`${caller.uid}|${reqId}`, 24)}`;
      const opponent = opponentView(findOpponent(state.rating));
      const expiresAt = caller.now + LIMITS.SESSION_TTL_MS;

      state.dailyAttempts -= 1;
      state.activeMatchId = matchId;

      persistArena(tx, caller.uid, state, archive, caller.now);
      tx.set(P.session(matchId), {
        sessionId: matchId,
        kind: "ARENA",
        uid: caller.uid,
        status: "ACTIVE",
        createdAt: caller.now,
        expiresAt,
        seasonId: state.seasonId,
        ratingBefore: state.rating,
        opponent,
        deckHash: sha(`${deck.heroId}|${[...deck.cardIds].sort().join(",")}`, 16),
      });
      savePlayer(tx, player, caller.now);

      return {
        success: true,
        resumed: false,
        matchId,
        opponent,
        ratingBefore: state.rating,
        remainingAttempts: state.dailyAttempts,
        expiresAt,
      };
    })
  );
};

// ---------------------------------------------------------------------------------------------
export const submitArenaResult = async (db: GameDb, caller: Caller, data: unknown) => {
  // Deliberately strict: there is no place for a client to put a reward, a rating or an opponent.
  const payload = asPayload(data, ["matchId", "outcome", "turns", "damageDealt", "damageTaken"]);
  const matchId = reqStr(payload, "matchId", { max: 64, pattern: ID_PATTERN });
  const outcome: Outcome = oneOf(payload, "outcome", OUTCOMES);
  const turns = int(payload, "turns", { min: 1, max: LIMITS.MAX_TURNS });
  const damageDealt = int(payload, "damageDealt", { min: 0, max: LIMITS.MAX_BATTLE_DAMAGE });
  const damageTaken = int(payload, "damageTaken", { min: 0, max: LIMITS.MAX_BATTLE_DAMAGE });

  return db.runTransaction(async (tx) => {
    const session = await tx.get(P.session(matchId));
    if (!session) return fail("not-found", "Unknown match.");
    if (session.kind !== "ARENA" || session.uid !== caller.uid) {
      return fail("permission-denied", "This match does not belong to the authenticated player.");
    }
    if (session.status === "COMPLETED") return { ...(session.response as DocData), idempotentReplay: true };
    if (session.status !== "ACTIVE") return fail("failed-precondition", `Match is ${session.status}.`);
    if (caller.now > session.expiresAt) return fail("failed-precondition", "Match expired.");
    if (caller.now - session.createdAt < LIMITS.MIN_BATTLE_MS) {
      return fail("failed-precondition", "Result submitted faster than a real battle can finish.");
    }

    const player = await loadPlayer(tx, caller.uid, caller.now);
    const { state, archive } = await loadArena(tx, caller.uid, caller.now);

    const ratingBefore = state.rating;
    const ratingDelta = eloDelta(ratingBefore, session.opponent.rating, outcome);
    const ratingAfter = applyRating(ratingBefore, ratingDelta);
    const tierBefore = tierOf(ratingBefore).name;
    const tierAfter = tierOf(ratingAfter).name;

    const victory = outcome === "VICTORY";
    const today = dateKey(caller.now);
    const streak = victory ? state.streak + 1 : 0;
    const isFirstWin = victory && state.firstWinDate !== today;
    const { rewards: table } = CATALOG.arena;
    const base = victory ? (isFirstWin ? table.firstWin : table.normalWin) : table.defeat;
    const bonus = victory ? table.streak[String(streak)] ?? { gold: 0, arenaPoints: 0 } : { gold: 0, arenaPoints: 0 };

    const grant: Grant = {
      ...emptyGrant(),
      gold: base.gold + bonus.gold,
      xp: base.xp,
      cardShards: base.cardShards,
      arenaPoints: base.arenaPoints + bonus.arenaPoints,
    };

    state.rating = ratingAfter;
    state.peakRating = Math.max(state.peakRating, ratingAfter);
    state.wins += victory ? 1 : 0;
    state.losses += outcome === "DEFEAT" ? 1 : 0;
    state.streak = streak;
    state.bestStreak = Math.max(state.bestStreak, streak);
    state.firstWinDate = isFirstWin ? today : state.firstWinDate;
    state.activeMatchId = null;

    player.profile.lifetime.battles += 1;
    player.profile.lifetime.wins += victory ? 1 : 0;
    player.profile.lifetime.losses += outcome === "DEFEAT" ? 1 : 0;
    player.profile.lifetime.damage += damageDealt;
    applyGrant(player.profile, grant);

    const claimId = `arena_${matchId}`;
    const rewardSummary = {
      gold: grant.gold, xp: grant.xp, cardShards: grant.cardShards, arenaPoints: grant.arenaPoints,
      isFirstWin, streak, streakBonusGold: bonus.gold, streakBonusPoints: bonus.arenaPoints,
    };
    const response = {
      success: true,
      matchId,
      outcome,
      ratingBefore,
      ratingAfter,
      ratingDelta,
      tierBefore,
      tierAfter,
      rewards: rewardSummary,
      claimId,
      remainingAttempts: state.dailyAttempts,
      arena: stateView(state, caller.now),
    };

    persistArena(tx, caller.uid, state, archive, caller.now);
    savePlayer(tx, player, caller.now);
    tx.merge(P.session(matchId), { status: "COMPLETED", completedAt: caller.now, outcome, response });
    tx.set(P.arenaMatch(caller.uid, matchId), {
      matchId,
      seasonId: state.seasonId,
      opponent: { id: session.opponent.id, name: session.opponent.name, heroName: session.opponent.heroName, archetype: session.opponent.archetype, rating: session.opponent.rating },
      outcome, ratingBefore, ratingAfter, ratingDelta, turns, damageDealt, damageTaken,
      rewards: rewardSummary,
      completedAt: caller.now,
    });
    tx.set(P.leaderboardEntry(state.seasonId, caller.uid), {
      uid: caller.uid,
      displayName: player.profile.displayName,
      rating: ratingAfter,
      peakRating: state.peakRating,
      wins: state.wins,
      losses: state.losses,
      tier: tierAfter,
      updatedAt: caller.now,
    });
    tx.set(P.reward(caller.uid, claimId), rewardDoc(claimId, "ARENA_MATCH", matchId, grant, caller.now));
    return response;
  });
};

// ---------------------------------------------------------------------------------------------
export const claimArenaReward = async (db: GameDb, caller: Caller, data: unknown) => {
  const payload = asPayload(data, ["requestId", "seasonId"]);
  const reqId = requestId(payload);
  const seasonId = reqStr(payload, "seasonId", { max: 24, pattern: /^season_[0-9]{1,6}$/ });

  return db.runTransaction((tx) =>
    idempotent(tx, caller, "claimArenaReward", reqId, async () => {
      const current = seasonOf(caller.now, CATALOG.arena.seasonDays);
      const number = Number(seasonId.slice("season_".length));
      if (number >= current.number) fail("failed-precondition", "That season has not ended yet.");

      const stored = await tx.get(P.arenaSeason(caller.uid, seasonId));
      const arenaDoc = await tx.get(P.arena(caller.uid));
      const player = await loadPlayer(tx, caller.uid, caller.now);

      // The previous season is archived lazily on the next arena action; make sure a player who
      // never played again can still claim it.
      const normalized = normalizeArena(arenaDoc, caller.now);
      const archive: SeasonArchive | undefined =
        (stored as SeasonArchive | undefined) ?? (normalized.archive?.seasonId === seasonId ? normalized.archive : undefined);
      if (!archive) return fail("not-found", "No ranked record exists for that season.");
      if (archive.claimed) return fail("already-exists", "Season reward already claimed.");

      const reward = CATALOG.arena.seasonRewards[archive.tier];
      const grant: Grant = {
        ...emptyGrant(),
        gold: reward.gold,
        cardShards: reward.cardShards,
        heroShards: reward.heroShards > 0 ? { [CATALOG.starter.heroId]: reward.heroShards } : {},
        frames: reward.frameId ? [reward.frameId] : [],
        cosmetics: reward.cosmeticId ? [reward.cosmeticId] : [],
        titles: reward.title ? [reward.title] : [],
      };
      applyGrant(player.profile, grant);

      if (normalized.archive && !stored) persistArena(tx, caller.uid, normalized.state, undefined, caller.now);
      tx.set(P.arenaSeason(caller.uid, seasonId), { ...archive, claimed: true, claimedAt: caller.now });
      savePlayer(tx, player, caller.now);
      const claimId = `arena_season_${seasonId}`;
      tx.set(P.reward(caller.uid, claimId), rewardDoc(claimId, "ARENA_SEASON", seasonId, grant, caller.now));

      return {
        success: true,
        alreadyClaimed: false,
        seasonId,
        tier: archive.tier,
        claimId,
        reward: {
          gold: reward.gold, cardShards: reward.cardShards, heroShards: reward.heroShards,
          frameId: reward.frameId, cosmeticId: reward.cosmeticId, title: reward.title,
        },
      };
    })
  );
};

// ---------------------------------------------------------------------------------------------
export const getArenaState = async (db: GameDb, caller: Caller, data: unknown) => {
  asPayload(data, []);
  const [stored, history, archives] = await Promise.all([
    db.get(P.arena(caller.uid)),
    db.list(P.arenaMatches(caller.uid), { orderBy: { field: "completedAt", direction: "desc" }, limit: LIMITS.HISTORY_LIMIT }),
    db.list(`players/${caller.uid}/arenaSeasons`, { where: [["claimed", "==", false]] }),
  ]);
  const { state, archive } = normalizeArena(stored, caller.now);
  const claimable = [...archives.map((a) => a.data as unknown as SeasonArchive), ...(archive ? [archive] : [])]
    .filter((a) => !a.claimed)
    .map((a) => ({ seasonId: a.seasonId, finalRating: a.finalRating, tier: a.tier }));

  return {
    success: true,
    state: stateView(state, caller.now),
    history: history.map((h) => h.data),
    claimableSeasons: claimable,
  };
};

export const getLeaderboard = async (db: GameDb, caller: Caller, data: unknown) => {
  const payload = asPayload(data, ["seasonId", "limit"]);
  const seasonId = payload.seasonId === undefined
    ? seasonOf(caller.now, CATALOG.arena.seasonDays).id
    : reqStr(payload, "seasonId", { max: 24, pattern: /^season_[0-9]{1,6}$/ });
  const limit = payload.limit === undefined ? 50 : int(payload, "limit", { min: 1, max: LIMITS.LEADERBOARD_MAX_LIMIT });

  const [top, mine] = await Promise.all([
    db.list(P.leaderboard(seasonId), { orderBy: { field: "rating", direction: "desc" }, limit }),
    db.get(P.leaderboardEntry(seasonId, caller.uid)),
  ]);
  const view = (e: DocData, rank: number) => ({
    rank, uid: e.uid, displayName: e.displayName, rating: e.rating, peakRating: e.peakRating,
    wins: e.wins, losses: e.losses, tier: e.tier,
  });
  const myRank = mine ? 1 + (await db.count(P.leaderboard(seasonId), [["rating", ">", mine.rating]])) : null;

  return {
    success: true,
    seasonId,
    entries: top.map((e, i) => view(e.data, i + 1)),
    me: mine && myRank !== null ? view(mine, myRank) : null,
  };
};
