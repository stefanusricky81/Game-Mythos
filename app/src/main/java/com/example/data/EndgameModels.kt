package com.example.data

import com.example.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Endgame Difficulty Modes with Centralized Scaling Multipliers (Phase 10 Section 1).
 */
enum class EndgameDifficulty(
    val id: String,
    val displayName: String,
    val multiplier: Float,
    val rewardMultiplier: Float,
    val recommendedPowerMultiplier: Float,
    val description: String
) {
    NORMAL(
        id = "normal",
        displayName = "Normal",
        multiplier = 1.0f,
        rewardMultiplier = 1.0f,
        recommendedPowerMultiplier = 1.0f,
        description = "Standard difficulty. Ideal for balanced decks."
    ),
    HARD(
        id = "hard",
        displayName = "Hard",
        multiplier = 1.35f,
        rewardMultiplier = 1.5f,
        recommendedPowerMultiplier = 1.35f,
        description = "Empowered foes (+35% stats). Enhanced shard bounties."
    ),
    NIGHTMARE(
        id = "nightmare",
        displayName = "Nightmare",
        multiplier = 1.75f,
        rewardMultiplier = 2.0f,
        recommendedPowerMultiplier = 1.75f,
        description = "Punishing encounter (+75% stats). Double rewards."
    ),
    MYTHIC(
        id = "mythic",
        displayName = "Mythic",
        multiplier = 2.25f,
        rewardMultiplier = 3.0f,
        recommendedPowerMultiplier = 2.25f,
        description = "Godly challenge (+125% stats). Triple rewards & mythic crests."
    );

    companion object {
        fun fromId(id: String): EndgameDifficulty =
            entries.find { it.id.equals(id, ignoreCase = true) } ?: NORMAL

        fun getHpMultiplier(difficulty: EndgameDifficulty): Float = difficulty.multiplier
        fun getAtkMultiplier(difficulty: EndgameDifficulty): Float = difficulty.multiplier
        fun getDefMultiplier(difficulty: EndgameDifficulty): Float = difficulty.multiplier
        fun getRewardMultiplier(difficulty: EndgameDifficulty): Float = difficulty.rewardMultiplier
        fun getRecommendedPower(basePower: Int, difficulty: EndgameDifficulty): Int =
            (basePower * difficulty.recommendedPowerMultiplier).toInt()
    }
}

/**
 * Endgame Challenge Stage / Trial Model (Phase 10 Section 2).
 */
data class EndgameChallengeStage(
    val stageId: String,
    val name: String,
    val world: String,
    val description: String,
    val defaultDifficulty: EndgameDifficulty = EndgameDifficulty.NORMAL,
    val baseRecommendedPower: Int,
    val enemyHeroId: String,
    val enemyName: String,
    val enemyTitle: String,
    val enemyFaction: String,
    val baseEnemyHp: Int,
    val baseEnemyAtk: Int,
    val baseEnemyDef: Int,
    val baseGoldReward: Int,
    val baseXpReward: Int,
    val baseEventTokens: Int,
    val cardRewardId: String,
    val cardRewardName: String,
    val cardRewardRarity: CardRarity,
    val starConditions: List<String> = listOf("Clear the Trial", "Remain above 50% HP", "Win in <= 8 turns"),
    val enemyPortraitResId: Int = R.drawable.img_enemy_hero,
    val isBoss: Boolean = true
) {
    fun toBattleEncounterConfig(difficulty: EndgameDifficulty): BattleEncounterConfig {
        val hpMult = EndgameDifficulty.getHpMultiplier(difficulty)
        val atkMult = EndgameDifficulty.getAtkMultiplier(difficulty)
        val defMult = EndgameDifficulty.getDefMultiplier(difficulty)
        val rewMult = EndgameDifficulty.getRewardMultiplier(difficulty)

        val scaledHp = (baseEnemyHp * hpMult).toInt()
        val scaledAtk = (baseEnemyAtk * atkMult).toInt()
        val scaledDef = (baseEnemyDef * defMult).toInt()

        val scaledGold = (baseGoldReward * rewMult).toInt()
        val scaledXp = (baseXpReward * rewMult).toInt()

        val enemyHero = Hero(
            id = enemyHeroId,
            name = enemyName,
            title = "$enemyTitle (${difficulty.displayName})",
            currentHp = scaledHp,
            maxHp = scaledHp,
            baseAttack = scaledAtk,
            baseDefense = scaledDef,
            portraitResId = enemyPortraitResId
        )

        return BattleEncounterConfig(
            stageId = stageId,
            stageNumber = 99,
            encounterName = "$name [${difficulty.displayName}]",
            enemyHero = enemyHero,
            rewards = BattleRewards(
                gold = scaledGold,
                xp = scaledXp,
                cardRewardName = cardRewardName,
                cardRewardRarity = cardRewardRarity
            ),
            rewardCardId = cardRewardId,
            isBoss = isBoss,
            maxTurnsForStarCondition = 8
        )
    }
}

