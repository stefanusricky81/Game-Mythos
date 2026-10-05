import { allianceAction, syncAlliance } from "./alliance";
import { getArenaState, getLeaderboard, claimArenaReward, startArenaMatch, submitArenaResult } from "./arena";
import { GameDb } from "./db";
import {
  ackRewards, claimEventReward, claimWorldBossReward, getEndgameState, getPendingRewards,
  startEndgameAttempt, submitRaidContribution, submitWorldBossContribution,
} from "./endgame";
import { fail } from "./errors";
import { attestCollection, ensurePlayerProfile, validateDeck } from "./player";
import { Payload } from "./schema";
import { Caller, assertPayloadUid, rateLimit, requireUid } from "./security";

export interface Env {
  db: GameDb;
  /** See attestCollection(): disable once summons/purchases are granted server-side. */
  collectionAttestationAllowed: boolean;
}

type Run = (env: Env, caller: Caller, data: unknown) => Promise<unknown>;

/**
 * The complete server API surface. Every entry goes through the same pipeline in handleCall:
 * authenticated uid -> payload-uid check -> server-side rate limit -> strict schema validation ->
 * server-state validation inside a Firestore transaction (idempotent where it mutates).
 */
export const HANDLERS: Record<string, { bucket: string; run: Run }> = {
  ensurePlayerProfile: { bucket: "profile", run: (e, c, d) => e.db.runTransaction((tx) => ensurePlayerProfile(tx, c, d)) },
  validateDeck: { bucket: "deck", run: (e, c, d) => e.db.runTransaction((tx) => validateDeck(tx, c, d)) },
  syncCollection: { bucket: "attest", run: (e, c, d) => e.db.runTransaction((tx) => attestCollection(tx, c, d, e.collectionAttestationAllowed)) },

  startArenaMatch: { bucket: "start", run: (e, c, d) => startArenaMatch(e.db, c, d) },
  submitArenaResult: { bucket: "submit", run: (e, c, d) => submitArenaResult(e.db, c, d) },
  claimArenaReward: { bucket: "claim", run: (e, c, d) => claimArenaReward(e.db, c, d) },
  getArenaState: { bucket: "read", run: (e, c, d) => getArenaState(e.db, c, d) },
  getLeaderboard: { bucket: "read", run: (e, c, d) => getLeaderboard(e.db, c, d) },

  allianceAction: { bucket: "alliance", run: (e, c, d) => allianceAction(e.db, c, d) },
  syncAlliance: { bucket: "read", run: (e, c, d) => syncAlliance(e.db, c, d) },

  startEndgameAttempt: { bucket: "start", run: (e, c, d) => startEndgameAttempt(e.db, c, d) },
  submitRaidContribution: { bucket: "submit", run: (e, c, d) => submitRaidContribution(e.db, c, d) },
  submitWorldBossContribution: { bucket: "submit", run: (e, c, d) => submitWorldBossContribution(e.db, c, d) },
  claimWorldBossReward: { bucket: "claim", run: (e, c, d) => claimWorldBossReward(e.db, c, d) },
  claimEventReward: { bucket: "claim", run: (e, c, d) => claimEventReward(e.db, c, d) },
  getEndgameState: { bucket: "read", run: (e, c, d) => getEndgameState(e.db, c, d) },

  getPendingRewards: { bucket: "read", run: (e, c, d) => getPendingRewards(e.db, c, d) },
  ackRewards: { bucket: "ack", run: (e, c, d) => ackRewards(e.db, c, d) },
};

export const handleCall = async (
  env: Env,
  name: string,
  auth: { uid?: string } | null | undefined,
  data: unknown,
  now: number
): Promise<unknown> => {
  const handler = HANDLERS[name];
  if (!handler) return fail("not-found", "Unknown function.");
  const uid = requireUid(auth);
  if (data && typeof data === "object" && !Array.isArray(data)) assertPayloadUid(data as Payload, uid);
  const caller: Caller = { uid, now };
  await rateLimit(env.db, caller, handler.bucket);
  return handler.run(env, caller, data);
};
