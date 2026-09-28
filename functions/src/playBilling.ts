import { onCall, HttpsError, CallableRequest } from "firebase-functions/v2/https";
import * as logger from "firebase-functions/logger";
import * as admin from "firebase-admin";
import * as crypto from "crypto";
import { google } from "googleapis";
import { checkPurchaseState, decideCredit, VerifyStatus } from "./creditLogic";

const REGION = "asia-southeast1";

// !! MUST match the applicationId actually published on Play Console for this app. !!
// As of this writing app/build.gradle.kts has applicationId "com.aistudio.mythosbattle.krxwtp"
// (an AI-Studio-generated id) - confirm it's identical to what's live before deploying,
// otherwise every purchases.products.get() call below will 404.
const PACKAGE_NAME = "com.aistudio.mythosbattle.krxwtp";

// Keyless auth to the Play Developer API: the function runs AS this dedicated service
// account (no JSON key file exists anywhere to leak or lose), and that same account is
// invited in Play Console > Users and permissions with "View financial data" for Mythos.
// It also needs the "Cloud Datastore User" role for the Firestore ledger. Google Play
// Android Developer API must be enabled in the Google Cloud project.
const RUNTIME_SERVICE_ACCOUNT = "mythos-play-verifier@mythos-game-a8b8c.iam.gserviceaccount.com";

const PURCHASES_COLLECTION = "purchases";

function hashToken(token: string): string {
  return crypto.createHash("sha256").update(token).digest("hex");
}

interface VerifyPurchaseResponse {
  status: VerifyStatus;
  productId?: string;
  orderId?: string | null;
}

/**
 * Verifies a Google Play purchase token before the client is allowed to grant its reward, and
 * is the single place a purchaseToken is ever turned into a credited entitlement.
 *
 * Identity: requires Firebase Auth (Anonymous Auth is enough - see PlayerIdentity.kt on the
 * client). MYTHOS has no email/password accounts, but a stable anonymous UID is what lets this
 * function answer "has THIS player already been credited for this token" instead of just
 * "has this token been used by ANYONE", which is what made the previous version of this
 * function unsafe to treat as a real idempotency guard from the client's perspective.
 *
 * Idempotency: purchaseToken (hashed, never stored raw) is the ONLY idempotency key - never
 * orderId, which Google does not guarantee stays stable/unique the way the token does for this
 * purpose. The read-decide-write for "has this token been credited, to whom" happens inside a
 * single Firestore transaction (see decideCredit in creditLogic.ts), so two verify calls for
 * the same token arriving concurrently cannot both create a credit record: Firestore retries a
 * transaction whose read set changed before it committed, so the second one to actually commit
 * always observes the first one's write and resolves to ALREADY_CREDITED or CONFLICT, never a
 * second CREDIT.
 *
 * Ownership: if the same token is ever presented under a DIFFERENT uid than whoever was first
 * credited for it, this is treated as a conflict - never silently re-assigned - and logged as a
 * security event. This is the resolution to "purchase token dapat terlihat di device lain
 * dengan akun Google Play yang sama": at most ONE Firebase identity ever gets credited for a
 * given token, whichever calls this function first.
 */
