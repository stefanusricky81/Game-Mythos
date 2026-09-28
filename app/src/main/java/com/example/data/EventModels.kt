package com.example.data

import com.example.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Event Types for seasonal and time-limited content (Phase 8 Section 8).
 */
enum class EventType {
    RAID_BOSS,
    DOUBLE_GOLD,
    OLYMPUS_TRIAL,
    SEASON_EXPEDITION;

    val displayTag: String
        get() = when (this) {
            RAID_BOSS -> "WORLD RAID"
            DOUBLE_GOLD -> "DOUBLE BOUNTY"
            OLYMPUS_TRIAL -> "OLYMPIAN TRIAL"
            SEASON_EXPEDITION -> "SPECIAL EXPEDITION"
        }
}

/**
 * Time-Limited Event Model (Phase 8 Section 8).
 */
data class MythosEvent(
    val eventId: String,
    val title: String,
    val description: String,
    val startDate: String,
    val endDate: String,
    val bannerResId: Int = R.drawable.img_battlefield_bg,
    val eventType: EventType,
    val rewardSummary: String,
    val participationRequirement: String = "Player Level 1+",
    val isActive: Boolean = true
) {
    fun isWithinDateRange(currentDate: String = MythosDateUtil.getCurrentLocalDate()): Boolean {
        return currentDate in startDate..endDate
    }

    val isCurrentlyActive: Boolean
        get() = isActive && isWithinDateRange()
}

/**
 * In-game notification / event alert model for retention (Phase 8 Section 7).
 */
data class MythosNotification(
    val id: String,
    val title: String,
    val message: String,
    val category: String, // "REWARD", "QUEST", "ALLIANCE", "EVENT"
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val targetRoute: String? = null
)

/**
 * Centralized Event Catalog & Defaults (Phase 8 Section 8).
 */
object EventCatalog {
    fun getDefaultEvents(): List<MythosEvent> = listOf(
        MythosEvent(
            eventId = "event_typhon_raid",
            title = "WRATH OF TYPHON",
            description = "The ancient storm titan awakens in Tartarus! Join forces with your Alliance to challenge the behemoth.",
            startDate = "2026-09-20",
            endDate = "2026-10-05",
            bannerResId = R.drawable.img_battlefield_bg,
            eventType = EventType.RAID_BOSS,
            rewardSummary = "500 Myth Gems • 50 Rare Shards • Epic Title",
            participationRequirement = "Player Level 3+ • Alliance Member",
            isActive = true
        ),
        MythosEvent(
            eventId = "event_midas_bounty",
            title = "BLESSING OF MIDAS",
            description = "The Aegean waters sparkle with gold! All Campaign replays and Direct Battles grant double Gold.",
            startDate = "2026-09-25",
            endDate = "2026-09-30",
            bannerResId = R.drawable.img_battlefield_bg,
            eventType = EventType.DOUBLE_GOLD,
            rewardSummary = "2x Gold Multiplier on All Encounters",
            participationRequirement = "All Champions",
            isActive = true
        ),
        MythosEvent(
            eventId = "event_ares_trial",
            title = "ARES GLADIATOR TRIALS",
            description = "Prove your worth in the sacred proving grounds under the gaze of the God of War.",
            startDate = "2026-10-01",
            endDate = "2026-10-14",
            bannerResId = R.drawable.img_battlefield_bg,
            eventType = EventType.OLYMPUS_TRIAL,
            rewardSummary = "1,000 Gold • 30 Achilles Shards • War Trophy",
            participationRequirement = "Clear Campaign Stage 2",
            isActive = false // Upcoming
        )
    )

    fun findEvent(eventId: String): MythosEvent? =
        getDefaultEvents().find { it.eventId == eventId }
}
