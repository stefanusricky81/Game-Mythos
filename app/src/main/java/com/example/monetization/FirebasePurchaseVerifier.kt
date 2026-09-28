package com.example.monetization

import com.google.firebase.Firebase
import com.google.firebase.functions.functions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Mirrors the exact status vocabulary verifyPlayPurchase returns (see
 * functions/src/creditLogic.ts VerifyStatus) - never a bare boolean, so the caller can tell
 * "this is a genuinely new, first-time credit" apart from "this token was already credited"
 * apart from "this failed and should not be treated as verified at all".
 */
sealed class VerificationOutcome {
    /** First time this purchaseToken has ever been credited, under this player's UID. */
    data class CreditedNow(val productId: String, val orderId: String?) : VerificationOutcome()

    /**
     * This exact token was already credited before (to THIS same player's UID - a different
     * uid owning it comes back as [Invalid], never as this). Still safe to proceed to grant:
     * PlayerEconomyRepository.grantEntitlements()'s own persisted purchaseId ledger is what
     * actually decides whether local state gets mutated, so this only matters for letting a
     * reconciliation retry (e.g. after consume() failed last time) reach the same outcome
     * without erroring - it does NOT by itself cause a second local grant.
     */
    data class AlreadyCredited(val productId: String, val orderId: String?) : VerificationOutcome()

    /** Purchase not settled yet (e.g. a pending cash/QRIS payment). Never grant on this. */
    object Pending : VerificationOutcome()

    /** Purchase was canceled/refunded. Never grant on this. */
    object Cancelled : VerificationOutcome()

    /**
     * Either Google rejected the productId/token/package combination outright, or this exact
     * token is already owned by a DIFFERENT player UID (an ownership conflict the server
     * refused to silently resolve). Never grant on this.
     */
    object Invalid : VerificationOutcome()

    /** Network/server failure - inconclusive, not a confirmed rejection. Never grant on this. */
    data class Error(val message: String) : VerificationOutcome()
}

/**
 * Calls the `verifyPlayPurchase` Cloud Function so a purchase is only granted after Google
 * itself confirms - via the Play Developer API on the server - that the purchase token is
 * real, matches this app's package name, and is actually in the PURCHASED state. Protected by
 * both Firebase App Check (proves the caller is a genuine Mythos install) and Firebase
 * Anonymous Auth via [PlayerIdentity] (gives the server a stable player UID to credit against -
 * MYTHOS still has no email/password accounts, this is identity, not login).
 */
class FirebasePurchaseVerifier(
    private val functionsRegion: String = "asia-southeast1"
) {
    private val functions by lazy { Firebase.functions(functionsRegion) }

    suspend fun verify(productId: String, purchaseToken: String): VerificationOutcome {
        return try {
            PlayerIdentity.ensureSignedIn()
            val payload = mapOf("productId" to productId, "purchaseToken" to purchaseToken)
            val data = callFunction("verifyPlayPurchase", payload)
            val map = data as? Map<*, *>
                ?: return VerificationOutcome.Error("Malformed response from verification server.")

            val status = map["status"] as? String
            val responseProductId = map["productId"] as? String ?: productId
            val orderId = map["orderId"] as? String
            when (status) {
                "CREDITED_NOW" -> VerificationOutcome.CreditedNow(responseProductId, orderId)
                "ALREADY_CREDITED" -> VerificationOutcome.AlreadyCredited(responseProductId, orderId)
                "PENDING" -> VerificationOutcome.Pending
                "CANCELLED" -> VerificationOutcome.Cancelled
                "INVALID" -> VerificationOutcome.Invalid
                else -> VerificationOutcome.Error("Unrecognized server status: $status")
            }
        } catch (e: Exception) {
            VerificationOutcome.Error(e.message ?: "Server verification failed.")
        }
    }

    data class EntitlementRecord(val productId: String, val orderId: String?, val purchaseTokenHash: String)

    /**
     * Fetches this player's payment-derived entitlements from the server, for reconciling local
     * state that may have been lost (e.g. SharedPreferences corruption) while the SAME Firebase
     * Anonymous Auth identity survived. Does NOT help after a full uninstall/clear-data - see
     * [PlayerIdentity]'s known limitation.
     */
    suspend fun fetchEntitlements(): List<EntitlementRecord> {
        return try {
            PlayerIdentity.ensureSignedIn()
            val data = callFunction("getPlayerEntitlements", emptyMap<String, Any>())
            val map = data as? Map<*, *> ?: return emptyList()
            val list = map["entitlements"] as? List<*> ?: return emptyList()
            list.mapNotNull { entry ->
                val entryMap = entry as? Map<*, *> ?: return@mapNotNull null
                val productId = entryMap["productId"] as? String ?: return@mapNotNull null
                val hash = entryMap["purchaseTokenHash"] as? String ?: return@mapNotNull null
                EntitlementRecord(productId, entryMap["orderId"] as? String, hash)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun callFunction(name: String, payload: Any): Any? =
        suspendCancellableCoroutine { continuation ->
            functions.getHttpsCallable(name).call(payload)
                .addOnSuccessListener { continuation.resume(it.data) }
                .addOnFailureListener { continuation.resumeWithException(it) }
        }
}
