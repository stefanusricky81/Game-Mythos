package com.example.data

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.MythosTokens

/**
 * Competitive Rank Tiers (Requirement #5).
 * Centralized configuration of thresholds, names, badges, and colors.
 */
enum class ArenaRankTier(
    val tierName: String,
    val minRating: Int,
    val maxRating: Int,
    val badgeSymbol: String,
    val badgeColorHex: Long,
    val frameRewardId: String
) {
    BRONZE("Bronze", 0, 999, "🥉", 0xFFCD7F32, "frame_default_bronze"),
    SILVER("Silver", 1000, 1299, "🥈", 0xFFC0C0C0, "frame_silver_warrior"),
    GOLD("Gold", 1300, 1599, "🥇", 0xFFFFD700, "frame_gold_champion"),
    PLATINUM("Platinum", 1600, 1899, "💎", 0xFF38BDF8, "frame_platinum_gladiator"),
    DIAMOND("Diamond", 1900, 2199, "💠", 0xFF818CF8, "frame_diamond_warlord"),
    MASTER("Master", 2200, 2499, "⚔️", 0xFFA855F7, "frame_master_conqueror"),
    GRANDMASTER("Grandmaster", 2500, 2799, "👑", 0xFFF43F5E, "frame_grandmaster_mythic"),
    MYTHIC("Mythic", 2800, Int.MAX_VALUE, "🌟", 0xFFF59E0B, "frame_mythic_sovereign");

    val color: Color get() = Color(badgeColorHex)

    companion object {
        fun fromRating(rating: Int): ArenaRankTier {
            val clamped = maxOf(0, rating)
            return entries.find { clamped in it.minRating..it.maxRating } ?: MYTHIC
        }

        fun getNextTier(currentTier: ArenaRankTier): ArenaRankTier? {
            val idx = currentTier.ordinal
            return if (idx < entries.size - 1) entries[idx + 1] else null
        }

        fun calculateProgressToNextTier(rating: Int): Float {
            val tier = fromRating(rating)
            if (tier == MYTHIC) return 1.0f
            val span = (tier.maxRating - tier.minRating + 1).toFloat()
            val progress = (rating - tier.minRating).toFloat() / span
            return progress.coerceIn(0.0f, 1.0f)
        }
    }
}

/**
 * PvP Archetypes for Deterministic Simulated Opponents (Requirement #3).
 */
enum class ArenaOpponentArchetype(
    val title: String,
    val heroName: String,
    val heroId: String,
    val description: String,
    val preferredStrategy: String
) {
    OLYMPUS_VANGUARD(
        title = "Olympus Vanguard",
        heroName = "Hercules",
        heroId = HerculesIdentity.HERO_ID,
        description = "Balanced Olympian bruiser focused on divine strike & unbreakable guard.",
        preferredStrategy = "Balanced Melee & Defense"
    ),
    SPARTAN_WARBORN(
        title = "Spartan Warborn",
        heroName = "Ares",
        heroId = "ares",
        description = "Aggressive warlord unleashing fierce bleed strikes and ferocious crits.",
        preferredStrategy = "Aggressive Burst"
    ),
    VALHALLA_STORM(
        title = "Valhalla Storm",
        heroName = "Thor",
        heroId = "thor",
        description = "Norse storm warrior wielding lightning bursts and raw crushing overwhelm.",
        preferredStrategy = "Heavy Shock & Overwhelm"
    ),
    UNDERWORLD_REAPER(
        title = "Underworld Reaper",
        heroName = "Hades",
        heroId = "hades",
        description = "Dark sovereign siphoning enemy life force with curses and soul drain.",
        preferredStrategy = "Control & Life Siphon"
    ),
    EGYPTIAN_JUDGMENT(
        title = "Egyptian Judgment",
        heroName = "Anubis",
        heroId = "anubis",
        description = "Underworld guardian weighing mortal souls with divine tomb execution.",
        preferredStrategy = "Debuff & Execute"
    ),
    CELESTIAL_GUARDIAN(
        title = "Celestial Guardian",
        heroName = "Athena",
        heroId = "athena",
        description = "Tactical shield commander utilizing impervious defense and strategic counter.",
        preferredStrategy = "Defense & Shield Retaliation"
    )
}

