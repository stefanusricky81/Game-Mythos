package com.example.data

import com.example.R

/**
 * Data model for a Campaign Stage encounter (Phase 7B Requirements #1, #3, #6, #8).
 */
data class CampaignStage(
    val stageId: String,
    val worldId: String,
    val stageNumber: Int,
    val name: String,
    val description: String,
    val enemyHeroId: String,
    val enemyName: String,
    val enemyTitle: String,
    val enemyFaction: String,
    val enemyHp: Int,
    val enemyAtk: Int,
    val enemyDef: Int,
    val enemyPortraitResId: Int = R.drawable.img_enemy_hero,
    val recommendedPower: Int,
    val goldReward: Int,
    val xpReward: Int,
    val cardRewardId: String,
    val cardRewardName: String,
    val cardRewardRarity: CardRarity,
    val unlockRequirement: String,
    val isBoss: Boolean = false,
    val firstClearGold: Int = goldReward,
    val firstClearXp: Int = xpReward,
    val firstClearCardId: String = cardRewardId,
    val firstClearCardName: String = cardRewardName,
    val firstClearCardRarity: CardRarity = cardRewardRarity,
    val replayGold: Int = maxOf(100, (goldReward * 0.35).toInt()),
    val replayXp: Int = maxOf(150, (xpReward * 0.35).toInt()),
    val maxTurnsForStarCondition: Int = if (isBoss) 10 else 8,
    val firstClearHeroXp: Int = 800,
    val replayHeroXp: Int = 250,
    val firstClearHeroShards: Int = when (stageNumber) {
        1, 2 -> 10
        3, 4 -> 15
        5 -> 25
        else -> 10
    },
    val replayHeroShards: Int = 0,
    val firstClearHeroShardHeroId: String = HerculesIdentity.HERO_ID
) {
    /**
     * Converts this campaign stage into a BattleEncounterConfig for BattleViewModel.
     */
    fun toEncounterConfig(): BattleEncounterConfig {
        val enemyHero = Hero(
            id = enemyHeroId,
            name = enemyName,
            title = enemyTitle,
            currentHp = enemyHp,
            maxHp = enemyHp,
            baseAttack = enemyAtk,
            baseDefense = enemyDef,
            portraitResId = enemyPortraitResId
        )
        return BattleEncounterConfig(
            stageId = stageId,
            stageNumber = stageNumber,
            encounterName = name,
            enemyHero = enemyHero,
            rewards = BattleRewards(
                gold = goldReward,
                xp = xpReward,
                cardRewardName = cardRewardName,
                cardRewardRarity = cardRewardRarity
            ),
            rewardCardId = cardRewardId,
            isBoss = isBoss,
            maxTurnsForStarCondition = maxTurnsForStarCondition,
            firstClearGold = firstClearGold,
            firstClearXp = firstClearXp,
            firstClearCardId = firstClearCardId,
            firstClearCardName = firstClearCardName,
            firstClearCardRarity = firstClearCardRarity,
            replayGold = replayGold,
            replayXp = replayXp,
            firstClearHeroXp = firstClearHeroXp,
            replayHeroXp = replayHeroXp,
            firstClearHeroShards = firstClearHeroShards,
            replayHeroShards = replayHeroShards,
            firstClearHeroShardHeroId = firstClearHeroShardHeroId
        )
    }
}

/**
 * Data model for a Campaign World containing multiple sequential stages.
 */
data class CampaignWorld(
    val worldId: String,
    val name: String,
    val subtitle: String,
    val description: String,
    val stages: List<CampaignStage>
)

/**
 * Clean encounter configuration interface passed into BattleViewModel.
 * Decouples Campaign progression logic from the combat engine.
 */
data class BattleEncounterConfig(
    val stageId: String? = null,
    val stageNumber: Int = 1,
    val encounterName: String = "Trial of Ares",
    val enemyHero: Hero = Hero.createAres(),
    val rewards: BattleRewards = BattleRewards(),
    val rewardCardId: String = "c_divine_aegis",
    val isBoss: Boolean = false,
    val maxTurnsForStarCondition: Int = 8,
    val firstClearGold: Int = rewards.gold,
    val firstClearXp: Int = rewards.xp,
    val firstClearCardId: String = rewardCardId,
    val firstClearCardName: String = rewards.cardRewardName,
    val firstClearCardRarity: CardRarity = rewards.cardRewardRarity,
    val replayGold: Int = (rewards.gold * 0.35).toInt(),
    val replayXp: Int = (rewards.xp * 0.35).toInt(),
    val firstClearHeroXp: Int = 800,
    val replayHeroXp: Int = 250,
    val firstClearHeroShards: Int = 10,
    val replayHeroShards: Int = 0,
    val firstClearHeroShardHeroId: String = HerculesIdentity.HERO_ID
)

