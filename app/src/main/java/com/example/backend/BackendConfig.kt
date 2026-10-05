package com.example.backend

enum class BackendMode {
    /** Offline prototype: the local economy is the only source of truth. No Firebase traffic from Phase 12. */
    LOCAL_DEVELOPMENT,

    /**
     * Server-authoritative: Arena, World Boss, Raid, Event rewards and Alliance go through the Phase 12
     * Cloud Functions. Nothing authoritative is granted locally; rewards arrive from the server's reward
     * outbox. Requires the functions to be deployed (Firebase Blaze plan) - see PHASE12_BACKEND.md.
     */
    ONLINE_AUTHORITATIVE,
}

/**
 * Phase 12 backend switches. Deliberately independent of MythosConfig.SERVER_VERIFICATION_ENABLED,
 * which belongs to payment verification and must never be coupled to gameplay.
 */
object BackendConfig {
    val MODE: BackendMode = BackendMode.LOCAL_DEVELOPMENT

    /** Same region as the payment functions. */
    const val FUNCTIONS_REGION: String = "asia-southeast1"
}