/**
 * World Boss Phase Model (Phase 10 Section 3).
 */
data class WorldBossPhase(
    val phaseNumber: Int,
    val name: String,
    val triggerHpPercent: Float, // e.g. 1.0f for Phase 1, 0.66f for Phase 2, 0.33f for Phase 3
    val attackMultiplier: Float,
    val defenseMultiplier: Float,
    val abilityName: String,
    val abilityDescription: String,
    val phaseBannerText: String
)

/**
 * World Boss Definition (Phase 10 Section 3).
 */
data class WorldBossDefinition(
    val bossId: String,
    val name: String,
    val title: String,
    val mythology: String,
    val maxHp: Long,
    val baseAttack: Int,
    val baseDefense: Int,
    val phases: List<WorldBossPhase>,
    val defaultDifficulty: EndgameDifficulty = EndgameDifficulty.MYTHIC,
    val startTime: String,
    val endTime: String,
    val portraitResId: Int = R.drawable.img_enemy_hero,
    val lore: String
) {
    fun getCurrentPhase(currentHp: Long): WorldBossPhase {
        val ratio = currentHp.toFloat() / maxHp.toFloat()
        return phases.sortedBy { it.triggerHpPercent }.firstOrNull { ratio <= it.triggerHpPercent }
            ?: phases.first()
    }
}

/**
 * World Boss Personal Contribution Tracking (Phase 10 Section 5).
 */
data class WorldBossContribution(
    val bossId: String,
    val totalDamage: Long = 0L,
    val highestSingleHit: Long = 0L,
    val battlesCompleted: Int = 0,
    val victories: Int = 0,
    val participationCount: Int = 0,
    val contributionScore: Long = 0L,
    val claimedRewardTier: String? = null
)

/**
 * World Boss Reward Tiers (Phase 10 Section 6).
 */
enum class WorldBossRewardTier(
    val displayName: String,
    val minPercentile: Float,
    val goldReward: Int,
    val eventTokensReward: Int,
    val heroShardsReward: Int,
    val cardShardsReward: Int,
    val badgeName: String
) {
    TOP_1_PERCENT(
        displayName = "Top 1% — Mythic Slayer",
        minPercentile = 0.01f,
        goldReward = 50_000,
        eventTokensReward = 200,
        heroShardsReward = 50,
        cardShardsReward = 100,
        badgeName = "Mythic Chronos Crest"
    ),
    TOP_5_PERCENT(
        displayName = "Top 5% — Legendary Vanguard",
        minPercentile = 0.05f,
        goldReward = 30_000,
        eventTokensReward = 120,
        heroShardsReward = 30,
        cardShardsReward = 60,
        badgeName = "Titan Dominator Sigil"
    ),
    TOP_10_PERCENT(
        displayName = "Top 10% — Epic Conqueror",
        minPercentile = 0.10f,
        goldReward = 18_000,
        eventTokensReward = 80,
        heroShardsReward = 20,
        cardShardsReward = 40,
        badgeName = "Pantheon Defender"
    ),
    TOP_25_PERCENT(
        displayName = "Top 25% — Rare Assailant",
        minPercentile = 0.25f,
        goldReward = 10_000,
        eventTokensReward = 50,
        heroShardsReward = 10,
        cardShardsReward = 20,
        badgeName = "Heroic Challenger"
    ),
    PARTICIPATION(
        displayName = "Participation Bounty",
        minPercentile = 1.0f,
        goldReward = 5_000,
        eventTokensReward = 25,
        heroShardsReward = 5,
        cardShardsReward = 10,
        badgeName = "Titan Participant"
    );

    companion object {
        fun determineTier(damage: Long): WorldBossRewardTier = when {
            damage >= 1_000_000L -> TOP_1_PERCENT
            damage >= 500_000L -> TOP_5_PERCENT
            damage >= 200_000L -> TOP_10_PERCENT
            damage >= 50_000L -> TOP_25_PERCENT
            else -> PARTICIPATION
        }
    }
}

