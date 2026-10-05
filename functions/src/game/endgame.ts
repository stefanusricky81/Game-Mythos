import {
  CATALOG, findBoss, findDifficulty, findEvent, findRaid, findShopItem,
} from "./catalog";
import { DocData, GameDb, Tx } from "./db";
import { fail } from "./errors";
import { LIMITS } from "./limits";
import { P } from "./paths";
import { applyContribution, loadAllianceCtx } from "./alliance";
import {
  Grant, applyGrant, emptyGrant, grantCards, loadCollection, loadPlayer, rewardDoc, savePlayer,
} from "./player";
import { ID_PATTERN, asPayload, bool, int, oneOf, reqStr, requestId, strArray } from "./schema";
import { Caller, idempotent, sha } from "./security";
import { dateKey, isWithinDateWindow, windowEndMs } from "./time";

/**
 * AUTHORITY BOUNDARY: the server owns attempts, session issuance, the shared boss HP, per-player
 * contribution, progression (raid stages unlock in order), rewards and claim state. The battle itself
 * is client-side, so `damage` / `cleared` / `victory` are CLAIMS validated against what that session
 * could legitimately produce (enemy HP issued by the server, hard per-attempt caps, minimum battle
 * time, one submit per session) - not proof. See README of the Phase 12 audit.
 */
interface EndgameState {
  date: string;
  wbAttempts: number;
  raidAttempts: number;
  raidProgress: Record<string, number>;
  updatedAt: number;
}

const normalizeEndgame = (stored: DocData | undefined, now: number): EndgameState => {
  const today = dateKey(now);
  const base: EndgameState = stored
    ? ({ ...(stored as EndgameState) })
    : { date: today, wbAttempts: 0, raidAttempts: 0, raidProgress: {}, updatedAt: now };
  if (base.date !== today) {
    base.date = today;
    base.wbAttempts = 0;
    base.raidAttempts = 0;
  }
  return base;
};

interface BossDoc {
  bossId: string; maxHp: number; currentHp: number; totalDamage: number; participants: number;
  status: "ACTIVE" | "DEFEATED"; createdAt: number; updatedAt: number; defeatedAt?: number | null;
}
interface ContributionDoc {
  uid: string; bossId: string; totalDamage: number; battles: number; victories: number;
  highestHit: number; claimed: boolean; claimedTier: string | null;
}

const newBoss = (bossId: string, maxHp: number, now: number): BossDoc => ({
  bossId, maxHp, currentHp: maxHp, totalDamage: 0, participants: 0, status: "ACTIVE", createdAt: now, updatedAt: now, defeatedAt: null,
});

const sessionIdFor = (kind: string, uid: string, reqId: string) => `${kind}_${sha(`${uid}|${reqId}`, 24)}`;

