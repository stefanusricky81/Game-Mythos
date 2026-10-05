package com.example.backend.online

import android.util.Log
import com.example.BuildConfig
import com.example.MythosConfig
import com.example.backend.BackendConfig
import com.example.backend.PlayerIdentity
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.functions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * An error returned by (or while reaching) the Phase 12 backend. [code] is the stable Cloud Functions
 * error code ("failed-precondition", "already-exists", ...), or "unavailable" for network problems.
 */
class BackendException(
    val code: String,
    override val message: String,
    val details: Map<String, Any?> = emptyMap()
) : Exception(message) {
    /** Worth retrying later with the SAME request id (nothing was decided by the server). */
    val isTransient: Boolean get() = code == "unavailable" || code == "deadline-exceeded" || code == "aborted"
}

interface BackendTransport {
    /** Calls a Cloud Function and returns its decoded response, or throws [BackendException]. */
    suspend fun call(function: String, payload: Map<String, Any?>): Map<String, Any?>
}

/**
 * Production transport: Firebase callable functions in the same region as the payment functions.
 * The Functions SDK attaches the Firebase ID token and (when installed) the App Check token; the
 * server derives the player's uid from that verified context and never from the payload.
 */
class FirebaseCallableTransport(
    private val identity: PlayerIdentity,
    private val region: String = BackendConfig.FUNCTIONS_REGION
) : BackendTransport {

    private val functions by lazy { Firebase.functions(region) }
    private val appCheckLock = Any()
    @Volatile private var appCheckInstalled = false

    override suspend fun call(function: String, payload: Map<String, Any?>): Map<String, Any?> {
        installAppCheckOnce()
        // Ensures an anonymous Firebase session exists (reusing the payment sign-in) before calling.
        identity.getAuthToken() ?: throw BackendException("unavailable", "Could not sign in. Check your connection.")

        return suspendCancellableCoroutine { continuation ->
            functions.getHttpsCallable(function).call(payload)
                .addOnSuccessListener { result ->
                    @Suppress("UNCHECKED_CAST")
                    continuation.resume((result.data as? Map<String, Any?>) ?: emptyMap())
                }
                .addOnFailureListener { error -> continuation.resumeWithException(map(error)) }
        }
    }

    private fun map(error: Exception): BackendException {
        if (error is FirebaseFunctionsException) {
            val details = (error.details as? Map<*, *>)?.entries
                ?.associate { it.key.toString() to it.value } ?: emptyMap()
            return BackendException(error.code.name.lowercase().replace('_', '-'), error.message ?: "Request failed.", details)
        }
        return BackendException("unavailable", error.message ?: "Network error.")
    }

    /** App Check is required by the server only when ENFORCE_APP_CHECK=true; installing it is always safe. */
    private fun installAppCheckOnce() {
        if (appCheckInstalled) return
        synchronized(appCheckLock) {
            if (appCheckInstalled) return
            try {
                val factory = if (BuildConfig.DEBUG || MythosConfig.DEBUG_BUILD) {
                    DebugAppCheckProviderFactory.getInstance()
                } else {
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                }
                Firebase.appCheck.installAppCheckProviderFactory(factory)
            } catch (e: Exception) {
                Log.w("BackendTransport", "App Check could not be installed; calls will be unattested.", e)
            }
            appCheckInstalled = true
        }
    }
}