/**
 * Alliance Raid Board Entry (Phase 10 Section 7).
 */
data class AllianceRaidBoardEntry(
    val allianceName: String,
    val membersCount: Int,
    val maxMembers: Int = 20,
    val totalDamage: Long,
    val rank: Int
)

/**
 * Raid Stage within a multi-stage raid (Phase 10 Section 8).
 */
data class RaidStage(
    val stageNumber: Int,
    val name: String,
    val description: String,
    val enemyName: String,
    val enemyTitle: String,
    val enemyHp: Int,
    val enemyAtk: Int,
    val enemyDef: Int,
    val recommendedPower: Int,
    val goldReward: Int,
    val eventTokensReward: Int,
    val xpReward: Int = 1500,
    val isBossStage: Boolean = false,
    val enemyPortraitResId: Int = R.drawable.img_enemy_hero
) {
    fun toBattleEncounterConfig(raidId: String, difficulty: EndgameDifficulty): BattleEncounterConfig {
        val hpMult = EndgameDifficulty.getHpMultiplier(difficulty)
        val atkMult = EndgameDifficulty.getAtkMultiplier(difficulty)
        val defMult = EndgameDifficulty.getDefMultiplier(difficulty)
        val rewMult = EndgameDifficulty.getRewardMultiplier(difficulty)

        val scaledHp = (enemyHp * hpMult).toInt()
        val scaledAtk = (enemyAtk * atkMult).toInt()
        val scaledDef = (enemyDef * defMult).toInt()
        val scaledGold = (goldReward * rewMult).toInt()
        val scaledXp = (xpReward * rewMult).toInt()

        val enemyHero = Hero(
            id = "raid_${raidId}_$stageNumber",
            name = enemyName,
            title = enemyTitle,
            currentHp = scaledHp,
            maxHp = scaledHp,
            baseAttack = scaledAtk,
            baseDefense = scaledDef,
            portraitResId = enemyPortraitResId
        )

        return BattleEncounterConfig(
            stageId = "raid_${raidId}_stage_$stageNumber",
            stageNumber = stageNumber,
            encounterName = "$name (${difficulty.displayName})",
            enemyHero = enemyHero,
            rewards = BattleRewards(
                gold = scaledGold,
                xp = scaledXp,
                cardRewardName = "Raid Victory Cache",
                cardRewardRarity = if (isBossStage) CardRarity.LEGENDARY else CardRarity.EPIC
            ),
            rewardCardId = if (isBossStage) "c_titans_wrath" else "c_olympian_guard",
            isBoss = isBossStage,
            maxTurnsForStarCondition = 10
        )
    }
}

/**
 * Raid Definition (Phase 10 Section 8).
 */
data class RaidDefinition(
    val raidId: String,
    val name: String,
    val subtitle: String,
    val mythology: String,
    val difficulty: EndgameDifficulty,
    val recommendedPower: Int,
    val stages: List<RaidStage>,
    val bossName: String,
    val entryRequirements: String = "Player Level 5+ • Active Alliance Member",
    val rewardSummary: String,
    val bannerResId: Int = R.drawable.img_battlefield_bg
)

