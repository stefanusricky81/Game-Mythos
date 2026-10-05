import { createHash } from "crypto";
import { GameDb, Tx } from "./db";
import { fail } from "./errors";
import { LIMITS, RATE_LIMITS } from "./limits";
import { ID_PATTERN, Payload } from "./schema";

export interface Caller {
  uid: string;
  now: number;
}

export const sha = (value: string, length = 32): string =>
  createHash("sha256").update(value).digest("hex").slice(0, length);

/** Authentication: the uid comes ONLY from the verified auth context, never from the payload. */
export const requireUid = (auth: { uid?: string } | null | undefined): string => {
  const uid = auth?.uid;
  if (!uid) return fail("unauthenticated", "Sign-in is required.");
  if (!ID_PATTERN.test(uid)) return fail("permission-denied", "Invalid player identity.");
  return uid;
};

/** A client may echo its own uid, but a different uid is an impersonation attempt. */
export const assertPayloadUid = (payload: Payload, uid: string): void => {
  if (payload.uid !== undefined && payload.uid !== uid) {
    fail("permission-denied", "Payload uid does not match the authenticated player.");
  }
};

/**
 * Server-side fixed-window rate limit stored in Firestore. It runs in its own transaction BEFORE the
 * action, so a rejected/failed action still counts (an in-action counter would be rolled back).
 */
export const rateLimit = async (db: GameDb, caller: Caller, bucket: string): Promise<void> => {
  const cfg = RATE_LIMITS[bucket];
  if (!cfg) return;
  const path = `rateLimits/${caller.uid}:${bucket}`;
  await db.runTransaction(async (tx) => {
    const doc = await tx.get(path);
    const windowStart = doc && caller.now - doc.windowStart < cfg.windowMs ? doc.windowStart : caller.now;
    const count = doc && windowStart === doc.windowStart ? doc.count : 0;
    if (count >= cfg.limit) {
      fail("resource-exhausted", "Too many requests. Please slow down.", { retryAfterMs: windowStart + cfg.windowMs - caller.now });
    }
    tx.set(path, { uid: caller.uid, bucket, windowStart, count: count + 1, expiresAt: windowStart + cfg.windowMs * 2 });
  });
};

/**
 * Exactly-once execution per (uid, action, requestId). A retry with the same requestId returns the
 * stored response (flagged idempotentReplay) instead of re-running the action, so a lost response or a
 * double tap can never double-grant. Failed actions store nothing and may be retried.
 */
export const idempotent = async <T extends Record<string, unknown>>(
  tx: Tx,
  caller: Caller,
  action: string,
  requestId: string,
  compute: () => Promise<T>
): Promise<T & { idempotentReplay?: boolean }> => {
  const path = `idempotency/${sha(`${caller.uid}|${action}|${requestId}`)}`;
  const existing = await tx.get(path);
  if (existing) return { ...(existing.response as T), idempotentReplay: true };
  const response = await compute();
  tx.set(path, {
    uid: caller.uid,
    action,
    response,
    createdAt: caller.now,
    expiresAt: caller.now + LIMITS.IDEMPOTENCY_TTL_MS,
  });
  return response;
};
