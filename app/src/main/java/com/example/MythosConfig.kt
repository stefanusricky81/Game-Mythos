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
}