/**
 * Event Reward Shop Item (Phase 10 Section 12).
 * 100% in-game currency shop. Absolute Payment Rule: NO real-money purchases!
 */
data class EventShopItem(
    val itemId: String,
    val name: String,
    val description: String,
    val tokenPrice: Int,
    val category: String, // "HERO_SHARDS", "CARD_SHARDS", "GOLD", "EXCLUSIVE_CARD", "COSMETIC"
    val purchaseLimit: Int,
    val rewardGold: Int = 0,
    val rewardHeroShards: Int = 0,
    val rewardHeroId: String = HerculesIdentity.HERO_ID,
    val rewardCardShards: Int = 0,
    val rewardCardId: String = "c_titans_wrath",
    val rewardCardName: String = "",
    val rewardCosmeticId: String = ""
)

/**
 * Weekly Endgame Objectives (Phase 10 Section 13).
 */
enum class WeeklyObjectiveType {
    WIN_BATTLES,
    DEAL_DAMAGE,
    CLEAR_ENDGAME_STAGES,
    PARTICIPATE_RAIDS,
    DEAL_WORLD_BOSS_DAMAGE
}

data class WeeklyObjective(
    val id: String,
    val type: WeeklyObjectiveType,
    val title: String,
    val description: String,
    val target: Long,
    val progress: Long = 0L,
    val isClaimed: Boolean = false,
    val goldReward: Int,
    val eventTokensReward: Int,
    val heroShardsReward: Int
) {
    val isCompleted: Boolean get() = progress >= target
    val canClaim: Boolean get() = isCompleted && !isClaimed
}

/**
 * Centralized Endgame Catalogs (Phase 10 Sections 1-13).
 */
object EndgameCatalog {

    // --- 1. ENDGAME CHALLENGE STAGES (TRIALS) ---
    val OLYMPUS_TRIAL = EndgameChallengeStage(
        stageId = "trial_olympus",
        name = "Olympus Trial",
        world = "Olympus",
        description = "Ascend the sacred heights and challenge the vanguard of the Olympians.",
        baseRecommendedPower = 18_000,
        enemyHeroId = "trial_enemy_olympus",
        enemyName = "Aegis Champion",
        enemyTitle = "Vanguard of Olympus",
        enemyFaction = "Greek / Olympus",
        baseEnemyHp = 22_000,
        baseEnemyAtk = 3_200,
        baseEnemyDef = 2_800,
        baseGoldReward = 4_500,
        baseXpReward = 3_000,
        baseEventTokens = 35,
        cardRewardId = "c_divine_challenge",
        cardRewardName = "Divine Challenge",
        cardRewardRarity = CardRarity.EPIC
    )

    val VALHALLA_TRIAL = EndgameChallengeStage(
        stageId = "trial_valhalla",
        name = "Valhalla Trial",
        world = "Valhalla",
        description = "Brave the howling blizzards and duel the fiercest warriors of the Einherjar.",
        baseRecommendedPower = 20_000,
        enemyHeroId = "trial_enemy_valhalla",
        enemyName = "Valkyrie Stormbearer",
        enemyTitle = "Chosen of Odin",
        enemyFaction = "Norse / Valhalla",
        baseEnemyHp = 24_000,
        baseEnemyAtk = 3_500,
        baseEnemyDef = 2_900,
        baseGoldReward = 5_000,
        baseXpReward = 3_200,
        baseEventTokens = 40,
        cardRewardId = "c_thor_wrath",
        cardRewardName = "Wrath of Thor",
        cardRewardRarity = CardRarity.LEGENDARY
    )

    val EGYPT_TRIAL = EndgameChallengeStage(
        stageId = "trial_egypt",
        name = "Egypt Trial",
        world = "Egypt",
        description = "Cross the shifting desert sands where ancient pharaonic guardians awaken.",
        baseRecommendedPower = 22_000,
        enemyHeroId = "trial_enemy_egypt",
        enemyName = "Sobek's Dread",
        enemyTitle = "Lord of the Sunken Tombs",
        enemyFaction = "Egyptian / Nile",
        baseEnemyHp = 26_000,
        baseEnemyAtk = 3_700,
        baseEnemyDef = 3_100,
        baseGoldReward = 5_500,
        baseXpReward = 3_500,
        baseEventTokens = 45,
        cardRewardId = "c_anubis_judgment",
        cardRewardName = "Anubis Judgment",
        cardRewardRarity = CardRarity.LEGENDARY
    )