// ---------------------------------------------------------------------------------------------
export const startEndgameAttempt = async (db: GameDb, caller: Caller, data: unknown) => {
  const payload = asPayload(data, ["requestId", "kind", "targetId", "stage", "difficulty"]);
  const reqId = requestId(payload);
  const kind = oneOf(payload, "kind", ["WORLD_BOSS", "RAID"] as const);
  const targetId = reqStr(payload, "targetId", { max: 64, pattern: ID_PATTERN });
  if (kind === "WORLD_BOSS" && (payload.stage !== undefined || payload.difficulty !== undefined)) {
    fail("invalid-argument", "stage and difficulty are only valid for RAID.");
  }
  const stageNumber = kind === "RAID" ? int(payload, "stage", { min: 1, max: 20 }) : 0;
  const difficultyId = kind === "RAID" ? reqStr(payload, "difficulty", { max: 24, pattern: ID_PATTERN }) : "";

  return db.runTransaction((tx) =>
    idempotent(tx, caller, `startEndgame:${kind}`, reqId, async () => {
      const egStored = await tx.get(P.endgame(caller.uid));
      const eg = normalizeEndgame(egStored, caller.now);
      const sessionId = sessionIdFor(kind.toLowerCase(), caller.uid, reqId);

      if (kind === "WORLD_BOSS") {
        const def = findBoss(targetId);
        if (!def) return fail("not-found", "Unknown world boss.");
        if (!isWithinDateWindow(caller.now, def.start, def.end)) return fail("failed-precondition", "This World Boss is not active.");
        const bossStored = (await tx.get(P.boss(def.bossId))) as BossDoc | undefined;
        const boss = bossStored ?? newBoss(def.bossId, def.maxHp, caller.now);
        if (boss.status === "DEFEATED" || boss.currentHp <= 0) return fail("failed-precondition", "The World Boss has already been defeated.");
        if (eg.wbAttempts >= LIMITS.WORLD_BOSS_DAILY_ATTEMPTS) {
          return fail("failed-precondition", "No World Boss attempts remaining today.", { max: LIMITS.WORLD_BOSS_DAILY_ATTEMPTS });
        }
        const enemyHp = Math.min(boss.currentHp, LIMITS.WORLD_BOSS_BATTLE_HP);
        eg.wbAttempts += 1;
        eg.updatedAt = caller.now;
        tx.set(P.endgame(caller.uid), eg as unknown as DocData);
        if (!bossStored) tx.set(P.boss(def.bossId), boss as unknown as DocData);
        tx.set(P.session(sessionId), {
          sessionId, kind: "WORLD_BOSS", uid: caller.uid, status: "ACTIVE", createdAt: caller.now,
          expiresAt: caller.now + LIMITS.SESSION_TTL_MS, bossId: def.bossId, enemyHp,
        });
        return {
          success: true, sessionId, kind, bossId: def.bossId, enemyHp, bossHp: boss.currentHp,
          attemptsRemaining: LIMITS.WORLD_BOSS_DAILY_ATTEMPTS - eg.wbAttempts,
        };
      }

      const raid = findRaid(targetId);
      if (!raid) return fail("not-found", "Unknown raid.");
      const stage = raid.stages.find((s) => s.stage === stageNumber);
      if (!stage) return fail("invalid-argument", "Unknown raid stage.");
      const difficulty = findDifficulty(difficultyId);
      if (!difficulty) return fail("invalid-argument", "Unknown difficulty.");

      const alliance = await loadAllianceCtx(tx, caller.uid);
      if (!alliance.allianceId) return fail("failed-precondition", "Raids require Alliance membership.");
      const unlocked = (eg.raidProgress[raid.raidId] ?? 0) + 1;
      if (stageNumber > unlocked) return fail("failed-precondition", "That raid stage is still locked.", { unlockedStage: unlocked });
      if (eg.raidAttempts >= LIMITS.RAID_DAILY_ATTEMPTS) {
        return fail("failed-precondition", "No daily raid attempts remaining.", { max: LIMITS.RAID_DAILY_ATTEMPTS });
      }

      const enemyHp = Math.floor(stage.hp * difficulty.hpMultiplier);
      eg.raidAttempts += 1;
      eg.updatedAt = caller.now;
      tx.set(P.endgame(caller.uid), eg as unknown as DocData);
      tx.set(P.session(sessionId), {
        sessionId, kind: "RAID", uid: caller.uid, status: "ACTIVE", createdAt: caller.now,
        expiresAt: caller.now + LIMITS.SESSION_TTL_MS, raidId: raid.raidId, stage: stageNumber,
        difficulty: difficulty.id, enemyHp, allianceId: alliance.allianceId,
      });
      return {
        success: true, sessionId, kind, raidId: raid.raidId, stage: stageNumber, difficulty: difficulty.id, enemyHp,
        attemptsRemaining: LIMITS.RAID_DAILY_ATTEMPTS - eg.raidAttempts,
      };
    })
  );
};

/** Shared session checks for submit endpoints. */
const checkSession = (session: DocData | undefined, caller: Caller, kind: string): DocData => {
  if (!session) return fail("not-found", "Unknown session.");
  if (session.kind !== kind || session.uid !== caller.uid) return fail("permission-denied", "This session does not belong to the authenticated player.");
  return session;
};

const assertSubmittable = (session: DocData, caller: Caller): void => {
  if (session.status !== "ACTIVE") fail("failed-precondition", `Session is ${session.status}.`);
  if (caller.now > session.expiresAt) fail("failed-precondition", "Session expired.");
  if (caller.now - session.createdAt < LIMITS.MIN_BATTLE_MS) fail("failed-precondition", "Result submitted faster than a real battle can finish.");
};

