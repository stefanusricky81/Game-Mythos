import raw from "./gameCatalog.json";

/**
 * Typed view over gameCatalog.json, which is generated from the Kotlin catalogs by
 * GameCatalogParityTest. The server never accepts card IDs, rewards, ratings or boss definitions
 * from a client - it only ever looks them up here.
 */
export interface Rewards { gold: number; cardShards: number; arenaPoints: number; xp: number }
export interface Opponent {
  id: string; name: string; heroId: string; heroName: string; archetype: string;
  title: string; rating: number; deck: string[];
}
export interface Tier { name: string; min: number; max: number }
export interface SeasonReward {
  gold: number; cardShards: number; heroShards: number;
  frameId: string | null; cosmeticId: string | null; title: string | null;
}
export interface Difficulty { id: string; hpMultiplier: number; rewardMultiplier: number }
export interface WorldBoss { bossId: string; maxHp: number; start: string; end: string }
export interface WorldBossTier {
  id: string; minDamage: number; gold: number; eventTokens: number; heroShards: number; cardShards: number;
}
export interface RaidStage { stage: number; hp: number; gold: number; eventTokens: number; xp: number; boss: boolean }
export interface Raid { raidId: string; defaultDifficulty: string; stages: RaidStage[] }
export interface GameEvent { eventId: string; type: string; start: string; end: string; active: boolean }
export interface ShopItem {
  itemId: string; category: string; tokenPrice: number; limit: number; gold: number;
  heroShards: number; heroId: string; cardShards: number; cardId: string; cosmeticId: string;
}

export const CATALOG = raw as unknown as {
  version: number;
  cards: string[];
  heroes: string[];
  deck: { size: number; maxCopies: number };
  starter: { heroId: string; deck: string[]; ownedCardCounts: Record<string, number> };
  arena: {
    maxDailyAttempts: number; seasonDays: number; startingRating: number; minRating: number;
    kFactor: number; searchRange: number;
    rewards: {
      firstWin: Rewards; normalWin: Rewards; defeat: Rewards;
      streak: Record<string, { gold: number; arenaPoints: number }>;
    };
    tiers: Tier[];
    seasonRewards: Record<string, SeasonReward>;
    opponents: Opponent[];
  };
  difficulties: Difficulty[];
  worldBosses: WorldBoss[];
  worldBossTiers: WorldBossTier[];
  raids: Raid[];
  events: GameEvent[];
  eventShop: ShopItem[];
  alliance: { maxMembers: number; maxLevel: number; xpTable: number[]; emblems: string[] };
};

export const CARD_IDS: ReadonlySet<string> = new Set(CATALOG.cards);
export const HERO_IDS: ReadonlySet<string> = new Set(CATALOG.heroes);

export const findBoss = (id: string): WorldBoss | undefined => CATALOG.worldBosses.find((b) => b.bossId === id);
export const findRaid = (id: string): Raid | undefined => CATALOG.raids.find((r) => r.raidId === id);
export const findEvent = (id: string): GameEvent | undefined => CATALOG.events.find((e) => e.eventId === id);
export const findShopItem = (id: string): ShopItem | undefined => CATALOG.eventShop.find((i) => i.itemId === id);
export const findDifficulty = (id: string): Difficulty | undefined => CATALOG.difficulties.find((d) => d.id === id);

export const tierOf = (rating: number): Tier => {
  const r = Math.max(0, rating);
  return CATALOG.arena.tiers.find((t) => r >= t.min && r <= t.max) ?? CATALOG.arena.tiers[CATALOG.arena.tiers.length - 1];
};

/** Port of ArenaRatingCalculator (Kotlin). Kotlin's roundToInt and Math.round both round ties up. */
export const eloDelta = (playerRating: number, opponentRating: number, outcome: Outcome): number => {
  const expected = 1 / (1 + Math.pow(10, (opponentRating - playerRating) / 400));
  const actual = outcome === "VICTORY" ? 1 : outcome === "DRAW" ? 0.5 : 0;
  const raw = Math.round(CATALOG.arena.kFactor * (actual - expected));
  if (outcome === "VICTORY") return Math.max(1, raw);
  if (outcome === "DEFEAT") return Math.min(-1, raw);
  return raw;
};

export const applyRating = (rating: number, delta: number): number => Math.max(CATALOG.arena.minRating, rating + delta);

export const seasonResetRating = (rating: number): number =>
  rating > CATALOG.arena.startingRating
    ? CATALOG.arena.startingRating + Math.trunc((rating - CATALOG.arena.startingRating) / 2)
    : CATALOG.arena.startingRating;

export type Outcome = "VICTORY" | "DEFEAT" | "DRAW";
export const OUTCOMES: readonly Outcome[] = ["VICTORY", "DEFEAT", "DRAW"];

/** Port of ArenaCatalog.findOpponent: deterministic, chosen by the server, never by the client. */
export const findOpponent = (playerRating: number): Opponent & { rating: number; combatPower: number } => {
  const { opponents, searchRange, minRating } = CATALOG.arena;
  const closest = (list: Opponent[]) =>
    list.reduce((best, o) => (Math.abs(o.rating - playerRating) < Math.abs(best.rating - playerRating) ? o : best));
  const eligible = opponents.filter((o) => o.rating >= playerRating - searchRange && o.rating <= playerRating + searchRange);
  const target = closest(eligible.length > 0 ? eligible : opponents);

  const offset = playerRating < 800 ? -30 : playerRating > 2000 ? 25 : playerRating % 2 === 0 ? 15 : -15;
  const rating = Math.max(minRating, playerRating + offset);
  return { ...target, rating, combatPower: Math.trunc(rating * 8.5) };
};
