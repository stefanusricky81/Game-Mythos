import { HttpsError, onCall } from "firebase-functions/v2/https";
import * as logger from "firebase-functions/logger";
import { FirestoreGameDb } from "./db";
import { GameError } from "./errors";
import { Env, HANDLERS, handleCall } from "./handlers";

/**
 * Phase 12 gameplay backend. Same region as the payment functions (asia-southeast1).
 *
 * App Check: enforced only when ENFORCE_APP_CHECK=true is set in the functions environment
 * (functions/.env.<project>), so a project/app that is not registered for App Check yet does not get
 * locked out, and local/debug development works without opening a production endpoint. Production
 * setup that is still required before turning it on is documented in PHASE12_BACKEND.md.
 */
const REGION = "asia-southeast1";
const enforceAppCheck = process.env.ENFORCE_APP_CHECK === "true";

let env: Env | undefined;
const getEnv = (): Env => {
  if (!env) {
    env = {
      db: new FirestoreGameDb(),
      collectionAttestationAllowed: process.env.ALLOW_COLLECTION_ATTESTATION !== "false",
    };
  }
  return env;
};

const toHttpsError = (name: string, e: unknown): HttpsError => {
  if (e instanceof GameError) return new HttpsError(e.code, e.message, e.details);
  logger.error(`${name} failed`, { error: e instanceof Error ? e.message : String(e) });
  return new HttpsError("internal", "Unexpected server error.");
};

const make = (name: keyof typeof HANDLERS) =>
  onCall({ region: REGION, enforceAppCheck, maxInstances: 20 }, async (request) => {
    try {
      return await handleCall(getEnv(), name, request.auth, request.data, Date.now());
    } catch (e) {
      throw toHttpsError(name, e);
    }
  });

export const ensurePlayerProfile = make("ensurePlayerProfile");
export const validateDeck = make("validateDeck");
export const syncCollection = make("syncCollection");
export const startArenaMatch = make("startArenaMatch");
export const submitArenaResult = make("submitArenaResult");
export const claimArenaReward = make("claimArenaReward");
export const getArenaState = make("getArenaState");
export const getLeaderboard = make("getLeaderboard");
export const allianceAction = make("allianceAction");
export const syncAlliance = make("syncAlliance");
export const startEndgameAttempt = make("startEndgameAttempt");
export const submitRaidContribution = make("submitRaidContribution");
export const submitWorldBossContribution = make("submitWorldBossContribution");
export const claimWorldBossReward = make("claimWorldBossReward");
export const claimEventReward = make("claimEventReward");
export const getEndgameState = make("getEndgameState");
export const getPendingRewards = make("getPendingRewards");
export const ackRewards = make("ackRewards");