/**
 * Result data model generated after completing a campaign stage battle.
 * Captures star calculations, performance conditions, and rewards awarded.
 */
data class StageVictoryResult(
    val stageId: String,
    val stageName: String,
    val stageNumber: Int,
    val isBoss: Boolean = false,
    val isFirstClear: Boolean,
    val starsEarned: Int,
    val previousStars: Int,
    val bestStars: Int,
    val hpConditionMet: Boolean,
    val turnsConditionMet: Boolean,
    val playerRemainingHp: Int,
    val playerMaxHp: Int,
    val hpPercentage: Int,
    val turnsCount: Int,
    val maxTurnsAllowed: Int,
    val goldAwarded: Int,
    val xpAwarded: Int,
    val cardIdAwarded: String? = null,
    val cardNameAwarded: String? = null,
    val cardRarityAwarded: CardRarity? = null,
    val wasDuplicateCard: Boolean = false,
    val shardsAwardedForDuplicate: Int = 0,
    val heroXpAwarded: Int = 0,
    val heroShardsAwarded: Int = 0,
    val heroShardsHeroId: String? = null,
    val heroUnlocked: String? = null
)

/**
 * Centralized, data-driven Campaign Catalog.
 * Extensible for future worlds without modifying UI components.
 */
object CampaignCatalog {