/**
 * Deterministic Simulated Opponent Model (Requirement #3).
 */
data class ArenaOpponent(
    val id: String,
    val name: String,
    val heroId: String,
    val heroName: String,
    val archetype: ArenaOpponentArchetype,
    val rating: Int,
    val combatPower: Int,
    val deckCardIds: List<String>, // Exactly 20 cards
    val avatarId: String = "avatar_default_hercules",
    val title: String = "Gladiator of Mythos"
) {
    val tier: ArenaRankTier get() = ArenaRankTier.fromRating(rating)
}

/**
 * Result of an Arena Battle (Requirement #2, #12).
 */
enum class ArenaMatchResult {
    VICTORY,
    DEFEAT,
    DRAW
}

/**
 * Persistent Arena Match Record (Requirement #2, #14).
 */
data class ArenaMatchRecord(
    val matchId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val dateFormatted: String,
    val opponentName: String,
    val opponentHeroName: String,
    val playerHeroName: String,
    val opponentArchetype: String,
    val result: ArenaMatchResult,
    val ratingBefore: Int,
    val ratingAfter: Int,
    val ratingChange: Int,
    val turns: Int,
    val durationSeconds: Int = 45,
    val goldAwarded: Int = 0,
    val xpAwarded: Int = 0,
    val shardsAwarded: Int = 0,
    val arenaPointsAwarded: Int = 0
)

/**
 * Seasonal Competitive Model (Requirement #9).
 */
data class ArenaSeason(
    val seasonId: String = "season_1",
    val seasonNumber: Int = 1,
    val startDateMs: Long = 0L,
    val endDateMs: Long = Long.MAX_VALUE,
    val rating: Int = 1000,
    val peakRating: Int = 1000,
    val wins: Int = 0,
    val losses: Int = 0,
    val matches: Int = 0,
    val isRewardsClaimed: Boolean = false
) {
    val durationDays: Int = 28
    val winRate: Float get() = if (matches > 0) (wins.toFloat() / matches) * 100f else 0f
    val rankTier: ArenaRankTier get() = ArenaRankTier.fromRating(rating)

    companion object {
        fun createDefaultSeason(nowMs: Long = System.currentTimeMillis()): ArenaSeason = ArenaSeason(
            seasonId = "season_1",
            seasonNumber = 1,
            startDateMs = nowMs,
            endDateMs = nowMs + (28L * 24 * 60 * 60 * 1000)
        )
    }
}

/**
 * Seasonal Reward Tier Entry (Requirement #10).
 */
data class ArenaSeasonReward(
    val tier: ArenaRankTier,
    val gold: Int,
    val cardShards: Int,
    val heroShards: Int,
    val exclusiveFrameId: String? = null,
    val exclusiveCosmeticId: String? = null,
    val exclusiveTitle: String? = null
)

/**
 * Leaderboard Entry (Requirement #11).
 */
data class ArenaLeaderboardEntry(
    val rank: Int,
    val playerName: String,
    val avatarId: String,
    val rating: Int,
    val tier: ArenaRankTier,
    val wins: Int,
    val losses: Int,
    val winRate: Float,
    val peakRating: Int,
    val isCurrentPlayer: Boolean = false
)

/**
 * Summary of Rewards & Rating changes after completing an Arena match.
 */
data class ArenaBattleResultSummary(
    val matchId: String,
    val result: ArenaMatchResult,
    val opponentName: String,
    val ratingBefore: Int,
    val ratingAfter: Int,
    val ratingChange: Int,
    val newTier: ArenaRankTier,
    val isTierUpgraded: Boolean,
    val goldAwarded: Int,
    val xpAwarded: Int,
    val cardShardsAwarded: Int,
    val arenaPointsAwarded: Int,
    val isFirstWinOfDay: Boolean,
    val currentStreak: Int,
    val streakBonusGold: Int = 0,
    val streakBonusPoints: Int = 0
)

/**
 * Matchmaking State Machine (Requirement #6).
 */
enum class ArenaMatchmakingState {
    IDLE,
    SEARCHING,
    MATCH_FOUND,
    CANCELLED
}