    val UNDERWORLD_TRIAL = EndgameChallengeStage(
        stageId = "trial_underworld",
        name = "Underworld Trial",
        world = "Underworld",
        description = "Delve into the shadowy abyss of Tartarus guarded by relentless souls.",
        baseRecommendedPower = 24_000,
        enemyHeroId = "trial_enemy_underworld",
        enemyName = "Cerberus Spawn",
        enemyTitle = "Gatekeeper of Tartarus",
        enemyFaction = "Underworld / Hades",
        baseEnemyHp = 28_000,
        baseEnemyAtk = 3_900,
        baseEnemyDef = 3_300,
        baseGoldReward = 6_000,
        baseXpReward = 3_800,
        baseEventTokens = 50,
        cardRewardId = "c_hades_grasp",
        cardRewardName = "Grasp of the Dead",
        cardRewardRarity = CardRarity.EPIC
    )

    val CELESTIAL_TRIAL = EndgameChallengeStage(
        stageId = "trial_celestial",
        name = "Celestial Trial",
        world = "Celestial",
        description = "Stand upon the cosmic rim and battle the raw celestial energy of the heavens.",
        baseRecommendedPower = 26_000,
        enemyHeroId = "trial_enemy_celestial",
        enemyName = "Astral Archon",
        enemyTitle = "Voice of the Cosmos",
        enemyFaction = "Primordial / Celestial",
        baseEnemyHp = 30_000,
        baseEnemyAtk = 4_200,
        baseEnemyDef = 3_500,
        baseGoldReward = 7_000,
        baseXpReward = 4_200,
        baseEventTokens = 60,
        cardRewardId = "c_phoenix_rebirth",
        cardRewardName = "Phoenix Rebirth",
        cardRewardRarity = CardRarity.MYTHIC
    )

    val ALL_TRIALS = listOf(
        OLYMPUS_TRIAL,
        VALHALLA_TRIAL,
        EGYPT_TRIAL,
        UNDERWORLD_TRIAL,
        CELESTIAL_TRIAL
    )

    fun findTrial(stageId: String): EndgameChallengeStage? =
        ALL_TRIALS.find { it.stageId == stageId }

    // --- 2. WORLD BOSSES ---
    val KRONOS = WorldBossDefinition(
        bossId = "world_boss_kronos",
        name = "Titan Kronos",
        title = "Lord of Time & Primordial Titan",
        mythology = "Greek / Titans",
        maxHp = 1_000_000L,
        baseAttack = 4_500,
        baseDefense = 3_500,
        phases = listOf(
            WorldBossPhase(
                phaseNumber = 1,
                name = "Phase 1: Titan's Scythe",
                triggerHpPercent = 1.0f,
                attackMultiplier = 1.0f,
                defenseMultiplier = 1.0f,
                abilityName = "Temporal Tear",
                abilityDescription = "Unleashes heavy temporal cleaves that rend space and time.",
                phaseBannerText = "KRONOS AWAKENS FROM TARTARUS"
            ),
            WorldBossPhase(
                phaseNumber = 2,
                name = "Phase 2: Chronos Fury",
                triggerHpPercent = 0.66f,
                attackMultiplier = 1.35f,
                defenseMultiplier = 0.80f,
                abilityName = "Chronos Fury",
                abilityDescription = "Increases damage by +35% while defense drops by 20% in reckless rage.",
                phaseBannerText = "CHRONOS FURY: ENRAGED DAMAGE!"
            ),
            WorldBossPhase(
                phaseNumber = 3,
                name = "Phase 3: Time Collapse",
                triggerHpPercent = 0.33f,
                attackMultiplier = 1.75f,
                defenseMultiplier = 1.20f,
                abilityName = "Time Collapse",
                abilityDescription = "Catastrophic time warp dealing lethal burst damage to unprepared mortals.",
                phaseBannerText = "TIME COLLAPSE: APOCALYPTIC BURST!"
            )
        ),
        startTime = "2026-09-20",
        endTime = "2026-10-15",
        portraitResId = R.drawable.img_enemy_hero,
        lore = "The ancient Titan Sovereign breaks free from Tartarus. All Champions and Alliances must unite to conquer his boundless temporal fury."
    )

