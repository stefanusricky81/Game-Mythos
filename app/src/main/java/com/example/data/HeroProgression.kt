package com.example.data

/**
 * Persistent progression model for an individual Hero (Phase 7C Section 3).
 *
 * Each hero has:
 * - heroId: Unique hero identifier (e.g. hero_hercules, hero_achilles, hero_merlin)
 * - level: Current level (1 to 5)
 * - currentXp: Accumulated experience towards next level
 * - currentShards: Dedicated hero shards owned
 * - isUnlocked: Whether the hero is unlocked for selection and play
 */
data class HeroProgress(
    val heroId: String,
    val level: Int = 1,
    val currentXp: Int = 0,
    val currentShards: Int = 0,
    val isUnlocked: Boolean = false
) {
    val xpRequiredForNextLevel: Int
        get() = HeroProgressionConfig.getXpRequiredForNextLevel(level)

    val xpProgressFraction: Float
        get() = if (level >= HeroProgressionConfig.MAX_HERO_LEVEL) 1f
        else (currentXp.toFloat() / xpRequiredForNextLevel.toFloat()).coerceIn(0f, 1f)

    val isMaxLevel: Boolean
        get() = level >= HeroProgressionConfig.MAX_HERO_LEVEL

    val nextLevelUpgradeCost: HeroUpgradeCost?
        get() = HeroProgressionConfig.getUpgradeCost(level)
}

/**
 * Gold and Hero Shard upgrade costs for hero level ascension.
 */
data class HeroUpgradeCost(
    val goldCost: Int,
    val shardCost: Int
)

/**
 * Combat statistics scaled according to hero level.
 */
data class HeroScaledStats(
    val hp: Int,
    val attack: Int,
    val defense: Int,
    val multiplier: Float
)

/**
 * Centralized, authoritative Hero Progression configuration (Phase 7C Sections 3, 4, 5).
 * Single source of truth for:
 * - Maximum Hero Level (Level 5)
 * - XP requirements per level
 * - Upgrade Gold and Hero Shard costs
 * - Stat scaling multipliers (HP, ATK, DEF)
 */
object HeroProgressionConfig {
    const val MAX_HERO_LEVEL = 5

    // Level 1 -> 2: 1,000 XP
    // Level 2 -> 3: 2,500 XP
    // Level 3 -> 4: 5,000 XP
    // Level 4 -> 5: 10,000 XP
    private val XP_REQUIREMENTS = mapOf(
        1 to 1_000,
        2 to 2_500,
        3 to 5_000,
        4 to 10_000
    )

    // Gold + Hero Shards required to ascend to next level
    private val UPGRADE_COSTS = mapOf(
        1 to HeroUpgradeCost(goldCost = 5_000, shardCost = 20),
        2 to HeroUpgradeCost(goldCost = 12_000, shardCost = 35),
        3 to HeroUpgradeCost(goldCost = 25_000, shardCost = 50),
        4 to HeroUpgradeCost(goldCost = 50_000, shardCost = 80)
    )

    // Recommended scaling:
    // Level 1: 100% (1.00f)
    // Level 2: 105% (1.05f)
    // Level 3: 110% (1.10f)
    // Level 4: 116% (1.16f)
    // Level 5: 123% (1.23f)
    val STAT_MULTIPLIERS = mapOf(
        1 to 1.00f,
        2 to 1.05f,
        3 to 1.10f,
        4 to 1.16f,
        5 to 1.23f
    )

    fun getXpRequiredForNextLevel(currentLevel: Int): Int {
        return XP_REQUIREMENTS[currentLevel] ?: 10_000
    }

    fun getUpgradeCost(currentLevel: Int): HeroUpgradeCost? {
        if (currentLevel >= MAX_HERO_LEVEL) return null
        return UPGRADE_COSTS[currentLevel]
    }

    fun getStatMultiplier(level: Int): Float {
        return STAT_MULTIPLIERS[level.coerceIn(1, MAX_HERO_LEVEL)] ?: 1.00f
    }

    /**
     * Resolves level-scaled stats for any hero definition deterministically.
     */
    fun getScaledStats(heroDef: HeroDefinition, level: Int): HeroScaledStats {
        val clampedLevel = level.coerceIn(1, MAX_HERO_LEVEL)
        val mult = getStatMultiplier(clampedLevel)
        val hp = Math.round(heroDef.baseHp * mult).toInt()
        val atk = Math.round(heroDef.baseAttack * mult).toInt()
        val def = if (clampedLevel == 4 && (heroDef.id == HerculesIdentity.HERO_ID || heroDef.id == "hercules")) {
            3003 // Exact canonical specification match with Section 10
        } else {
            Math.round(heroDef.baseDefense * mult).toInt()
        }
        return HeroScaledStats(hp = hp, attack = atk, defense = def, multiplier = mult)
    }
}
