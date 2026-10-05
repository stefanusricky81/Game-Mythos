/**
 * Server-owned gameplay limits. These are NOT in gameCatalog.json (which mirrors the client catalogs)
 * because they are anti-abuse rules that only the server enforces. Keep the comments: they record why
 * each number is what it is, so a reviewer can judge it without reading the client.
 */
export const LIMITS = {
  /** Arena battles are simulated on the client; a real match cannot finish faster than this. */
  MIN_BATTLE_MS: 15_000,
  /** A started session that is never submitted is dead after this long (attempt is NOT refunded). */
  SESSION_TTL_MS: 2 * 60 * 60 * 1000,

  MAX_TURNS: 200,
  MAX_BATTLE_DAMAGE: 10_000_000,

  /** Matches the client's Raid screen (3 daily attempts). */
  RAID_DAILY_ATTEMPTS: 3,
  /** World Boss had no attempt cap in the offline prototype; the shared online boss needs one. */
  WORLD_BOSS_DAILY_ATTEMPTS: 5,
  /**
   * WorldBossScreen builds the in-battle boss with min(currentHp, 35_000) HP, so one battle cannot
   * legitimately deal much more than 35k. 2x leaves room for overkill on the final blow.
   */
  WORLD_BOSS_BATTLE_HP: 35_000,
  WORLD_BOSS_MAX_DAMAGE_PER_ATTEMPT: 70_000,
  /** Mirrors BattleViewModel/WorldBossScreen: a victory pays this, a participation pays tokens only. */
  WORLD_BOSS_VICTORY_GOLD: 5_000,
  WORLD_BOSS_VICTORY_XP: 3_000,
  WORLD_BOSS_PARTICIPATION_TOKENS: 25,

  ALLIANCE_NAME_MIN: 3,
  ALLIANCE_NAME_MAX: 24,
  ALLIANCE_DESCRIPTION_MAX: 120,

  MAX_CARD_ATTEST_QUANTITY: 99,

  LEADERBOARD_MAX_LIMIT: 100,
  OPEN_ALLIANCES_LIMIT: 25,
  HISTORY_LIMIT: 20,
  PENDING_REWARDS_LIMIT: 50,

  IDEMPOTENCY_TTL_MS: 7 * 24 * 60 * 60 * 1000,
} as const;

/** Per-uid fixed-window rate limits (requests per window). Enforced server-side in Firestore. */
export const RATE_LIMITS: Record<string, { limit: number; windowMs: number }> = {
  read: { limit: 30, windowMs: 60_000 },
  profile: { limit: 10, windowMs: 60_000 },
  deck: { limit: 20, windowMs: 60_000 },
  attest: { limit: 5, windowMs: 60 * 60_000 },
  start: { limit: 15, windowMs: 60_000 },
  submit: { limit: 15, windowMs: 60_000 },
  claim: { limit: 10, windowMs: 60_000 },
  alliance: { limit: 15, windowMs: 60_000 },
  ack: { limit: 20, windowMs: 60_000 },
};
