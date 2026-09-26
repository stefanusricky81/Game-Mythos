package com.example.data

import com.example.monetization.PlayerEconomyRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Persistent Player Progress & Statistics Model (Phase 7D Sections 4, 21, 24).
 * Single authoritative container for account-level progression and gameplay statistics.
 */
data class PlayerProgress(
    val playerLevel: Int = 1,
    val playerXp: Int = 0,
    val totalBattles: Int = 0,
    val totalVictories: Int = 0,
    val totalDefeats: Int = 0,
    val totalCardsPlayed: Int = 0,
    val totalDamageDealt: Long = 0L,
    val totalDamageTaken: Long = 0L,
    val totalCampaignStagesCleared: Int = 0,
    val claimedLevelRewards: Set<Int> = emptySet()
) {
    val xpRequiredForNextLevel: Int
        get() = PlayerProgressionConfig.getXpRequiredForNextLevel(playerLevel)

    val xpProgressFraction: Float
        get() = if (playerLevel >= PlayerProgressionConfig.MAX_PLAYER_LEVEL) 1f
        else (playerXp.toFloat() / xpRequiredForNextLevel.toFloat()).coerceIn(0f, 1f)

    val isMaxLevel: Boolean
        get() = playerLevel >= PlayerProgressionConfig.MAX_PLAYER_LEVEL

    val winRatePercent: Int
        get() = if (totalBattles > 0) ((totalVictories.toFloat() / totalBattles.toFloat()) * 100).toInt() else 0
}

/**
 * Level-Up Reward structure (Phase 7D Section 6).
 */
data class PlayerLevelReward(
    val level: Int,
    val gold: Int = 0,
    val mythGems: Int = 0,
    val cardShards: Int = 0,
    val heroShards: Int = 0,
    val description: String
)

/**
 * Centralized, authoritative Player Progression configuration (Phase 7D Sections 4, 6).
 */
object PlayerProgressionConfig {
    const val MAX_PLAYER_LEVEL = 20

    // Level 1 -> 2: 1,000 XP
    // Level 2 -> 3: 2,500 XP
    // Level 3 -> 4: 5,000 XP
    // Level 4 -> 5: 8,000 XP
    // Level 5+: 8,000 + (level - 4) * 4,000
    private val XP_REQUIREMENTS = mapOf(
        1 to 1_000,
        2 to 2_500,
        3 to 5_000,
        4 to 8_000
    )

    fun getXpRequiredForNextLevel(currentLevel: Int): Int {
        if (currentLevel >= MAX_PLAYER_LEVEL) return 0
        return XP_REQUIREMENTS[currentLevel] ?: (8_000 + (currentLevel - 4) * 4_000)
    }

    // Configurable Level-Up Rewards (Section 6)
    val LEVEL_REWARDS: Map<Int, PlayerLevelReward> = mapOf(
        2 to PlayerLevelReward(level = 2, gold = 1_000, description = "+1,000 Gold"),
        3 to PlayerLevelReward(level = 3, mythGems = 50, description = "+50 Myth Gems"),
        4 to PlayerLevelReward(level = 4, cardShards = 20, description = "+20 Card Shards"),
        5 to PlayerLevelReward(level = 5, heroShards = 10, description = "+10 Hero Shards"),
        6 to PlayerLevelReward(level = 6, gold = 2_000, description = "+2,000 Gold"),
        7 to PlayerLevelReward(level = 7, mythGems = 75, description = "+75 Myth Gems"),
        8 to PlayerLevelReward(level = 8, cardShards = 30, description = "+30 Card Shards"),
        9 to PlayerLevelReward(level = 9, heroShards = 20, description = "+20 Hero Shards"),
        10 to PlayerLevelReward(level = 10, gold = 5_000, mythGems = 100, description = "+5,000 Gold & +100 Gems"),
        11 to PlayerLevelReward(level = 11, gold = 3_000, description = "+3,000 Gold"),
        12 to PlayerLevelReward(level = 12, mythGems = 100, description = "+100 Myth Gems"),
        13 to PlayerLevelReward(level = 13, cardShards = 40, description = "+40 Card Shards"),
        14 to PlayerLevelReward(level = 14, heroShards = 30, description = "+30 Hero Shards"),
        15 to PlayerLevelReward(level = 15, gold = 7_500, mythGems = 150, description = "+7,500 Gold & +150 Gems"),
        16 to PlayerLevelReward(level = 16, cardShards = 50, description = "+50 Card Shards"),
        17 to PlayerLevelReward(level = 17, heroShards = 40, description = "+40 Hero Shards"),
        18 to PlayerLevelReward(level = 18, mythGems = 200, description = "+200 Myth Gems"),
        19 to PlayerLevelReward(level = 19, gold = 10_000, description = "+10,000 Gold"),
        20 to PlayerLevelReward(level = 20, gold = 15_000, mythGems = 300, heroShards = 50, description = "+15,000 Gold, +300 Gems, +50 Hero Shards")
    )

    fun getLevelReward(level: Int): PlayerLevelReward? = LEVEL_REWARDS[level]
}

/**
 * Daily Quest Types supported by game events (Phase 7D Section 9).
 */
