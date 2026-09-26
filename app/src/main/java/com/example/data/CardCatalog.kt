package com.example.data

import com.example.R

/**
 * Data model defining level progression steps for a card (Requirement #7).
 */
data class CardProgressionStep(
    val level: Int,
    val effect: CardEffect,
    val effectDescription: String,
    val goldCostToNext: Int = 0,
    val shardCostToNext: Int = 0
)

/**
 * Master Card Definition containing metadata, base stats, and progression tables.
 */
data class CardDefinition(
    val id: String,
    val name: String,
    val cost: Int,
    val type: CardType,
    val rarity: CardRarity,
    val loreQuote: String,
    val iconKey: String = "strike",
    val artworkResId: Int? = null,
    val faction: String = "Olympus",
    val shardId: String = id,
    val maxLevel: Int = 5,
    val progression: Map<Int, CardProgressionStep>
) {
    fun getCard(level: Int = 1): Card {
        val clampedLevel = level.coerceIn(1, maxLevel)
        val step = progression[clampedLevel] ?: progression[1] ?: CardProgressionStep(1, CardEffect(), "")
        return Card(
            id = id,
            name = name,
            cost = cost,
            type = type,
            rarity = rarity,
            effectDescription = step.effectDescription,
            loreQuote = loreQuote,
            effect = step.effect,
            iconKey = iconKey,
            artworkResId = artworkResId,
            faction = faction,
            level = clampedLevel,
            maxLevel = maxLevel
        )
    }

    fun getUpgradeCost(currentLevel: Int): Pair<Int, Int>? {
        if (currentLevel >= maxLevel) return null
        val configured = CardUpgradeConfig.getUpgradeCost(currentLevel)
        if (configured != null) {
            return Pair(configured.goldCost, configured.shardCost)
        }
        val step = progression[currentLevel] ?: return null
        return Pair(step.goldCostToNext, step.shardCostToNext)
    }
}

/**
 * Player inventory instance of a card tracking progression and unlock state.
 * (Phase 6A Requirement #4)
 */
data class CardInstance(
    val cardId: String,
    val quantity: Int,
    val level: Int,
    val shards: Int,
    val isUnlocked: Boolean,
    val definition: CardDefinition
)

/**
 * Centralized Card Upgrade Scaling Configuration (Phase 6A Requirement #8).
 * Configurable progression costs across card levels:
 * Level 1 -> 2: 5,000 Gold + 20 Shards
 * Level 2 -> 3: 7,500 Gold + 30 Shards
 * Level 3 -> 4: 10,000 Gold + 40 Shards
 * Level 4 -> 5: 15,000 Gold + 50 Shards
 */
data class CardUpgradeCost(
    val fromLevel: Int,
    val goldCost: Int,
    val shardCost: Int
)

object CardUpgradeConfig {
    val PROGRESSION: Map<Int, CardUpgradeCost> = mapOf(
        1 to CardUpgradeCost(fromLevel = 1, goldCost = 5_000, shardCost = 20),
        2 to CardUpgradeCost(fromLevel = 2, goldCost = 7_500, shardCost = 30),
        3 to CardUpgradeCost(fromLevel = 3, goldCost = 10_000, shardCost = 40),
        4 to CardUpgradeCost(fromLevel = 4, goldCost = 15_000, shardCost = 50)
    )

    fun getUpgradeCost(currentLevel: Int): CardUpgradeCost? = PROGRESSION[currentLevel]
}

/**
 * Centralized Card Catalog (Requirements #2, #7, #13).
 * Single source of truth for all MYTHOS cards, their effects, and progression.
 */
object CardCatalog {