    val JORMUNGANDR = WorldBossDefinition(
        bossId = "world_boss_jormungandr",
        name = "Jormungandr",
        title = "The Midgard Serpent",
        mythology = "Norse / Ragnarok",
        maxHp = 1_200_000L,
        baseAttack = 4_200,
        baseDefense = 4_000,
        phases = listOf(
            WorldBossPhase(
                phaseNumber = 1,
                name = "Phase 1: Abyssal Coils",
                triggerHpPercent = 1.0f,
                attackMultiplier = 1.0f,
                defenseMultiplier = 1.0f,
                abilityName = "Venomous Breath",
                abilityDescription = "Coils tighten, spewing noxious venom with each heavy crush.",
                phaseBannerText = "THE WORLD SERPENT UNCOILS"
            ),
            WorldBossPhase(
                phaseNumber = 2,
                name = "Phase 2: Venom Surge",
                triggerHpPercent = 0.66f,
                attackMultiplier = 1.30f,
                defenseMultiplier = 1.10f,
                abilityName = "Venom Surge",
                abilityDescription = "Noxious poison intensifies, restoring scales and dealing toxic damage.",
                phaseBannerText = "VENOM SURGE: TOXIC REGENERATION!"
            ),
            WorldBossPhase(
                phaseNumber = 3,
                name = "Phase 3: Ragnarok Coil",
                triggerHpPercent = 0.33f,
                attackMultiplier = 1.80f,
                defenseMultiplier = 0.90f,
                abilityName = "Ragnarok Coil",
                abilityDescription = "The final apocalyptic strike that shatters land and sea.",
                phaseBannerText = "RAGNAROK COIL: CATACLYSMIC THRASH!"
            )
        ),
        startTime = "2026-10-01",
        endTime = "2026-10-30",
        portraitResId = R.drawable.img_enemy_hero,
        lore = "Encircling the entirety of Midgard, the Great Serpent rises from ocean depths as the horn of Heimdall sounds."
    )

    val ALL_WORLD_BOSSES = listOf(KRONOS, JORMUNGANDR)

    fun getActiveWorldBoss(): WorldBossDefinition = KRONOS

    fun findWorldBoss(bossId: String): WorldBossDefinition? =
        ALL_WORLD_BOSSES.find { it.bossId == bossId }

    // --- 3. RAIDS ---
    val OLYMPUS_RAID = RaidDefinition(
        raidId = "raid_olympus",
        name = "Olympus Raid",
        subtitle = "Assault on the Celestial Citadel",
        mythology = "Greek / Olympus",
        difficulty = EndgameDifficulty.HARD,
        recommendedPower = 19_500,
        stages = listOf(
            RaidStage(
                stageNumber = 1,
                name = "Temple Gate",
                description = "Breach the outer sanctuary protected by blessed stone wardens.",
                enemyName = "Temple Colossus",
                enemyTitle = "Guardian of the Marble Gates",
                enemyHp = 18_000,
                enemyAtk = 2_800,
                enemyDef = 2_600,
                recommendedPower = 18_000,
                goldReward = 3_000,
                eventTokensReward = 20
            ),
            RaidStage(
                stageNumber = 2,
                name = "Titan's Army",
                description = "Fight through the vanguard legion of corrupted titan thralls.",
                enemyName = "Titan Warleader",
                enemyTitle = "Vanguard Commander",
                enemyHp = 22_000,
                enemyAtk = 3_300,
                enemyDef = 2_900,
                recommendedPower = 19_500,
                goldReward = 4_000,
                eventTokensReward = 30
            ),
            RaidStage(
                stageNumber = 3,
                name = "Titan Boss",
                description = "Confront Asterius the Awakened Titan in the Inner Sanctum.",
                enemyName = "Awakened Titan",
                enemyTitle = "Scourge of Olympus",
                enemyHp = 28_000,
                enemyAtk = 3_800,
                enemyDef = 3_200,
                recommendedPower = 21_000,
                goldReward = 6_000,
                eventTokensReward = 50,
                isBossStage = true
            )
        ),
        bossName = "Awakened Titan",
        rewardSummary = "6,000 Gold • 100 Event Tokens • Titan's Wrath Shards"
    )

