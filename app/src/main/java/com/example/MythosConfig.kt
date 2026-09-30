package com.example

/**
 * Global configuration for MYTHOS — Age of Heroes.
 * Controls production display vs development/prototype overlays.
 */
object MythosConfig {
    /**
     * When false:
     * - Hides "VERTICAL SLICE — HERCULES TEST" prototype labels.
     * - Hides development-only debug controls from normal gameplay.
     * When true:
     * - Shows dev debug controls and prototype headers.
     */
    var DEBUG_BUILD: Boolean = false

    /**
     * When false (current default): purchases go through real Google Play Billing and are
     * granted once Play reports them PURCHASED - no Firebase / Cloud Function involved.
     * When true: each purchase is additionally verified server-side by verifyPlayPurchase
     * (Anonymous Auth + App Check + Firestore ledger). Flip this ONLY after the Cloud Function
     * has been deployed (needs the Firebase Blaze plan) - see SETUP_PLAY_BILLING.md.
     */
    const val SERVER_VERIFICATION_ENABLED: Boolean = false
}