enum class QuestType {
    WIN_BATTLE,
    WIN_CAMPAIGN_BATTLE,
    PLAY_CARDS,
    DEAL_DAMAGE,
    UPGRADE_CARD,
    UPGRADE_HERO,
    COMPLETE_CAMPAIGN_STAGE
}

/**
 * Reward awarded upon completing and claiming a daily quest (Phase 7D Section 10).
 */
data class QuestReward(
    val gold: Int = 0,
    val mythGems: Int = 0,
    val cardShards: Int = 0,
    val cardShardCardId: String? = null,
    val heroShards: Int = 0,
    val heroShardHeroId: String? = null,
    val playerXp: Int = 0
) {
    fun toDisplayText(): String {
        val parts = mutableListOf<String>()
        if (gold > 0) parts.add("🪙 $gold Gold")
        if (mythGems > 0) parts.add("💎 $mythGems Gems")
        if (cardShards > 0) parts.add("🃏 $cardShards Shards")
        if (heroShards > 0) parts.add("🛡️ $heroShards Hero Shards")
        if (playerXp > 0) parts.add("✦ $playerXp XP")
        return parts.joinToString(" • ")
    }
}

/**
 * Individual Daily Quest Model (Phase 7D Sections 9, 10, 12).
 */
data class DailyQuest(
    val questId: String,
    val title: String,
    val description: String,
    val type: QuestType,
    val target: Int,
    val progress: Int = 0,
    val reward: QuestReward,
    val isClaimed: Boolean = false
) {
    val isCompleted: Boolean
        get() = progress >= target

    val progressFraction: Float
        get() = (progress.toFloat() / target.toFloat()).coerceIn(0f, 1f)
}

/**
 * Centralized Daily Quest Configuration & Defaults (Phase 7D Section 10).
 */
object DailyQuestConfig {
    fun createDefaultQuests(): List<DailyQuest> = listOf(
        DailyQuest(
            questId = "quest_1",
            title = "BATTLE TESTED",
            description = "Win 1 battle in Campaign or Direct Battle.",
            type = QuestType.WIN_BATTLE,
            target = 1,
            progress = 0,
            reward = QuestReward(gold = 500)
        ),
        DailyQuest(
            questId = "quest_2",
            title = "CARD MASTER",
            description = "Play 10 cards in combat.",
            type = QuestType.PLAY_CARDS,
            target = 10,
            progress = 0,
            reward = QuestReward(gold = 500, cardShards = 20, cardShardCardId = "c_power_strike")
        ),
        DailyQuest(
            questId = "quest_3",
            title = "BRUTAL FORCE",
            description = "Deal 10,000 total damage to enemies.",
            type = QuestType.DEAL_DAMAGE,
            target = 10_000,
            progress = 0,
            reward = QuestReward(gold = 750, playerXp = 50)
        ),
        DailyQuest(
            questId = "quest_4",
            title = "POWER UP",
            description = "Upgrade 1 card in your collection.",
            type = QuestType.UPGRADE_CARD,
            target = 1,
            progress = 0,
            reward = QuestReward(mythGems = 25)
        ),
        DailyQuest(
            questId = "quest_5",
            title = "CHAMPION'S PATH",
            description = "Complete 1 campaign stage.",
            type = QuestType.COMPLETE_CAMPAIGN_STAGE,
            target = 1,
            progress = 0,
            reward = QuestReward(gold = 1_000, heroShards = 50, heroShardHeroId = HerculesIdentity.HERO_ID)
        )
    )
}

/**
 * Daily Login Reward entry for the 7-day reward cycle (Phase 7D Section 16).
 */
data class DailyLoginReward(
    val day: Int,
    val gold: Int = 0,
    val mythGems: Int = 0,
    val cardShards: Int = 0,
    val heroShards: Int = 0,
    val cardRewardId: String? = null,
    val cardRewardName: String? = null,
    val title: String,
    val description: String
)

/**
 * 7-Day Login Reward Schedule (Phase 7D Section 16).
 */
object DailyLoginRewardConfig {
    val REWARDS: List<DailyLoginReward> = listOf(
        DailyLoginReward(day = 1, gold = 500, title = "500 Gold", description = "Coins of the realm"),
        DailyLoginReward(day = 2, mythGems = 25, title = "25 Myth Gems", description = "Precious celestial crystals"),
        DailyLoginReward(day = 3, cardShards = 20, title = "20 Card Shards", description = "Shards for Power Strike"),
        DailyLoginReward(day = 4, gold = 500, title = "500 Gold", description = "Coins of the realm"),
        DailyLoginReward(day = 5, heroShards = 50, title = "50 Hero Shards", description = "Hercules ascension shards"),
        DailyLoginReward(day = 6, mythGems = 50, title = "50 Myth Gems", description = "Precious celestial crystals"),
        DailyLoginReward(
            day = 7,
            gold = 1_000,
            cardRewardId = "c_heroic_rage",
            cardRewardName = "Heroic Rage",
            title = "Rare Card + 1,000 Gold",
            description = "Heroic Rage (Rare) Card Reward"
        )
    )

    fun getReward(day: Int): DailyLoginReward {
        val clamped = ((day - 1) % 7) + 1
        return REWARDS.find { it.day == clamped } ?: REWARDS[0]
    }
}

/**
 * Helper to get standard local date string in YYYY-MM-DD.
 */
object MythosDateUtil {
    fun getCurrentLocalDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}