    val WORLD_1_STAGES: List<CampaignStage> = listOf(
        CampaignStage(
            stageId = "stage_1_1",
            worldId = "world_olympus",
            stageNumber = 1,
            name = "Trial of Ares",
            description = "Ares tests your resolve with Athenian skirmishers at the foot of Mount Olympus.",
            enemyHeroId = "enemy_ares_stage1",
            enemyName = "Ares",
            enemyTitle = "God of War",
            enemyFaction = "Greek / Olympus",
            enemyHp = 8000,
            enemyAtk = 2000,
            enemyDef = 1800,
            enemyPortraitResId = R.drawable.img_enemy_hero,
            recommendedPower = 2000,
            goldReward = 500,
            xpReward = 800,
            cardRewardId = "c_olympian_guard",
            cardRewardName = "Olympian Guard",
            cardRewardRarity = CardRarity.COMMON,
            unlockRequirement = "Unlocked by default",
            firstClearGold = 500,
            firstClearXp = 800,
            firstClearCardId = "c_olympian_guard",
            firstClearCardName = "Olympian Guard",
            firstClearCardRarity = CardRarity.COMMON,
            replayGold = 150,
            replayXp = 250,
            maxTurnsForStarCondition = 8
        ),
        CampaignStage(
            stageId = "stage_1_2",
            worldId = "world_olympus",
            stageNumber = 2,
            name = "Wrath of the Arena",
            description = "Enter the sacred war amphitheater where Spartan phalanxes clash under bronze shields.",
            enemyHeroId = "enemy_ares_stage2",
            enemyName = "Ares",
            enemyTitle = "Commander of the Phalanx",
            enemyFaction = "Greek / Olympus",
            enemyHp = 9500,
            enemyAtk = 2200,
            enemyDef = 2000,
            enemyPortraitResId = R.drawable.img_enemy_hero,
            recommendedPower = 2500,
            goldReward = 750,
            xpReward = 1200,
            cardRewardId = "c_power_strike",
            cardRewardName = "Power Strike",
            cardRewardRarity = CardRarity.COMMON,
            unlockRequirement = "Complete Stage 1: Trial of Ares",
            firstClearGold = 750,
            firstClearXp = 1200,
            firstClearCardId = "c_power_strike",
            firstClearCardName = "Power Strike",
            firstClearCardRarity = CardRarity.COMMON,
            replayGold = 220,
            replayXp = 350,
            maxTurnsForStarCondition = 8
        ),
        CampaignStage(
            stageId = "stage_1_3",
            worldId = "world_olympus",
            stageNumber = 3,
            name = "Champion's Trial",
            description = "Battle seasoned war-priests who channel the furious battle-rage of Ares.",
            enemyHeroId = "enemy_ares_stage3",
            enemyName = "Ares",
            enemyTitle = "Bringer of Strife",
            enemyFaction = "Greek / Olympus",
            enemyHp = 11000,
            enemyAtk = 2500,
            enemyDef = 2300,
            enemyPortraitResId = R.drawable.img_enemy_hero,
            recommendedPower = 3000,
            goldReward = 1000,
            xpReward = 1600,
            cardRewardId = "c_heroic_rage",
            cardRewardName = "Heroic Rage",
            cardRewardRarity = CardRarity.RARE,
            unlockRequirement = "Complete Stage 2: Wrath of the Arena",
            firstClearGold = 1000,
            firstClearXp = 1600,
            firstClearCardId = "c_heroic_rage",
            firstClearCardName = "Heroic Rage",
            firstClearCardRarity = CardRarity.RARE,
            replayGold = 300,
            replayXp = 500,
            maxTurnsForStarCondition = 8
        ),
        CampaignStage(
            stageId = "stage_1_4",
            worldId = "world_olympus",
            stageNumber = 4,
            name = "Guardian of Olympus",
            description = "Ascend the celestial gates guarded by Ares' elite celestial champion.",
            enemyHeroId = "enemy_ares_stage4",
            enemyName = "Guardian of Olympus",
            enemyTitle = "Iron Sentinel of the Gods",
            enemyFaction = "Greek / Olympus",
            enemyHp = 12500,
            enemyAtk = 2700,
            enemyDef = 2600,
            enemyPortraitResId = R.drawable.img_enemy_hero,
            recommendedPower = 3500,
            goldReward = 1500,
            xpReward = 2200,
            cardRewardId = "c_titans_wrath",
            cardRewardName = "Titan's Wrath",
            cardRewardRarity = CardRarity.EPIC,
            unlockRequirement = "Complete Stage 3: Champion's Trial",
            firstClearGold = 1500,
            firstClearXp = 2200,
            firstClearCardId = "c_titans_wrath",
            firstClearCardName = "Titan's Wrath",
            firstClearCardRarity = CardRarity.EPIC,
            replayGold = 450,
            replayXp = 700,
            maxTurnsForStarCondition = 8
        ),
        CampaignStage(
            stageId = "stage_1_5",
            worldId = "world_olympus",
            stageNumber = 5,
            name = "Wrath of Olympus",
            description = "Final confrontation atop the divine peak against the fully unleashed God of War.",
            enemyHeroId = "enemy_ares_stage5",
            enemyName = "Ares",
            enemyTitle = "Warlord of Olympus",
            enemyFaction = "Greek / Olympus",
            enemyHp = 14000,
            enemyAtk = 3000,
            enemyDef = 2800,
            enemyPortraitResId = R.drawable.img_enemy_hero,
            recommendedPower = 4000,
            goldReward = 2500,
            xpReward = 3500,
            cardRewardId = "c_divine_aegis",
            cardRewardName = "Divine Aegis of Olympus",
            cardRewardRarity = CardRarity.EPIC,
            unlockRequirement = "Complete Stage 4: Guardian of Olympus",
            isBoss = true,
            firstClearGold = 2500,
            firstClearXp = 3500,
            firstClearCardId = "c_divine_aegis",
            firstClearCardName = "Divine Aegis of Olympus",
            firstClearCardRarity = CardRarity.EPIC,
            replayGold = 750,
            replayXp = 1000,
            maxTurnsForStarCondition = 10
        )
    )

    val WORLD_1 = CampaignWorld(
        worldId = "world_olympus",
        name = "AEGEAN / OLYMPUS",
        subtitle = "The Path to Mount Olympus",
        description = "Climb the celestial heights of Olympus, overcoming the trials of the God of War to prove your heroic supremacy.",
        stages = WORLD_1_STAGES
    )

    val ALL_WORLDS: List<CampaignWorld> = listOf(WORLD_1)

    fun findStage(stageId: String): CampaignStage? {
        return ALL_WORLDS.flatMap { it.stages }.find { it.stageId == stageId }
    }

    fun findStageByNumber(worldId: String, stageNumber: Int): CampaignStage? {
        return ALL_WORLDS.find { it.worldId == worldId }?.stages?.find { it.stageNumber == stageNumber }
    }

    fun getDefaultWorld(): CampaignWorld = WORLD_1
}