    val VALHALLA_RAID = RaidDefinition(
        raidId = "raid_valhalla",
        name = "Valhalla Raid",
        subtitle = "March on the Frozen Fjords",
        mythology = "Norse / Frost",
        difficulty = EndgameDifficulty.NIGHTMARE,
        recommendedPower = 22_000,
        stages = listOf(
            RaidStage(
                stageNumber = 1,
                name = "Frozen Gate",
                description = "Shatter the glacial barriers guarded by Rime Giants.",
                enemyName = "Frost Warden",
                enemyTitle = "Giant of the Northern Pass",
                enemyHp = 20_000,
                enemyAtk = 3_200,
                enemyDef = 2_900,
                recommendedPower = 20_000,
                goldReward = 3_500,
                eventTokensReward = 25
            ),
            RaidStage(
                stageNumber = 2,
                name = "Einherjar Legion",
                description = "Clash with the fallen champions who feast and battle eternally.",
                enemyName = "Einherjar Berserker",
                enemyTitle = "Veteran of a Thousand Battles",
                enemyHp = 25_000,
                enemyAtk = 3_600,
                enemyDef = 3_100,
                recommendedPower = 21_500,
                goldReward = 4_500,
                eventTokensReward = 35
            ),
            RaidStage(
                stageNumber = 3,
                name = "Jormungandr Avatar",
                description = "Face the monstrous manifestation of the World Serpent.",
                enemyName = "Avatar of Jormungandr",
                enemyTitle = "Fjord Abomination",
                enemyHp = 32_000,
                enemyAtk = 4_200,
                enemyDef = 3_500,
                recommendedPower = 23_000,
                goldReward = 7_500,
                eventTokensReward = 65,
                isBossStage = true
            )
        ),
        bossName = "Avatar of Jormungandr",
        rewardSummary = "7,500 Gold • 125 Event Tokens • Mythic Frost Shards"
    )

    val ALL_RAIDS = listOf(OLYMPUS_RAID, VALHALLA_RAID)

    fun findRaid(raidId: String): RaidDefinition? =
        ALL_RAIDS.find { it.raidId == raidId }

    // --- 4. EVENT REWARD SHOP ITEMS ---
    val DEFAULT_EVENT_SHOP_ITEMS = listOf(
        EventShopItem(
            itemId = "shop_hercules_shards",
            name = "Hercules Hero Shards x10",
            description = "Empower Hercules with 10 Champion Shards for rank progression.",
            tokenPrice = 50,
            category = "HERO_SHARDS",
            purchaseLimit = 5,
            rewardHeroShards = 10,
            rewardHeroId = HerculesIdentity.HERO_ID
        ),
        EventShopItem(
            itemId = "shop_card_shards_pack",
            name = "Card Shards Pack x25",
            description = "25 Shards for Titan's Wrath to enhance legendary card potency.",
            tokenPrice = 40,
            category = "CARD_SHARDS",
            purchaseLimit = 5,
            rewardCardShards = 25,
            rewardCardId = "c_titans_wrath"
        ),
        EventShopItem(
            itemId = "shop_gold_bounty",
            name = "10,000 Gold Bounty",
            description = "A treasury crate of 10,000 gleaming gold coins.",
            tokenPrice = 30,
            category = "GOLD",
            purchaseLimit = 10,
            rewardGold = 10_000
        ),
        EventShopItem(
            itemId = "shop_exclusive_card",
            name = "Chronos Temporal Strike",
            description = "Exclusive event card: Strike through the timeline to bypass defenses.",
            tokenPrice = 150,
            category = "EXCLUSIVE_CARD",
            purchaseLimit = 1,
            rewardCardId = "c_zeus_thunderstone",
            rewardCardName = "Chronos Temporal Strike"
        ),
        EventShopItem(
            itemId = "shop_aegis_frame",
            name = "Aegis Champion Frame",
            description = "Cosmetic portrait frame adorned with divine golden laurels.",
            tokenPrice = 100,
            category = "COSMETIC",
            purchaseLimit = 1,
            rewardCosmeticId = "frame_aegis_champion"
        )
    )