export const verifyPlayPurchase = onCall(
  {
    region: REGION,
    serviceAccount: RUNTIME_SERVICE_ACCOUNT,
    enforceAppCheck: true,
  },
  async (request: CallableRequest<{ productId: string; purchaseToken: string }>): Promise<VerifyPurchaseResponse> => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Sign-in (Anonymous Auth is sufficient) is required.");
    }
    const uid = request.auth.uid;

    const { productId, purchaseToken } = request.data ?? {};
    if (!productId || !purchaseToken) {
      throw new HttpsError("invalid-argument", "productId and purchaseToken are required.");
    }

    const tokenHash = hashToken(purchaseToken);
    const tokenHashPrefix = tokenHash.slice(0, 8);

    // ─── 1. Verify for real against Google Play (never trust the client) ──────────────
    let purchaseState: number | null | undefined;
    let googleOrderId: string | null | undefined;
    try {
      // Application Default Credentials = the runtime service account above (no key file).
      const auth = new google.auth.GoogleAuth({
        scopes: ["https://www.googleapis.com/auth/androidpublisher"],
      });
      const androidpublisher = google.androidpublisher({ version: "v3", auth });
      const res = await androidpublisher.purchases.products.get({
        packageName: PACKAGE_NAME,
        productId,
        token: purchaseToken,
      });
      purchaseState = res.data.purchaseState;
      googleOrderId = res.data.orderId ?? null;
    } catch (e: any) {
      // Covers wrong productId, wrong packageName, malformed/unknown token, and transient
      // Google API failures alike - Google's own API is what rejects a productId/token pair
      // that doesn't correspond to a real purchase of THIS app, so no separate allowlist of
      // valid product IDs is needed here.
      logger.error("verifyPlayPurchase: Google Play verification request failed", {
        productId,
        tokenHashPrefix,
        uid,
        error: e?.message,
      });
      return { status: "ERROR" };
    }

    const stateCheck = checkPurchaseState(purchaseState);
    if (stateCheck === "PENDING") return { status: "PENDING", productId };
    if (stateCheck === "CANCELLED") return { status: "CANCELLED", productId };
    if (stateCheck === "INVALID") return { status: "INVALID", productId };

    // ─── 2. Atomic read-decide-write: exactly one CREDIT per token, ever ───────────────
    const guardRef = admin.firestore().collection(PURCHASES_COLLECTION).doc(tokenHash);
    const decision = await admin.firestore().runTransaction(async (tx) => {
      const snap = await tx.get(guardRef);
      const existing = snap.exists ? { playerUid: snap.data()?.playerUid as string } : null;
      const result = decideCredit(existing, uid);

      if (result.kind === "CREDIT") {
        const now = admin.firestore.FieldValue.serverTimestamp();
        tx.set(guardRef, {
          playerUid: uid,
          purchaseTokenHash: tokenHash,
          productId,
          packageName: PACKAGE_NAME,
          orderId: googleOrderId,
          purchaseState,
          // Consumption happens client-side via BillingClient.consumePurchase(), which this
          // function has no visibility into - `acknowledged` here just marks "credited by us",
          // not "confirmed consumed on Play's side". See SETUP_PLAY_BILLING.md for the
          // known limitation this implies for restore/recovery.
          acknowledged: false,
          creditedAt: now,
          createdAt: now,
          updatedAt: now,
        });
      }
      return result;
    });

    if (decision.kind === "CREDIT") {
      return { status: "CREDITED_NOW", productId, orderId: googleOrderId };
    }
    if (decision.kind === "ALREADY_CREDITED") {
      return { status: "ALREADY_CREDITED", productId, orderId: googleOrderId };
    }

    // CONFLICT: this exact token was already credited to a DIFFERENT Firebase uid. Never
    // silently move it - log it as a security event and refuse.
    logger.warn("verifyPlayPurchase: SECURITY ownership conflict on purchase token", {
      productId,
      tokenHashPrefix,
      requestUid: uid,
      creditedToUid: decision.ownerUid,
    });
    return { status: "INVALID", productId };
  }
);

/**
 * Lets a signed-in device recover entitlements it paid for if its own local economy
 * (SharedPreferences) was lost while the SAME Firebase Anonymous Auth identity survived -
 * e.g. the app's own storage got corrupted/reset but the OS-level Firebase Auth credential
 * did not. This does NOT help after a full uninstall or "clear all app data", since Anonymous
 * Auth's credential lives in the same app-private storage and is wiped along with everything
 * else - see SETUP_PLAY_BILLING.md for why reinstall recovery needs a durable identity
 * (e.g. Play Games Sign-In) as a separate, later step.
 */
export const getPlayerEntitlements = onCall(
  { region: REGION, enforceAppCheck: true },
  async (request: CallableRequest) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Sign-in (Anonymous Auth is sufficient) is required.");
    }
    const uid = request.auth.uid;

    const snap = await admin
      .firestore()
      .collection(PURCHASES_COLLECTION)
      .where("playerUid", "==", uid)
      .get();

    const entitlements = snap.docs.map((doc) => {
      const data = doc.data();
      return {
        productId: data.productId as string,
        orderId: (data.orderId as string | null) ?? null,
        // Safe to expose - a one-way hash, never the purchaseToken itself - and unlike
        // orderId (not always guaranteed present) this is always available as a stable
        // per-purchase key for the client's own local idempotency ledger when reconciling.
        purchaseTokenHash: doc.id,
      };
    });

    return { entitlements };
  }
);