    val ALL_CARDS: List<CardDefinition> = listOf(
        // 1. Olympian Guard (Common Defense)
        CardDefinition(
            id = "c_olympian_guard",
            name = "Olympian Guard",
            cost = 2,
            type = CardType.DEFENSE,
            rarity = CardRarity.COMMON,
            loreQuote = "Protected by the bronze aegis of ancient champions.",
            iconKey = "shield",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(shield = 1200), "Gain 1,200 Shield.", goldCostToNext = 1000, shardCostToNext = 15),
                2 to CardProgressionStep(2, CardEffect(shield = 1350), "Gain 1,350 Shield.", goldCostToNext = 2500, shardCostToNext = 30),
                3 to CardProgressionStep(3, CardEffect(shield = 1500), "Gain 1,500 Shield.", goldCostToNext = 5000, shardCostToNext = 50),
                4 to CardProgressionStep(4, CardEffect(shield = 1680), "Gain 1,680 Shield.", goldCostToNext = 10000, shardCostToNext = 80),
                5 to CardProgressionStep(5, CardEffect(shield = 1900), "Gain 1,900 Shield.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 2. Titan's Wrath (Legendary Attack)
        CardDefinition(
            id = "c_titans_wrath",
            name = "Titan's Wrath",
            cost = 5,
            type = CardType.ATTACK,
            rarity = CardRarity.LEGENDARY,
            loreQuote = "Strength that shattered the foundations of Mount Othrys.",
            iconKey = "crush",
            artworkResId = HerculesIdentity.assets.cardArt.resolveResId(),
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(damage = 3600), "Deal 3,600 physical damage.", goldCostToNext = 5000, shardCostToNext = 20),
                2 to CardProgressionStep(2, CardEffect(damage = 3850), "Deal 3,850 physical damage.", goldCostToNext = 10000, shardCostToNext = 50),
                3 to CardProgressionStep(3, CardEffect(damage = 4100), "Deal 4,100 physical damage.", goldCostToNext = 20000, shardCostToNext = 80),
                4 to CardProgressionStep(4, CardEffect(damage = 4350), "Deal 4,350 physical damage.", goldCostToNext = 35000, shardCostToNext = 120),
                5 to CardProgressionStep(5, CardEffect(damage = 4700), "Deal 4,700 physical damage.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 3. Spartan Phalanx (Epic Summon)
        CardDefinition(
            id = "c_spartan_phalanx",
            name = "Spartan Phalanx",
            cost = 4,
            type = CardType.SUMMON,
            rarity = CardRarity.EPIC,
            loreQuote = "An unbreakable line of warrior brothers stood shoulder-to-shoulder.",
            iconKey = "spears",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(summonShield = 2000, summonCounterDamage = 800), "Summon Phalanx: Grants 2,000 Shield and 800 retaliation.", goldCostToNext = 3000, shardCostToNext = 16),
                2 to CardProgressionStep(2, CardEffect(summonShield = 2250, summonCounterDamage = 950), "Summon Phalanx: Grants 2,250 Shield and 950 retaliation.", goldCostToNext = 6000, shardCostToNext = 35),
                3 to CardProgressionStep(3, CardEffect(summonShield = 2500, summonCounterDamage = 1100), "Summon Phalanx: Grants 2,500 Shield and 1,100 retaliation.", goldCostToNext = 12000, shardCostToNext = 60),
                4 to CardProgressionStep(4, CardEffect(summonShield = 2800, summonCounterDamage = 1300), "Summon Phalanx: Grants 2,800 Shield and 1,300 retaliation.", goldCostToNext = 22000, shardCostToNext = 90),
                5 to CardProgressionStep(5, CardEffect(summonShield = 3200, summonCounterDamage = 1550), "Summon Phalanx: Grants 3,200 Shield and 1,550 retaliation.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 4. Nemean Lion Hide (Rare Relic)
        CardDefinition(
            id = "c_nemean_hide",
            name = "Nemean Lion Hide",
            cost = 3,
            type = CardType.RELIC,
            rarity = CardRarity.RARE,
            loreQuote = "Impervious pelt stripped from the golden beast of Nemea.",
            iconKey = "armor",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(shield = 800, damageReductionPercent = 15), "Gain 800 Shield and reduce incoming damage by 15%.", goldCostToNext = 2000, shardCostToNext = 12),
                2 to CardProgressionStep(2, CardEffect(shield = 950, damageReductionPercent = 17), "Gain 950 Shield and reduce incoming damage by 17%.", goldCostToNext = 4000, shardCostToNext = 25),
                3 to CardProgressionStep(3, CardEffect(shield = 1100, damageReductionPercent = 20), "Gain 1,100 Shield and reduce incoming damage by 20%.", goldCostToNext = 8000, shardCostToNext = 45),
                4 to CardProgressionStep(4, CardEffect(shield = 1300, damageReductionPercent = 22), "Gain 1,300 Shield and reduce incoming damage by 22%.", goldCostToNext = 15000, shardCostToNext = 70),
                5 to CardProgressionStep(5, CardEffect(shield = 1550, damageReductionPercent = 25), "Gain 1,550 Shield and reduce incoming damage by 25%.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 5. Heroic Rage (Uncommon Spell)
        CardDefinition(
            id = "c_heroic_rage",
            name = "Heroic Rage",
            cost = 3,
            type = CardType.SPELL,
            rarity = CardRarity.UNCOMMON,
            loreQuote = "The blood of Zeus surges with unrelenting fury.",
            iconKey = "fire",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(attackBuffPercent = 20, attackBuffTurns = 1, mythPowerGain = 20), "Gain +20% Attack for 1 turn and +20 Myth Power.", goldCostToNext = 1500, shardCostToNext = 10),
                2 to CardProgressionStep(2, CardEffect(attackBuffPercent = 24, attackBuffTurns = 1, mythPowerGain = 22), "Gain +24% Attack for 1 turn and +22 Myth Power.", goldCostToNext = 3000, shardCostToNext = 20),
                3 to CardProgressionStep(3, CardEffect(attackBuffPercent = 28, attackBuffTurns = 1, mythPowerGain = 25), "Gain +28% Attack for 1 turn and +25 Myth Power.", goldCostToNext = 6000, shardCostToNext = 35),
                4 to CardProgressionStep(4, CardEffect(attackBuffPercent = 32, attackBuffTurns = 1, mythPowerGain = 28), "Gain +32% Attack for 1 turn and +28 Myth Power.", goldCostToNext = 12000, shardCostToNext = 55),
                5 to CardProgressionStep(5, CardEffect(attackBuffPercent = 36, attackBuffTurns = 1, mythPowerGain = 32), "Gain +36% Attack for 1 turn and +32 Myth Power.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 6. Divine Challenge (Epic Spell)
        CardDefinition(
            id = "c_divine_challenge",
            name = "Divine Challenge",
            cost = 4,
            type = CardType.SPELL,
            rarity = CardRarity.EPIC,
            loreQuote = "Even the immortal gods cannot withstand direct defiance.",
            iconKey = "lightning",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(damage = 2500, bonusDamageAgainstShield = 500), "Deal 2,500 damage (+500 bonus against active Shield).", goldCostToNext = 3000, shardCostToNext = 16),
                2 to CardProgressionStep(2, CardEffect(damage = 2750, bonusDamageAgainstShield = 600), "Deal 2,750 damage (+600 bonus against active Shield).", goldCostToNext = 6000, shardCostToNext = 35),
                3 to CardProgressionStep(3, CardEffect(damage = 3000, bonusDamageAgainstShield = 700), "Deal 3,000 damage (+700 bonus against active Shield).", goldCostToNext = 12000, shardCostToNext = 60),
                4 to CardProgressionStep(4, CardEffect(damage = 3300, bonusDamageAgainstShield = 850), "Deal 3,300 damage (+850 bonus against active Shield).", goldCostToNext = 22000, shardCostToNext = 90),
                5 to CardProgressionStep(5, CardEffect(damage = 3700, bonusDamageAgainstShield = 1000), "Deal 3,700 damage (+1,000 bonus against active Shield).", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 7. Hydra Venom Blade (Rare Attack)
        CardDefinition(
            id = "c_hydra_blade",
            name = "Hydra Venom Blade",
            cost = 3,
            type = CardType.ATTACK,
            rarity = CardRarity.RARE,
            loreQuote = "Coated in the corrosive bile of the multi-headed serpent.",
            iconKey = "poison",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(damage = 1800, poisonDamagePerTurn = 400, poisonTurns = 2), "Deal 1,800 damage and inflict Poison (400/t for 2t).", goldCostToNext = 2000, shardCostToNext = 12),
                2 to CardProgressionStep(2, CardEffect(damage = 2000, poisonDamagePerTurn = 450, poisonTurns = 2), "Deal 2,000 damage and inflict Poison (450/t for 2t).", goldCostToNext = 4000, shardCostToNext = 25),
                3 to CardProgressionStep(3, CardEffect(damage = 2200, poisonDamagePerTurn = 500, poisonTurns = 2), "Deal 2,200 damage and inflict Poison (500/t for 2t).", goldCostToNext = 8000, shardCostToNext = 45),
                4 to CardProgressionStep(4, CardEffect(damage = 2450, poisonDamagePerTurn = 570, poisonTurns = 2), "Deal 2,450 damage and inflict Poison (570/t for 2t).", goldCostToNext = 15000, shardCostToNext = 70),
                5 to CardProgressionStep(5, CardEffect(damage = 2750, poisonDamagePerTurn = 650, poisonTurns = 2), "Deal 2,750 damage and inflict Poison (650/t for 2t).", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 8. Nectar of the Gods (Rare Spell)
        CardDefinition(
            id = "c_nectar_gods",
            name = "Nectar of the Gods",
            cost = 2,
            type = CardType.SPELL,
            rarity = CardRarity.RARE,
            loreQuote = "Golden ambrosia brewed atop Olympus, granting renewed vigor.",
            iconKey = "chalice",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(healAmount = 1500), "Restore 1,500 HP and cleanse negative effects.", goldCostToNext = 2000, shardCostToNext = 12),
                2 to CardProgressionStep(2, CardEffect(healAmount = 1750), "Restore 1,750 HP and cleanse negative effects.", goldCostToNext = 4000, shardCostToNext = 25),
                3 to CardProgressionStep(3, CardEffect(healAmount = 2000), "Restore 2,000 HP and cleanse negative effects.", goldCostToNext = 8000, shardCostToNext = 45),
                4 to CardProgressionStep(4, CardEffect(healAmount = 2300), "Restore 2,300 HP and cleanse negative effects.", goldCostToNext = 15000, shardCostToNext = 70),
                5 to CardProgressionStep(5, CardEffect(healAmount = 2700), "Restore 2,700 HP and cleanse negative effects.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 9. Zeus's Thunderstone (Mythic Relic)
        CardDefinition(
            id = "c_zeus_thunderstone",
            name = "Zeus's Thunderstone",
            cost = 4,
            type = CardType.RELIC,
            rarity = CardRarity.MYTHIC,
            loreQuote = "A shard crystallized from the Father of Gods' sacred bolts.",
            iconKey = "thunder",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(turnStartLightningDamage = 1000, turnStartMythPowerGain = 10), "Relic: Strikes for 1,000 lightning each turn and grants +10 MP.", goldCostToNext = 8000, shardCostToNext = 30),
                2 to CardProgressionStep(2, CardEffect(turnStartLightningDamage = 1200, turnStartMythPowerGain = 12), "Relic: Strikes for 1,200 lightning each turn and grants +12 MP.", goldCostToNext = 16000, shardCostToNext = 60),
                3 to CardProgressionStep(3, CardEffect(turnStartLightningDamage = 1450, turnStartMythPowerGain = 14), "Relic: Strikes for 1,450 lightning each turn and grants +14 MP.", goldCostToNext = 30000, shardCostToNext = 100),
                4 to CardProgressionStep(4, CardEffect(turnStartLightningDamage = 1750, turnStartMythPowerGain = 16), "Relic: Strikes for 1,750 lightning each turn and grants +16 MP.", goldCostToNext = 50000, shardCostToNext = 150),
                5 to CardProgressionStep(5, CardEffect(turnStartLightningDamage = 2100, turnStartMythPowerGain = 20), "Relic: Strikes for 2,100 lightning each turn and grants +20 MP.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 10. Divine Aegis of Olympus (Epic Defense)
        CardDefinition(
            id = "c_divine_aegis",
            name = "Divine Aegis of Olympus",
            cost = 3,
            type = CardType.DEFENSE,
            rarity = CardRarity.EPIC,
            loreQuote = "Forged by Hephaestus to turn aside celestial fury.",
            iconKey = "shield",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(shield = 1800, damageReductionPercent = 10), "Gain 1,800 Shield and 10% damage reduction.", goldCostToNext = 3000, shardCostToNext = 16),
                2 to CardProgressionStep(2, CardEffect(shield = 2050, damageReductionPercent = 12), "Gain 2,050 Shield and 12% damage reduction.", goldCostToNext = 6000, shardCostToNext = 35),
                3 to CardProgressionStep(3, CardEffect(shield = 2300, damageReductionPercent = 15), "Gain 2,300 Shield and 15% damage reduction.", goldCostToNext = 12000, shardCostToNext = 60),
                4 to CardProgressionStep(4, CardEffect(shield = 2600, damageReductionPercent = 17), "Gain 2,600 Shield and 17% damage reduction.", goldCostToNext = 22000, shardCostToNext = 90),
                5 to CardProgressionStep(5, CardEffect(shield = 3000, damageReductionPercent = 20), "Gain 3,000 Shield and 20% damage reduction.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 11. Power Strike (Common Attack)
        CardDefinition(
            id = "c_power_strike",
            name = "Power Strike",
            cost = 2,
            type = CardType.ATTACK,
            rarity = CardRarity.COMMON,
            loreQuote = "A single blow forged through celestial labors.",
            iconKey = "strike",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(damage = 1500), "Deal 1,500 physical damage to the enemy.", goldCostToNext = 1000, shardCostToNext = 15),
                2 to CardProgressionStep(2, CardEffect(damage = 1700), "Deal 1,700 physical damage to the enemy.", goldCostToNext = 2500, shardCostToNext = 30),
                3 to CardProgressionStep(3, CardEffect(damage = 1900), "Deal 1,900 physical damage to the enemy.", goldCostToNext = 5000, shardCostToNext = 50),
                4 to CardProgressionStep(4, CardEffect(damage = 2150), "Deal 2,150 physical damage to the enemy.", goldCostToNext = 10000, shardCostToNext = 80),
                5 to CardProgressionStep(5, CardEffect(damage = 2450), "Deal 2,450 physical damage to the enemy.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 12. Celestial Volley (Common Attack)
        CardDefinition(
            id = "c_celestial_arrow",
            name = "Celestial Volley",
            cost = 2,
            type = CardType.ATTACK,
            rarity = CardRarity.COMMON,
            loreQuote = "Golden arrows descending from the stars.",
            iconKey = "spears",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(damage = 1350), "Deal 1,350 piercing damage.", goldCostToNext = 1000, shardCostToNext = 15),
                2 to CardProgressionStep(2, CardEffect(damage = 1520), "Deal 1,520 piercing damage.", goldCostToNext = 2500, shardCostToNext = 30),
                3 to CardProgressionStep(3, CardEffect(damage = 1700), "Deal 1,700 piercing damage.", goldCostToNext = 5000, shardCostToNext = 50),
                4 to CardProgressionStep(4, CardEffect(damage = 1920), "Deal 1,920 piercing damage.", goldCostToNext = 10000, shardCostToNext = 80),
                5 to CardProgressionStep(5, CardEffect(damage = 2200), "Deal 2,200 piercing damage.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 13. Athena's Aegis (Rare Defense)
        CardDefinition(
            id = "c_athena_blessing",
            name = "Athena's Aegis",
            cost = 3,
            type = CardType.DEFENSE,
            rarity = CardRarity.RARE,
            loreQuote = "Wisdom anticipates every strike before it lands.",
            iconKey = "shield",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(shield = 1400, mythPowerGain = 10), "Gain 1,400 Shield and +10 Myth Power.", goldCostToNext = 2000, shardCostToNext = 12),
                2 to CardProgressionStep(2, CardEffect(shield = 1600, mythPowerGain = 12), "Gain 1,600 Shield and +12 Myth Power.", goldCostToNext = 4000, shardCostToNext = 25),
                3 to CardProgressionStep(3, CardEffect(shield = 1800, mythPowerGain = 14), "Gain 1,800 Shield and +14 Myth Power.", goldCostToNext = 8000, shardCostToNext = 45),
                4 to CardProgressionStep(4, CardEffect(shield = 2050, mythPowerGain = 16), "Gain 2,050 Shield and +16 Myth Power.", goldCostToNext = 15000, shardCostToNext = 70),
                5 to CardProgressionStep(5, CardEffect(shield = 2350, mythPowerGain = 20), "Gain 2,350 Shield and +20 Myth Power.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 14. Cyclops Hammer (Uncommon Attack)
        CardDefinition(
            id = "c_cyclops_hammer",
            name = "Cyclops Hammer",
            cost = 4,
            type = CardType.ATTACK,
            rarity = CardRarity.UNCOMMON,
            loreQuote = "Brute force that sunders shields.",
            iconKey = "crush",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(damage = 2600, bonusDamageAgainstShield = 400), "Deal 2,600 damage (+400 vs Shield).", goldCostToNext = 1500, shardCostToNext = 10),
                2 to CardProgressionStep(2, CardEffect(damage = 2850, bonusDamageAgainstShield = 500), "Deal 2,850 damage (+500 vs Shield).", goldCostToNext = 3000, shardCostToNext = 20),
                3 to CardProgressionStep(3, CardEffect(damage = 3100, bonusDamageAgainstShield = 600), "Deal 3,100 damage (+600 vs Shield).", goldCostToNext = 6000, shardCostToNext = 35),
                4 to CardProgressionStep(4, CardEffect(damage = 3400, bonusDamageAgainstShield = 750), "Deal 3,400 damage (+750 vs Shield).", goldCostToNext = 12000, shardCostToNext = 55),
                5 to CardProgressionStep(5, CardEffect(damage = 3800, bonusDamageAgainstShield = 900), "Deal 3,800 damage (+900 vs Shield).", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 15. Ares's Retribution (Uncommon Trap)
        CardDefinition(
            id = "c_ares_retribution",
            name = "Ares's Retribution",
            cost = 3,
            type = CardType.TRAP,
            rarity = CardRarity.UNCOMMON,
            loreQuote = "A violent trap set for overconfident adversaries.",
            iconKey = "spears",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(summonCounterDamage = 1200, shield = 500), "Trap: Counter-strikes for 1,200 damage and gain 500 Shield.", goldCostToNext = 1500, shardCostToNext = 10),
                2 to CardProgressionStep(2, CardEffect(summonCounterDamage = 1350, shield = 600), "Trap: Counter-strikes for 1,350 damage and gain 600 Shield.", goldCostToNext = 3000, shardCostToNext = 20),
                3 to CardProgressionStep(3, CardEffect(summonCounterDamage = 1500, shield = 700), "Trap: Counter-strikes for 1,500 damage and gain 700 Shield.", goldCostToNext = 6000, shardCostToNext = 35),
                4 to CardProgressionStep(4, CardEffect(summonCounterDamage = 1700, shield = 850), "Trap: Counter-strikes for 1,700 damage and gain 850 Shield.", goldCostToNext = 12000, shardCostToNext = 55),
                5 to CardProgressionStep(5, CardEffect(summonCounterDamage = 1950, shield = 1000), "Trap: Counter-strikes for 1,950 damage and gain 1,000 Shield.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 16. Ambrosia Draught (Common Spell)
        CardDefinition(
            id = "c_ambrosia_draft",
            name = "Ambrosia Draught",
            cost = 1,
            type = CardType.SPELL,
            rarity = CardRarity.COMMON,
            loreQuote = "A light sip of divine youth.",
            iconKey = "chalice",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(healAmount = 800, mythPowerGain = 5), "Restore 800 HP and gain +5 Myth Power.", goldCostToNext = 1000, shardCostToNext = 15),
                2 to CardProgressionStep(2, CardEffect(healAmount = 950, mythPowerGain = 6), "Restore 950 HP and gain +6 Myth Power.", goldCostToNext = 2500, shardCostToNext = 30),
                3 to CardProgressionStep(3, CardEffect(healAmount = 1100, mythPowerGain = 7), "Restore 1,100 HP and gain +7 Myth Power.", goldCostToNext = 5000, shardCostToNext = 50),
                4 to CardProgressionStep(4, CardEffect(healAmount = 1300, mythPowerGain = 8), "Restore 1,300 HP and gain +8 Myth Power.", goldCostToNext = 10000, shardCostToNext = 80),
                5 to CardProgressionStep(5, CardEffect(healAmount = 1550, mythPowerGain = 10), "Restore 1,550 HP and gain +10 Myth Power.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 17. Olympian Shield Bash (Common Attack)
        CardDefinition(
            id = "c_shield_bash",
            name = "Olympian Shield Bash",
            cost = 2,
            type = CardType.ATTACK,
            rarity = CardRarity.COMMON,
            loreQuote = "Defense and offense united in a shattering strike.",
            iconKey = "shield",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(damage = 1000, shield = 600), "Deal 1,000 damage and gain 600 Shield.", goldCostToNext = 1000, shardCostToNext = 15),
                2 to CardProgressionStep(2, CardEffect(damage = 1150, shield = 700), "Deal 1,150 damage and gain 700 Shield.", goldCostToNext = 2500, shardCostToNext = 30),
                3 to CardProgressionStep(3, CardEffect(damage = 1300, shield = 800), "Deal 1,300 damage and gain 800 Shield.", goldCostToNext = 5000, shardCostToNext = 50),
                4 to CardProgressionStep(4, CardEffect(damage = 1500, shield = 950), "Deal 1,500 damage and gain 950 Shield.", goldCostToNext = 10000, shardCostToNext = 80),
                5 to CardProgressionStep(5, CardEffect(damage = 1750, shield = 1150), "Deal 1,750 damage and gain 1,150 Shield.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 18. Gaze of the Gorgon (Epic Spell)
        CardDefinition(
            id = "c_medusa_gaze",
            name = "Gaze of the Gorgon",
            cost = 3,
            type = CardType.SPELL,
            rarity = CardRarity.EPIC,
            loreQuote = "Petrifying eyes that turn courage to stone.",
            iconKey = "poison",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(damage = 1200, damageReductionPercent = 20), "Deal 1,200 damage and reduce enemy attack by 20%.", goldCostToNext = 3000, shardCostToNext = 16),
                2 to CardProgressionStep(2, CardEffect(damage = 1350, damageReductionPercent = 22), "Deal 1,350 damage and reduce enemy attack by 22%.", goldCostToNext = 6000, shardCostToNext = 35),
                3 to CardProgressionStep(3, CardEffect(damage = 1500, damageReductionPercent = 25), "Deal 1,500 damage and reduce enemy attack by 25%.", goldCostToNext = 12000, shardCostToNext = 60),
                4 to CardProgressionStep(4, CardEffect(damage = 1700, damageReductionPercent = 28), "Deal 1,700 damage and reduce enemy attack by 28%.", goldCostToNext = 22000, shardCostToNext = 90),
                5 to CardProgressionStep(5, CardEffect(damage = 2000, damageReductionPercent = 32), "Deal 2,000 damage and reduce enemy attack by 32%.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 19. Cerberus Maw (Rare Attack)
        CardDefinition(
            id = "c_cerberus_bite",
            name = "Cerberus Maw",
            cost = 3,
            type = CardType.ATTACK,
            rarity = CardRarity.RARE,
            loreQuote = "Vicious snap of the three-headed hound.",
            iconKey = "strike",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(damage = 2000, poisonDamagePerTurn = 300, poisonTurns = 2), "Deal 2,000 damage and inflict 300 poison for 2 turns.", goldCostToNext = 2000, shardCostToNext = 12),
                2 to CardProgressionStep(2, CardEffect(damage = 2250, poisonDamagePerTurn = 350, poisonTurns = 2), "Deal 2,250 damage and inflict 350 poison for 2 turns.", goldCostToNext = 4000, shardCostToNext = 25),
                3 to CardProgressionStep(3, CardEffect(damage = 2500, poisonDamagePerTurn = 400, poisonTurns = 2), "Deal 2,500 damage and inflict 400 poison for 2 turns.", goldCostToNext = 8000, shardCostToNext = 45),
                4 to CardProgressionStep(4, CardEffect(damage = 2800, poisonDamagePerTurn = 480, poisonTurns = 2), "Deal 2,800 damage and inflict 480 poison for 2 turns.", goldCostToNext = 15000, shardCostToNext = 70),
                5 to CardProgressionStep(5, CardEffect(damage = 3200, poisonDamagePerTurn = 560, poisonTurns = 2), "Deal 3,200 damage and inflict 560 poison for 2 turns.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 20. Phoenix Ash (Mythic Spell)
        CardDefinition(
            id = "c_phoenix_rebirth",
            name = "Phoenix Ash",
            cost = 4,
            type = CardType.SPELL,
            rarity = CardRarity.MYTHIC,
            loreQuote = "From sacred flames rises unending life.",
            iconKey = "fire",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(healAmount = 3000, shield = 1000), "Restore 3,000 HP and gain 1,000 Shield.", goldCostToNext = 8000, shardCostToNext = 30),
                2 to CardProgressionStep(2, CardEffect(healAmount = 3500, shield = 1200), "Restore 3,500 HP and gain 1,200 Shield.", goldCostToNext = 16000, shardCostToNext = 60),
                3 to CardProgressionStep(3, CardEffect(healAmount = 4000, shield = 1450), "Restore 4,000 HP and gain 1,450 Shield.", goldCostToNext = 30000, shardCostToNext = 100),
                4 to CardProgressionStep(4, CardEffect(healAmount = 4600, shield = 1750), "Restore 4,600 HP and gain 1,750 Shield.", goldCostToNext = 50000, shardCostToNext = 150),
                5 to CardProgressionStep(5, CardEffect(healAmount = 5400, shield = 2200), "Restore 5,400 HP and gain 2,200 Shield.", goldCostToNext = 0, shardCostToNext = 0)
            )
        )
    )

    private val cardMap: Map<String, CardDefinition> = ALL_CARDS.associateBy { it.id }

    fun findDefinition(cardId: String): CardDefinition? = cardMap[cardId]
    fun getDefinition(cardId: String): CardDefinition? = findDefinition(cardId)

    fun getCard(cardId: String, level: Int = 1): Card {
        val def = findDefinition(cardId)
        return def?.getCard(level) ?: DeckFactory.createPrototypeDeck().first { it.id == cardId }
    }

    fun getAllCards(levels: Map<String, Int> = emptyMap()): List<Card> {
        return ALL_CARDS.map { def ->
            val lvl = levels[def.id] ?: 1
            def.getCard(lvl)
        }
    }
}