    // --- 5. WEEKLY OBJECTIVES ---
    fun createDefaultWeeklyObjectives(): List<WeeklyObjective> = listOf(
        WeeklyObjective(
            id = "weekly_win_battles",
            type = WeeklyObjectiveType.WIN_BATTLES,
            title = "Victorious Vanguard",
            description = "Win 10 battles across any game mode.",
            target = 10L,
            goldReward = 8_000,
            eventTokensReward = 50,
            heroShardsReward = 15
        ),
        WeeklyObjective(
            id = "weekly_deal_damage",
            type = WeeklyObjectiveType.DEAL_DAMAGE,
            title = "Wrath of the Gods",
            description = "Deal a total of 100,000 damage in combat.",
            target = 100_000L,
            goldReward = 10_000,
            eventTokensReward = 60,
            heroShardsReward = 20
        ),
        WeeklyObjective(
            id = "weekly_clear_trials",
            type = WeeklyObjectiveType.CLEAR_ENDGAME_STAGES,
            title = "Trials of Immortality",
            description = "Clear 3 Endgame Trial challenge stages.",
            target = 3L,
            goldReward = 9_000,
            eventTokensReward = 55,
            heroShardsReward = 15
        ),
        WeeklyObjective(
            id = "weekly_participate_raids",
            type = WeeklyObjectiveType.PARTICIPATE_RAIDS,
            title = "Alliance Siege",
            description = "Participate in 3 Raid battles.",
            target = 3L,
            goldReward = 10_000,
            eventTokensReward = 65,
            heroShardsReward = 20
        ),
        WeeklyObjective(
            id = "weekly_world_boss_damage",
            type = WeeklyObjectiveType.DEAL_WORLD_BOSS_DAMAGE,
            title = "Titanbane",
            description = "Deal 50,000 damage to the World Boss.",
            target = 50_000L,
            goldReward = 12_000,
            eventTokensReward = 75,
            heroShardsReward = 25
        )
    )

    // --- 6. MOCK ALLIANCE RAID BOARD ---
    fun getMockAllianceRaidBoard(): List<AllianceRaidBoardEntry> = listOf(
        AllianceRaidBoardEntry("Valhalla Ascendant", 20, 20, 18_420_000L, 1),
        AllianceRaidBoardEntry("Titan Slayers", 20, 20, 16_150_000L, 2),
        AllianceRaidBoardEntry("Anubis Harbingers", 19, 20, 14_900_000L, 3),
        AllianceRaidBoardEntry("Aegis Immortal", 20, 20, 14_100_000L, 4),
        AllianceRaidBoardEntry("Odin's Ravens", 19, 20, 13_500_000L, 5),
        AllianceRaidBoardEntry("Sun of Ra", 18, 20, 13_100_000L, 6),
        AllianceRaidBoardEntry("Olympus Guardians", 18, 20, 12_840_500L, 7),
        AllianceRaidBoardEntry("Tartarus Watchers", 17, 20, 11_900_000L, 8),
        AllianceRaidBoardEntry("Spartan Legion", 19, 20, 10_750_000L, 9),
        AllianceRaidBoardEntry("Midgard Wolves", 16, 20, 9_800_000L, 10)
    )

    fun getCurrentWeekId(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val week = calendar.get(Calendar.WEEK_OF_YEAR)
        return String.format(Locale.US, "%04d-W%02d", year, week)
    }
}