// ---------------------------------------------------------------------------------------------
export const submitWorldBossContribution = async (db: GameDb, caller: Caller, data: unknown) => {
  const payload = asPayload(data, ["sessionId", "damage", "victory"]);
  const sessionId = reqStr(payload, "sessionId", { max: 64, pattern: ID_PATTERN });
  const damage = int(payload, "damage", { min: 0, max: LIMITS.WORLD_BOSS_MAX_DAMAGE_PER_ATTEMPT });
  const victory = bool(payload, "victory");

  return db.runTransaction(async (tx) => {
    const session = checkSession(await tx.get(P.session(sessionId)), caller, "WORLD_BOSS");
    if (session.status === "COMPLETED") return { ...(session.response as DocData), idempotentReplay: true };
    assertSubmittable(session, caller);
    if (victory && damage < session.enemyHp) fail("invalid-argument", "Victory was claimed without enough damage to defeat the boss.");

    const def = findBoss(session.bossId);
    if (!def) return fail("not-found", "Unknown world boss.");
    const bossStored = (await tx.get(P.boss(def.bossId))) as BossDoc | undefined;
    const contribStored = (await tx.get(P.bossContribution(def.bossId, caller.uid))) as ContributionDoc | undefined;
    const player = await loadPlayer(tx, caller.uid, caller.now);
    const alliance = await loadAllianceCtx(tx, caller.uid);

    const boss = bossStored ?? newBoss(def.bossId, def.maxHp, caller.now);
    const before = boss.currentHp;
    boss.currentHp = Math.max(0, before - damage);
    boss.totalDamage += damage;
    boss.participants += contribStored ? 0 : 1;
    if (boss.currentHp === 0 && boss.status !== "DEFEATED") { boss.status = "DEFEATED"; boss.defeatedAt = caller.now; }
    boss.updatedAt = caller.now;

    const contrib: ContributionDoc = contribStored ?? {
      uid: caller.uid, bossId: def.bossId, totalDamage: 0, battles: 0, victories: 0, highestHit: 0, claimed: false, claimedTier: null,
    };
    contrib.totalDamage += damage;
    contrib.battles += 1;
    contrib.victories += victory ? 1 : 0;
    contrib.highestHit = Math.max(contrib.highestHit, damage);

    const grant: Grant = {
      ...emptyGrant(),
      eventTokens: LIMITS.WORLD_BOSS_PARTICIPATION_TOKENS,
      gold: victory ? LIMITS.WORLD_BOSS_VICTORY_GOLD : 0,
      xp: victory ? LIMITS.WORLD_BOSS_VICTORY_XP : 0,
    };
    player.profile.lifetime.battles += 1;
    player.profile.lifetime.wins += victory ? 1 : 0;
    player.profile.lifetime.losses += victory ? 0 : 1;
    player.profile.lifetime.damage += damage;
    applyGrant(player.profile, grant);

    const claimId = `wb_${sessionId}`;
    const response = {
      success: true,
      sessionId,
      bossId: def.bossId,
      damageApplied: before - boss.currentHp,
      bossHpRemaining: boss.currentHp,
      bossDefeated: boss.status === "DEFEATED",
      contribution: { totalDamage: contrib.totalDamage, battles: contrib.battles, victories: contrib.victories, highestHit: contrib.highestHit },
      rewards: { gold: grant.gold, xp: grant.xp, eventTokens: grant.eventTokens },
      claimId,
    };

    const alliancePart = applyContribution(tx, alliance, caller.uid, victory ? 1 : 0, damage, caller.now);
    savePlayer(tx, player, caller.now);
    tx.set(P.boss(def.bossId), boss as unknown as DocData);
    tx.set(P.bossContribution(def.bossId, caller.uid), contrib as unknown as DocData);
    tx.merge(P.session(sessionId), { status: "COMPLETED", completedAt: caller.now, response: { ...response, allianceScore: alliancePart.score } });
    tx.set(P.reward(caller.uid, claimId), rewardDoc(claimId, "WORLD_BOSS_BATTLE", sessionId, grant, caller.now));
    return { ...response, allianceScore: alliancePart.score };
  });
};

