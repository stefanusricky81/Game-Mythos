package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.initialize

/**
 * Installs Firebase App Check before any other Firebase API (Functions) is touched.
 * App Check is what lets verifyPlayPurchase trust a call without the game having user
 * accounts: Play Integrity attests the request came from a genuine, unmodified install
 * of this app instead of a script replaying a captured purchase token.
 *
 * Deliberately defensive: until SETUP_PLAY_BILLING.md's Firebase project setup is done (no
 * app/google-services.json yet), Firebase.initialize() throws. The rest of the game - Battle,
 * Campaign, Deck Builder, Collection, Hero Progression - has nothing to do with Firebase and
 * must keep working regardless; only real-money purchases depend on this succeeding
 * (GooglePlayBillingProvider/FirebasePurchaseVerifier already surface a clear "could not
 * verify" failure if Firebase/App Check never got wired up, rather than assuming success).
 */
class MythosApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Firebase/App Check only matter for server-side purchase verification. While that is
        // switched off, don't start Firebase at all (no network calls, nothing to misconfigure).
        if (!MythosConfig.SERVER_VERIFICATION_ENABLED) return

        try {
            Firebase.initialize(this)

            val providerFactory = if (MythosConfig.DEBUG_BUILD) {
                // Debug provider prints a token in Logcat that must be registered once in the
                // Firebase Console (App Check > Manage debug tokens) for local/emulator testing.
                DebugAppCheckProviderFactory.getInstance()
            } else {
                PlayIntegrityAppCheckProviderFactory.getInstance()
            }
            Firebase.appCheck.installAppCheckProviderFactory(providerFactory)
        } catch (e: Exception) {
            Log.w(
                "MythosApplication",
                "Firebase/App Check did not initialize (expected until SETUP_PLAY_BILLING.md " +
                    "is completed - google-services.json missing/invalid). Real-money purchases " +
                    "will fail verification until this is fixed; the rest of the game is unaffected.",
                e
            )
        }
    }
}
