import { CARD_IDS, CATALOG, HERO_IDS } from "./catalog";
import { Tx } from "./db";
import { fail } from "./errors";
import { LIMITS } from "./limits";
import { P } from "./paths";
import { asPayload, Payload, int, requestId, strArray, reqStr, ID_PATTERN } from "./schema";
import { Caller, idempotent } from "./security";

export interface Profile {
  uid: string;
  displayName: string;
  createdAt: number;
  updatedAt: number;
  level: number;
  xp: number;
  lifetime: { battles: number; wins: number; losses: number; damage: number };
  arenaPoints: number;
  cardShards: number;
  eventTokens: number;
  frames: string[];
  cosmetics: string[];
  titles: string[];
}

export const levelFromXp = (xp: number): number => Math.min(100, 1 + Math.floor(xp / 1000));

const newProfile = (uid: string, now: number): Profile => ({
  uid,
  displayName: `Hero_${uid.slice(0, 5)}`,
  createdAt: now,
  updatedAt: now,
  level: 1,
  xp: 0,
  lifetime: { battles: 0, wins: 0, losses: 0, damage: 0 },
  arenaPoints: 0,
  cardShards: 0,
  eventTokens: 0,
  frames: [],
  cosmetics: [],
  titles: [],
});

export interface PlayerCtx {
  profile: Profile;
  isNew: boolean;
}

/** Loads the player's server profile, provisioning it on first contact (the uid is auth-derived). */
export const loadPlayer = async (tx: Tx, uid: string, now: number): Promise<PlayerCtx> => {
  const existing = await tx.get(P.profile(uid));
  if (existing) return { profile: existing as Profile, isNew: false };
  return { profile: newProfile(uid, now), isNew: true };
};

export const savePlayer = (tx: Tx, ctx: PlayerCtx, now: number): void => {
  ctx.profile.updatedAt = now;
  tx.set(P.profile(ctx.profile.uid), ctx.profile as unknown as Record<string, unknown>);
  if (ctx.isNew) {
    tx.set(P.collection(ctx.profile.uid), {
      counts: { ...CATALOG.starter.ownedCardCounts },
      attestedAt: null,
      updatedAt: now,
    });
  }
};

/** Card ownership registry. New players start with the catalog's starter collection. */
export const loadCollection = async (tx: Tx, uid: string): Promise<Record<string, number>> => {
  const doc = await tx.get(P.collection(uid));
  return doc ? { ...(doc.counts as Record<string, number>) } : { ...CATALOG.starter.ownedCardCounts };
};

// ---------------------------------------------------------------------------------------------
// Grants. A grant is always computed by the server from catalog rules; the client only ever
// receives the result. Gold / XP / hero shards are applied by the game client from the reward
// outbox (they live in the local economy); everything else is held authoritatively on the server.
// ---------------------------------------------------------------------------------------------
export interface Grant {
  gold: number;
  xp: number;
  heroShards: Record<string, number>;
  cardShards: number;
  arenaPoints: number;
  eventTokens: number;
  frames: string[];
  cosmetics: string[];
  titles: string[];
  cards: Record<string, number>;
}

export const emptyGrant = (): Grant => ({
  gold: 0, xp: 0, heroShards: {}, cardShards: 0, arenaPoints: 0, eventTokens: 0,
  frames: [], cosmetics: [], titles: [], cards: {},
});

const addUnique = (list: string[], values: string[]): string[] => [...new Set([...list, ...values])];

/** Applies the server-held parts of a grant. Returns the card grants for the caller to persist. */
export const applyGrant = (profile: Profile, grant: Grant): void => {
  profile.xp += grant.xp;
  profile.level = levelFromXp(profile.xp);
  profile.cardShards += grant.cardShards;
  profile.arenaPoints += grant.arenaPoints;
  profile.eventTokens += grant.eventTokens;
  profile.frames = addUnique(profile.frames, grant.frames);
  profile.cosmetics = addUnique(profile.cosmetics, grant.cosmetics);
  profile.titles = addUnique(profile.titles, grant.titles);
};

export const grantCards = (counts: Record<string, number>, cards: Record<string, number>): Record<string, number> => {
  const next = { ...counts };
  for (const [cardId, qty] of Object.entries(cards)) next[cardId] = (next[cardId] ?? 0) + qty;
  return next;
};

export const hasClientApplicablePart = (g: Grant): boolean =>
  g.gold > 0 || g.xp > 0 || Object.values(g.heroShards).some((v) => v > 0);

/**
 * Reward outbox entry. The deterministic claimId makes granting naturally idempotent, and the
 * PENDING -> APPLIED handshake lets the client apply a reward exactly once even across crashes
 * and reconnects (see GameRewardSync on the client).
 */
export const rewardDoc = (claimId: string, source: string, sourceId: string, grant: Grant, now: number) => ({
  claimId,
  source,
  sourceId,
  gold: grant.gold,
  xp: grant.xp,
  heroShards: grant.heroShards,
  summary: {
    cardShards: grant.cardShards,
    arenaPoints: grant.arenaPoints,
    eventTokens: grant.eventTokens,
    frames: grant.frames,
    cosmetics: grant.cosmetics,
    titles: grant.titles,
    cards: grant.cards,
  },
  status: hasClientApplicablePart(grant) ? "PENDING" : "APPLIED",
  createdAt: now,
  appliedAt: hasClientApplicablePart(grant) ? null : now,
});