// ---------------------------------------------------------------------------------------------
export const submitRaidContribution = async (db: GameDb, caller: Caller, data: unknown) => {
  const payload = asPayload(data, ["sessionId", "damage", "cleared"]);
  const sessionId = reqStr(payload, "sessionId", { max: 64, pattern: ID_PATTERN });
  const cleared = bool(payload, "cleared");

  return db.runTransaction(async (tx) => {
    const session = checkSession(await tx.get(P.session(sessionId)), caller, "RAID");
    if (session.status === "COMPLETED") return { ...(session.response as DocData), idempotentReplay: true };
    assertSubmittable(session, caller);
    // Damage cannot meaningfully exceed the enemy the SERVER issued for this session (2x = overkill margin).
    const damage = int(payload, "damage", { min: 0, max: session.enemyHp * 2 });
    if (cleared && damage < session.enemyHp) fail("invalid-argument", "Stage clear was claimed without enough damage to defeat the enemy.");

    const raid = findRaid(session.raidId);
    const stage = raid?.stages.find((s) => s.stage === session.stage);
    const difficulty = findDifficulty(session.difficulty);
    if (!raid || !stage || !difficulty) return fail("internal", "Session references unknown raid data.");

    const egStored = await tx.get(P.endgame(caller.uid));
    const aggPath = P.allianceRaid(session.allianceId, raid.raidId);
    const agg = ((await tx.get(aggPath)) as DocData | undefined) ?? { allianceId: session.allianceId, raidId: raid.raidId, totalDamage: 0, clears: {} };
    const player = await loadPlayer(tx, caller.uid, caller.now);
    const alliance = await loadAllianceCtx(tx, caller.uid);
    if (alliance.allianceId !== session.allianceId) return fail("failed-precondition", "You are no longer in the Alliance this raid attempt was started for.");

    const eg = normalizeEndgame(egStored, caller.now);
    const grant: Grant = { ...emptyGrant() };
    if (cleared) {
      grant.gold = Math.floor(stage.gold * difficulty.rewardMultiplier);
      grant.xp = Math.floor(stage.xp * difficulty.rewardMultiplier);
      grant.eventTokens = stage.eventTokens;
      eg.raidProgress[raid.raidId] = Math.max(eg.raidProgress[raid.raidId] ?? 0, session.stage);
      agg.clears[String(session.stage)] = (agg.clears[String(session.stage)] ?? 0) + 1;
    }
    agg.totalDamage += damage;
    eg.updatedAt = caller.now;

    player.profile.lifetime.battles += 1;
    player.profile.lifetime.wins += cleared ? 1 : 0;
    player.profile.lifetime.losses += cleared ? 0 : 1;
    player.profile.lifetime.damage += damage;
    applyGrant(player.profile, grant);

    const claimId = `raid_${sessionId}`;
    const alliancePart = applyContribution(tx, alliance, caller.uid, cleared ? 1 : 0, damage, caller.now);
    const response = {
      success: true,
      sessionId,
      raidId: raid.raidId,
      stage: session.stage,
      cleared,
      progress: eg.raidProgress[raid.raidId] ?? 0,
      rewards: { gold: grant.gold, xp: grant.xp, eventTokens: grant.eventTokens },
      allianceScore: alliancePart.score,
      claimId,
    };
    savePlayer(tx, player, caller.now);
    tx.set(P.endgame(caller.uid), eg as unknown as DocData);
    tx.set(aggPath, agg);
    tx.merge(P.session(sessionId), { status: "COMPLETED", completedAt: caller.now, response });
    tx.set(P.reward(caller.uid, claimId), rewardDoc(claimId, "RAID_STAGE", sessionId, grant, caller.now));
    return response;
  });
};

