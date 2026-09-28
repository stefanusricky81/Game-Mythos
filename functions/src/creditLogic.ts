/**
 * Pure decision logic for crediting a Google Play purchase, extracted from playBilling.ts so
 * it can be unit tested without Firestore/network. Firestore's transaction semantics
 * (optimistic concurrency: a transaction is retried if the documents it read changed before
 * it commits) are what make the *caller* of decideCredit race-safe when both the read and the
 * write happen inside the same db.runTransaction() call - this function only encodes what to
 * DO with whatever consistent snapshot the transaction handed it.
 */

export type ExistingPurchaseRecord = {
  playerUid: string;
};

export type CreditDecision =
  | { kind: "CREDIT" }
  | { kind: "ALREADY_CREDITED" }
  | { kind: "CONFLICT"; ownerUid: string };

/**
 * existing: the current Firestore doc for this purchaseTokenHash, or null if none exists yet -
 * both read inside the SAME transaction that will perform the write, so this is safe from the
 * classic check-then-write race as long as the caller passes a doc read from that transaction.
 */
export function decideCredit(
  existing: ExistingPurchaseRecord | null,
  requestUid: string
): CreditDecision {
  if (!existing) {
    return { kind: "CREDIT" };
  }
  if (existing.playerUid === requestUid) {
    return { kind: "ALREADY_CREDITED" };
  }
  return { kind: "CONFLICT", ownerUid: existing.playerUid };
}

/** Google's purchases.products.get purchaseState: 0 = purchased, 1 = canceled, 2 = pending. */
export type PurchaseStateCheck = "PROCEED" | "PENDING" | "CANCELLED" | "INVALID";

export function checkPurchaseState(purchaseState: number | null | undefined): PurchaseStateCheck {
  if (purchaseState === 0) return "PROCEED";
  if (purchaseState === 2) return "PENDING";
  if (purchaseState === 1) return "CANCELLED";
  return "INVALID";
}

/** The exact status vocabulary returned to the Android client - never a bare boolean. */
export type VerifyStatus =
  | "CREDITED_NOW"
  | "ALREADY_CREDITED"
  | "PENDING"
  | "CANCELLED"
  | "INVALID"
  | "ERROR";