// ---------------------------------------------------------------------------------------------
// Deck validation (authoritative): exactly 20 cards, max 2 copies, valid IDs, owned quantities.
// ---------------------------------------------------------------------------------------------
export interface DeckIssue { type: string; message: string; cardId?: string }

export const checkDeck = (heroId: string, cardIds: string[], owned: Record<string, number>): DeckIssue[] => {
  const issues: DeckIssue[] = [];
  const { size, maxCopies } = CATALOG.deck;

  if (cardIds.length !== size) {
    issues.push({ type: "INVALID_DECK_SIZE", message: `Deck must contain exactly ${size} cards, but contained ${cardIds.length}.` });
  }
  if (!HERO_IDS.has(heroId)) issues.push({ type: "UNKNOWN_HERO_ID", message: `Unknown hero ID: '${heroId}'.` });

  const required: Record<string, number> = {};
  for (const cardId of cardIds) {
    if (!CARD_IDS.has(cardId)) {
      issues.push({ type: "UNKNOWN_CARD_ID", message: `Card ID '${cardId}' does not exist in the official catalog.`, cardId });
      continue;
    }
    required[cardId] = (required[cardId] ?? 0) + 1;
  }
  for (const [cardId, count] of Object.entries(required)) {
    if (count > maxCopies) {
      issues.push({ type: "EXCEEDED_DUPLICATE_LIMIT", message: `Card '${cardId}' exceeds maximum limit of ${maxCopies} copies (found ${count}).`, cardId });
    }
    const have = owned[cardId] ?? 0;
    if (have < count) {
      issues.push({ type: "UNOWNED_CARD", message: `Player owns ${have} copies of card '${cardId}', but deck requires ${count}.`, cardId });
    }
  }
  return issues;
};

export const parseDeck = (payload: Payload): { heroId: string; cardIds: string[] } => ({
  heroId: reqStr(payload, "heroId", { max: 64, pattern: ID_PATTERN }),
  // Bounded so a malicious client cannot make the server validate a huge array.
  cardIds: strArray(payload, "cardIds", { min: 0, max: 60 }),
});

// ---------------------------------------------------------------------------------------------
// Handlers
// ---------------------------------------------------------------------------------------------
export const profileView = (p: Profile) => ({
  uid: p.uid,
  displayName: p.displayName,
  level: p.level,
  xp: p.xp,
  lifetime: p.lifetime,
  arenaPoints: p.arenaPoints,
  cardShards: p.cardShards,
  eventTokens: p.eventTokens,
  frames: p.frames,
  cosmetics: p.cosmetics,
  titles: p.titles,
});

export const ensurePlayerProfile = async (tx: Tx, caller: Caller, data: unknown) => {
  asPayload(data, []);
  const ctx = await loadPlayer(tx, caller.uid, caller.now);
  if (ctx.isNew) savePlayer(tx, ctx, caller.now);
  return { created: ctx.isNew, profile: profileView(ctx.profile) };
};

export const validateDeck = async (tx: Tx, caller: Caller, data: unknown) => {
  const payload = asPayload(data, ["heroId", "cardIds"]);
  const { heroId, cardIds } = parseDeck(payload);
  const owned = await loadCollection(tx, caller.uid);
  const issues = checkDeck(heroId, cardIds, owned);
  return { valid: issues.length === 0, issues, size: cardIds.length };
};

/**
 * Ownership today is only partly server-known: the starter set plus anything the server granted.
 * Cards the player obtained through the offline summon/shop (which still live in the client economy)
 * reach the server through this attestation. It is bounded (catalog IDs only, never lowers a count)
 * and can be switched off with ALLOW_COLLECTION_ATTESTATION=false, but it is CLIENT-ATTESTED and is
 * the main remaining trust gap until summons and purchases are granted server-side.
 */
export const attestCollection = async (tx: Tx, caller: Caller, data: unknown, attestationAllowed: boolean) => {
  const payload = asPayload(data, ["requestId", "ownedCardCounts"]);
  const reqId = requestId(payload);
  if (!attestationAllowed) fail("failed-precondition", "Collection attestation is disabled on this server.");

  const raw = payload.ownedCardCounts;
  if (typeof raw !== "object" || raw === null || Array.isArray(raw)) fail("invalid-argument", "ownedCardCounts must be an object.");
  const entries = Object.entries(raw as Record<string, unknown>);
  if (entries.length > CATALOG.cards.length) fail("invalid-argument", "ownedCardCounts has too many entries.");
  for (const [cardId, qty] of entries) {
    if (!CARD_IDS.has(cardId)) fail("invalid-argument", `Unknown card ID '${cardId}'.`, { cardId });
    int({ qty }, "qty", { min: 0, max: LIMITS.MAX_CARD_ATTEST_QUANTITY });
  }

  return idempotent(tx, caller, "attestCollection", reqId, async () => {
    const ctx = await loadPlayer(tx, caller.uid, caller.now);
    const counts = await loadCollection(tx, caller.uid);
    for (const [cardId, qty] of entries) counts[cardId] = Math.max(counts[cardId] ?? 0, qty as number);
    savePlayer(tx, ctx, caller.now);
    tx.set(P.collection(caller.uid), { counts, attestedAt: caller.now, updatedAt: caller.now });
    return { distinctCards: Object.keys(counts).filter((c) => counts[c] > 0).length, attested: true };
  });
};