// ---------------------------------------------------------------------------------------------
export const claimWorldBossReward = async (db: GameDb, caller: Caller, data: unknown) => {
  const payload = asPayload(data, ["requestId", "bossId"]);
  const reqId = requestId(payload);
  const bossId = reqStr(payload, "bossId", { max: 64, pattern: ID_PATTERN });

  return db.runTransaction((tx) =>
    idempotent(tx, caller, "claimWorldBossReward", reqId, async () => {
      const def = findBoss(bossId);
      if (!def) return fail("not-found", "Unknown world boss.");
      const boss = (await tx.get(P.boss(bossId))) as BossDoc | undefined;
      const contrib = (await tx.get(P.bossContribution(bossId, caller.uid))) as ContributionDoc | undefined;
      const player = await loadPlayer(tx, caller.uid, caller.now);

      if (!contrib || contrib.battles <= 0) return fail("failed-precondition", "No participation recorded for this boss.");
      if (contrib.claimed) return fail("already-exists", "World Boss rewards have already been claimed.");
      const over = boss?.status === "DEFEATED" || caller.now >= windowEndMs(def.end);
      if (!over) return fail("failed-precondition", "World Boss rewards unlock once the boss is defeated or the event ends.");

      const tier = [...CATALOG.worldBossTiers].sort((a, b) => b.minDamage - a.minDamage).find((t) => contrib.totalDamage >= t.minDamage);
      if (!tier) return fail("internal", "No reward tier configured.");
      const grant: Grant = {
        ...emptyGrant(),
        gold: tier.gold,
        eventTokens: tier.eventTokens,
        cardShards: tier.cardShards,
        heroShards: tier.heroShards > 0 ? { [CATALOG.starter.heroId]: tier.heroShards } : {},
      };
      applyGrant(player.profile, grant);
      savePlayer(tx, player, caller.now);
      tx.merge(P.bossContribution(bossId, caller.uid), { claimed: true, claimedTier: tier.id, claimedAt: caller.now });
      const claimId = `wb_reward_${bossId}`;
      tx.set(P.reward(caller.uid, claimId), rewardDoc(claimId, "WORLD_BOSS_REWARD", bossId, grant, caller.now));
      return { success: true, bossId, tier: tier.id, claimId, rewards: { gold: tier.gold, eventTokens: tier.eventTokens, heroShards: tier.heroShards, cardShards: tier.cardShards } };
    })
  );
};

// ---------------------------------------------------------------------------------------------
export const claimEventReward = async (db: GameDb, caller: Caller, data: unknown) => {
  const payload = asPayload(data, ["requestId", "eventId", "itemId"]);
  const reqId = requestId(payload);
  const eventId = reqStr(payload, "eventId", { max: 64, pattern: ID_PATTERN });
  const itemId = reqStr(payload, "itemId", { max: 64, pattern: ID_PATTERN });

  return db.runTransaction((tx) =>
    idempotent(tx, caller, "claimEventReward", reqId, async () => {
      const ev = findEvent(eventId);
      if (!ev) return fail("not-found", "Unknown event.");
      if (!ev.active || !isWithinDateWindow(caller.now, ev.start, ev.end)) return fail("failed-precondition", "This event is not active.");
      const item = findShopItem(itemId);
      if (!item) return fail("not-found", "Unknown event reward.");

      const eventDoc = ((await tx.get(P.event(caller.uid, eventId))) as DocData | undefined) ?? { eventId, purchases: {} };
      const player = await loadPlayer(tx, caller.uid, caller.now);
      const counts = item.category === "EXCLUSIVE_CARD" ? await loadCollection(tx, caller.uid) : undefined;

      const bought = (eventDoc.purchases[itemId] as number | undefined) ?? 0;
      if (bought >= item.limit) return fail("already-exists", "Purchase limit reached for this reward.", { limit: item.limit });
      if (player.profile.eventTokens < item.tokenPrice) {
        return fail("failed-precondition", "Insufficient event tokens.", { required: item.tokenPrice, balance: player.profile.eventTokens });
      }

      const grant: Grant = {
        ...emptyGrant(),
        gold: item.gold,
        heroShards: item.heroShards > 0 ? { [item.heroId]: item.heroShards } : {},
        cardShards: item.cardShards,
        cosmetics: item.cosmeticId ? [item.cosmeticId] : [],
        cards: item.category === "EXCLUSIVE_CARD" ? { [item.cardId]: 1 } : {},
      };
      player.profile.eventTokens -= item.tokenPrice;
      applyGrant(player.profile, grant);
      savePlayer(tx, player, caller.now);
      if (counts) tx.set(P.collection(caller.uid), { counts: grantCards(counts, grant.cards), attestedAt: null, updatedAt: caller.now });
      eventDoc.purchases[itemId] = bought + 1;
      tx.set(P.event(caller.uid, eventId), { ...eventDoc, updatedAt: caller.now });
      const claimId = `event_${eventId}_${itemId}_${bought + 1}`;
      tx.set(P.reward(caller.uid, claimId), rewardDoc(claimId, "EVENT_SHOP", `${eventId}:${itemId}`, grant, caller.now));
      return { success: true, eventId, itemId, claimId, tokensRemaining: player.profile.eventTokens, purchased: bought + 1, limit: item.limit };
    })
  );
};

