package com.example.data

import com.example.R

/**
 * Boss Phase Model containing distinct mechanical attributes for multi-phase boss encounters.
 */
data class BossPhase(
    val phaseNumber: Int,
    val name: String,
    val healthThresholdPercent: Float, // Triggered when boss HP falls to or below this %
    val attackBonusPercent: Int = 0,
    val defenseBonusPercent: Int = 0,
    val bannerText: String,
    val phaseMechanicDescription: String,
    val specialAbilityName: String,
    val specialAbilityDamage: Int = 0
)

/**
 * Full Boss Definition for campaign climaxes and raid trials.
 */
data class BossDefinition(
    val bossId: String,
    val name: String,
    val title: String,
    val faction: String,
    val baseHp: Int,
    val baseAttack: Int,
    val baseDefense: Int,
    val phases: List<BossPhase>,
    val firstClearGold: Int,
    val firstClearXp: Int,
    val cardRewardId: String,
    val cardRewardName: String,
    val cardRewardRarity: CardRarity,
    val portraitResId: Int = R.drawable.img_enemy_hero,
    val behaviorType: EnemyBehaviorType = EnemyBehaviorType.AGGRESSIVE_MELEE,
    val phase2AbilityDescription: String = "Unleashes second-phase mythic assault!"
) {
    fun toBattleHero(): Hero {
        return Hero(
            id = bossId,
            name = name,
            title = title,
            currentHp = baseHp,
            maxHp = baseHp,
            baseAttack = baseAttack,
            baseDefense = baseDefense,
            portraitResId = portraitResId
        )
    }
}

/**
 * Centralized Boss Catalog (Phase 9 Requirement #7, #13).
 */
object BossCatalog {

    val MINOTAUR_KING = BossDefinition(
        bossId = "boss_minotaur_king",
        name = "Asterius, Minotaur King",
        title = "Sovereign of the Labyrinth",
        faction = "Greek / Crete",
        baseHp = 16000,
        baseAttack = 2800,
        baseDefense = 2400,
        phases = listOf(
            BossPhase(
                phaseNumber = 1,
                name = "Phase 1: Iron Horns",
                healthThresholdPercent = 1.0f,
                attackBonusPercent = 0,
                bannerText = "MINOTAUR KING RISES",
                phaseMechanicDescription = "Strikes with heavy cleaves and charges.",
                specialAbilityName = "Bull Rush",
                specialAbilityDamage = 2200
            ),
            BossPhase(
                phaseNumber = 2,
                name = "Phase 2: Enraged Carnage",
                healthThresholdPercent = 0.50f,
                attackBonusPercent = 40,
                bannerText = "ENRAGED: +40% ATK POWER!",
                phaseMechanicDescription = "Asterius enters a berserk frenzy, gaining massive +40% Attack!",
                specialAbilityName = "Cataclysmic Gore",
                specialAbilityDamage = 3500
            )
        ),
        firstClearGold = 3500,
        firstClearXp = 4500,
        cardRewardId = "c_titans_wrath",
        cardRewardName = "Titan's Wrath",
        cardRewardRarity = CardRarity.LEGENDARY
    )

    val LERNAEAN_HYDRA = BossDefinition(
        bossId = "boss_hydra",
        name = "Hydra of Lerna",
        title = "Nine-Headed Primordial Brood",
        faction = "Greek / Swamps",
        baseHp = 18000,
        baseAttack = 2500,
        baseDefense = 2600,
        phases = listOf(
            BossPhase(
                phaseNumber = 1,
                name = "Phase 1: Venom Serpent",
                healthThresholdPercent = 1.0f,
                attackBonusPercent = 0,
                bannerText = "HYDRA OF LERNA DESCENDS",
                phaseMechanicDescription = "Spits venom and inflicts corrosive poison.",
                specialAbilityName = "Corrosive Acid",
                specialAbilityDamage = 1800
            ),
            BossPhase(
                phaseNumber = 2,
                name = "Phase 2: Multi-Head Regrowth",
                healthThresholdPercent = 0.50f,
                attackBonusPercent = 25,
                defenseBonusPercent = 15,
                bannerText = "MULTI-HEAD REGROWTH ACTIVATED!",
                phaseMechanicDescription = "Hydra sprouts four new heads, gaining +1,200 Regeneration/turn and attacking twice!",
                specialAbilityName = "Nine-Fanged Barrage",
                specialAbilityDamage = 3200
            )
        ),
        firstClearGold = 4000,
        firstClearXp = 5000,
        cardRewardId = "c_hydra_blade",
        cardRewardName = "Hydra Venom Blade",
        cardRewardRarity = CardRarity.RARE
    )

    val MEDUSA_QUEEN = BossDefinition(
        bossId = "boss_medusa_queen",
        name = "Medusa, Gorgon Queen",
        title = "Matriarch of the Stone Den",
        faction = "Greek / Underworld",
        baseHp = 15000,
        baseAttack = 2900,
        baseDefense = 2300,
        phases = listOf(
            BossPhase(
                phaseNumber = 1,
                name = "Phase 1: Serpent Queen",
                healthThresholdPercent = 1.0f,
                attackBonusPercent = 0,
                bannerText = "MEDUSA QUEEN ENTERS THE ARENA",
                phaseMechanicDescription = "Afflicts the hero with Weaken and viper poison.",
                specialAbilityName = "Viper Strike",
                specialAbilityDamage = 2000
            ),
            BossPhase(
                phaseNumber = 2,
                name = "Phase 2: Eyes of Eternity",
                healthThresholdPercent = 0.50f,
                attackBonusPercent = 20,
                bannerText = "PETRIFYING GAZE UNLEASHED!",
                phaseMechanicDescription = "Medusa exposes her unshielded gaze, inflicting Stun and permanent +30% Vulnerability!",
                specialAbilityName = "Absolute Petrification",
                specialAbilityDamage = 3400
            )
        ),
        firstClearGold = 4500,
        firstClearXp = 6000,
        cardRewardId = "c_medusa_gaze",
        cardRewardName = "Gaze of the Gorgon",
        cardRewardRarity = CardRarity.EPIC
    )

    val ALL_BOSSES: List<BossDefinition> = listOf(
        MINOTAUR_KING,
        LERNAEAN_HYDRA,
        MEDUSA_QUEEN
    )

    private val bossMap: Map<String, BossDefinition> = ALL_BOSSES.associateBy { it.bossId }

    fun findBoss(bossId: String): BossDefinition? = bossMap[bossId]
}
