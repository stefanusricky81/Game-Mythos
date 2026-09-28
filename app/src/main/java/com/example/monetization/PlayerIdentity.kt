package com.example.monetization

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Stable-per-install player identity for the payment backend - NOT a login system. Firebase
 * Anonymous Auth gives a durable Firebase UID with zero user-facing sign-up/sign-in UI, which
 * is what lets verifyPlayPurchase/getPlayerEntitlements answer "which player does this purchase
 * belong to". App Check alone cannot answer that question - it only proves the CALLING APP is a
 * genuine, unmodified Mythos install, not who is using it (see SETUP_PLAY_BILLING.md, "Player
 * Identity" section, for why this distinction matters).
 *
 * KNOWN LIMITATION - read before assuming this "solves" restore: this UID lives in the app's
 * local storage. Uninstalling the app (or clearing all app data) deletes it along with
 * everything else, and the next launch mints a BRAND NEW anonymous UID with no link to the old
 * one - server-side purchase records tied to the old UID become unreachable through normal
 * means. A reinstall-safe identity would require linking this anonymous account to something
 * external (e.g. Play Games Sign-In) - intentionally NOT built here to avoid inventing an
 * insecure ad-hoc "claim my old purchases" flow. Treat that as a separate, later production
 * step (see SETUP_PLAY_BILLING.md).
 */
object PlayerIdentity {
    private val signInMutex = Mutex()

    /** Returns the current (or newly created) anonymous Firebase UID. Safe to call repeatedly. */
    suspend fun ensureSignedIn(): String {
        Firebase.auth.currentUser?.let { return it.uid }
        return signInMutex.withLock {
            // Re-check after acquiring the lock: a racing caller may have just finished signing in.
            Firebase.auth.currentUser?.let { return@withLock it.uid }
            signInAnonymously().uid
        }
    }

    private suspend fun signInAnonymously(): FirebaseUser = suspendCancellableCoroutine { continuation ->
        Firebase.auth.signInAnonymously()
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    continuation.resume(user)
                } else {
                    continuation.resumeWithException(IllegalStateException("Anonymous sign-in returned no user."))
                }
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
    }
}