// ---------------------------------------------------------------------------------------------
export const getEndgameState = async (db: GameDb, caller: Caller, data: unknown) => {
  asPayload(data, []);
  const now = caller.now;
  const [profile, egStored] = await Promise.all([db.get(P.profile(caller.uid)), db.get(P.endgame(caller.uid))]);
  const eg = normalizeEndgame(egStored, now);

  const bosses = await Promise.all(CATALOG.worldBosses.map(async (def) => {
    const [boss, contrib] = await Promise.all([db.get(P.boss(def.bossId)), db.get(P.bossContribution(def.bossId, caller.uid))]);
    const active = isWithinDateWindow(now, def.start, def.end);
    return {
      bossId: def.bossId, maxHp: def.maxHp, currentHp: (boss?.currentHp as number | undefined) ?? def.maxHp,
      defeated: boss?.status === "DEFEATED", active, start: def.start, end: def.end,
      rewardsUnlocked: boss?.status === "DEFEATED" || now >= windowEndMs(def.end),
      mine: contrib ? {
        totalDamage: contrib.totalDamage, battles: contrib.battles, victories: contrib.victories,
        highestHit: contrib.highestHit, claimed: contrib.claimed, claimedTier: contrib.claimedTier,
      } : null,
    };
  }));

  const activeEvents = CATALOG.events.filter((e) => e.active && isWithinDateWindow(now, e.start, e.end));
  const purchases: Record<string, Record<string, number>> = {};
  await Promise.all(activeEvents.map(async (e) => {
    const doc = await db.get(P.event(caller.uid, e.eventId));
    purchases[e.eventId] = (doc?.purchases as Record<string, number> | undefined) ?? {};
  }));

  return {
    success: true,
    wallet: {
      eventTokens: (profile?.eventTokens as number | undefined) ?? 0,
      cardShards: (profile?.cardShards as number | undefined) ?? 0,
      arenaPoints: (profile?.arenaPoints as number | undefined) ?? 0,
    },
    attempts: {
      worldBossRemaining: LIMITS.WORLD_BOSS_DAILY_ATTEMPTS - eg.wbAttempts,
      worldBossMax: LIMITS.WORLD_BOSS_DAILY_ATTEMPTS,
      raidRemaining: LIMITS.RAID_DAILY_ATTEMPTS - eg.raidAttempts,
      raidMax: LIMITS.RAID_DAILY_ATTEMPTS,
    },
    raidProgress: eg.raidProgress,
    worldBosses: bosses,
    activeEvents: activeEvents.map((e) => ({ eventId: e.eventId, type: e.type, start: e.start, end: e.end })),
    eventPurchases: purchases,
  };
};

// ---------------------------------------------------------------------------------------------
// Reward outbox
// ---------------------------------------------------------------------------------------------
export const getPendingRewards = async (db: GameDb, caller: Caller, data: unknown) => {
  asPayload(data, []);
  const pending = await db.list(P.rewards(caller.uid), { where: [["status", "==", "PENDING"]], limit: LIMITS.PENDING_REWARDS_LIMIT });
  return {
    success: true,
    rewards: pending
      .map((r) => r.data)
      .sort((a, b) => a.createdAt - b.createdAt)
      .map((r) => ({ claimId: r.claimId, source: r.source, gold: r.gold, xp: r.xp, heroShards: r.heroShards, summary: r.summary, createdAt: r.createdAt })),
  };
};

export const ackRewards = async (db: GameDb, caller: Caller, data: unknown) => {
  const payload = asPayload(data, ["claimIds"]);
  const claimIds = strArray(payload, "claimIds", { min: 1, max: LIMITS.PENDING_REWARDS_LIMIT, item: ID_PATTERN });
  return db.runTransaction(async (tx: Tx) => {
    const docs = await Promise.all([...new Set(claimIds)].map(async (id) => ({ id, doc: await tx.get(P.reward(caller.uid, id)) })));
    let acknowledged = 0;
    for (const { id, doc } of docs) {
      if (doc && doc.status === "PENDING") {
        tx.merge(P.reward(caller.uid, id), { status: "APPLIED", appliedAt: caller.now });
        acknowledged += 1;
      }
    }
    return { success: true, acknowledged };
  });
};
