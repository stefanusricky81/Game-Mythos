package com.example.backend

import android.content.Context
import com.example.backend.online.OnlineBackend

/**
 * Single app-level entry point to the Phase 12 backend.
 *
 *  - LOCAL_DEVELOPMENT (default): [sync] validates decks locally; screens keep using the offline
 *    economy exactly as before. [online] must not be used.
 *  - ONLINE_AUTHORITATIVE: screens, the battle ViewModel and the reward sync talk to [online], which
 *    calls the Cloud Functions. Identity is the Firebase Auth uid (never a placeholder).
 */
object MythosBackend {
    @Volatile
    var mode: BackendMode = BackendConfig.MODE
        internal set

    val isOnline: Boolean get() = mode == BackendMode.ONLINE_AUTHORITATIVE

    private var appContext: Context? = null
    private var onlineInstance: OnlineBackend? = null

    /** Called once from MainActivity.onCreate (the Application class is payment-owned and untouched). */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    val identity: PlayerIdentity by lazy {
        if (BackendConfig.MODE == BackendMode.ONLINE_AUTHORITATIVE) FirebasePlayerIdentity() else DefaultPlayerIdentity.Instance
    }

    /** Local deck validation boundary used in LOCAL_DEVELOPMENT mode. */
    val sync: GameSyncRepository by lazy { GameSyncRepository(playerIdentity = identity) }

    val online: OnlineBackend
        get() = onlineInstance ?: synchronized(this) {
            onlineInstance ?: OnlineBackend.create(
                appContext ?: error("MythosBackend.initialize(context) must run before the online backend is used.")
            ).also { onlineInstance = it }
        }

    /** Test seam: swap in a backend wired to a fake transport and force a mode. */
    internal fun installForTesting(backend: OnlineBackend?, mode: BackendMode) {
        onlineInstance = backend
        this.mode = mode
    }
}
