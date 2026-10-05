package com.example.backend

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.example.monetization.PlayerIdentity as PaymentPlayerIdentity

/**
 * Backend-side [PlayerIdentity] for ONLINE_AUTHORITATIVE mode.
 *
 * Two different "PlayerIdentity" types exist on purpose:
 *  - com.example.monetization.PlayerIdentity (object): payment-owned anonymous Firebase sign-in
 *    used by purchase verification. It is NOT modified here.
 *  - com.example.backend.PlayerIdentity (interface): what the gameplay backend sees.
 *
 * This adapter never signs in by itself; it reuses the payment object's sign-in so both features
 * resolve to the SAME Firebase UID (one player, one identity). It never invents an identity: with no
 * signed-in user [currentUid] throws and [getAuthToken] returns null, so nothing can be keyed to a
 * placeholder like "player_local". Servers derive the uid from the verified auth context anyway.
 */
class FirebasePlayerIdentity : PlayerIdentity {
    override val currentUid: String
        get() = Firebase.auth.currentUser?.uid ?: throw IllegalStateException("Not signed in to Firebase yet.")

    override val isAnonymous: Boolean
        get() = Firebase.auth.currentUser?.isAnonymous ?: true

    override val isAuthenticated: Boolean
        get() = Firebase.auth.currentUser != null

    override suspend fun getAuthToken(): String? {
        try {
            PaymentPlayerIdentity.ensureSignedIn()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return null
        }
        val user = Firebase.auth.currentUser ?: return null
        return suspendCancellableCoroutine { continuation ->
            user.getIdToken(false)
                .addOnSuccessListener { continuation.resume(it.token) }
                .addOnFailureListener { continuation.resume(null) }
        }
    }
}
