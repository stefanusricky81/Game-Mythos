package com.example.data

enum class CardType(val label: String) {
    ATTACK("Attack"),
    DEFENSE("Defense"),
    SPELL("Spell"),
    SUMMON("Summon"),
    RELIC("Relic"),
    TRAP("Trap")
}

enum class CardRarity(val label: String) {
    COMMON("Common"),
    UNCOMMON("Uncommon"),
    RARE("Rare"),
    EPIC("Epic"),
    LEGENDARY("Legendary"),
    MYTHIC("Mythic")
}

data class CardEffect(
    val damage: Int = 0,
    val shield: Int = 0,
    val attackBuffPercent: Int = 0,
    val attackBuffTurns: Int = 1,
    val mythPowerGain: Int = 0,
    val poisonDamagePerTurn: Int = 0,
    val poisonTurns: Int = 0,
    val damageReductionPercent: Int = 0,
    val healAmount: Int = 0,
    val summonShield: Int = 0,
    val summonCounterDamage: Int = 0,
    val bonusDamageAgainstShield: Int = 0,
    val turnStartLightningDamage: Int = 0,
    val turnStartMythPowerGain: Int = 0
)

/**
 * Data-driven representation of a MYTHOS card.
 * Decides WHAT the card IS (independent of visual rendering).
 */
data class Card(
    val id: String,
    val name: String,
    val cost: Int,
    val type: CardType,
    val rarity: CardRarity,
    val effectDescription: String,
    val loreQuote: String,
    val effect: CardEffect,
    val iconKey: String = "strike",
    val artworkResId: Int? = null,
    val faction: String = "Olympus",
    val level: Int = 1,
    val maxLevel: Int = 5,
    val instanceId: String = ""
)

object DeckFactory {
    fun createPrototypeDeck(): List<Card> {
        return listOf(
            // Sample Card 1 (Requirement #8): Olympian Guard
            Card(
                id = "c_olympian_guard",
                name = "Olympian Guard",
                cost = 2,
                type = CardType.DEFENSE,
                rarity = CardRarity.COMMON,
                effectDescription = "Gain 1,200 Shield.",
                loreQuote = "Protected by the bronze aegis of ancient champions.",
                effect = CardEffect(shield = 1200),
                iconKey = "shield"
            ),
            // Sample Card 2 (Requirement #8): Titan's Wrath
            Card(
                id = "c_titans_wrath",
                name = "Titan's Wrath",
                cost = 5,
                type = CardType.ATTACK,
                rarity = CardRarity.LEGENDARY,
                effectDescription = "Deal 3,600 physical damage.",
                loreQuote = "Strength that shattered the foundations of Mount Othrys.",
                effect = CardEffect(damage = 3600),
                iconKey = "crush",
                artworkResId = HerculesIdentity.assets.cardArt.resolveResId()
            ),
            // Sample Card 3 (Requirement #8): Nectar of the Gods
            Card(
                id = "c_nectar_gods",
                name = "Nectar of the Gods",
                cost = 2,
                type = CardType.SPELL,
                rarity = CardRarity.RARE,
                effectDescription = "Restore 1,500 HP and cleanse negative effects.",
                loreQuote = "Golden ambrosia brewed atop Olympus, granting renewed vigor.",
                effect = CardEffect(healAmount = 1500),
                iconKey = "chalice"
            ),
            Card(
                id = "c_power_strike",
                name = "Power Strike",
                cost = 2,
                type = CardType.ATTACK,
                rarity = CardRarity.COMMON,
                effectDescription = "Deal 1,500 physical damage to the enemy.",
                loreQuote = "A single blow forged through celestial labors.",
                effect = CardEffect(damage = 1500),
                iconKey = "strike"
            ),
            Card(
                id = "c_spartan_phalanx",
                name = "Spartan Phalanx",
                cost = 4,
                type = CardType.SUMMON,
                rarity = CardRarity.EPIC,
                effectDescription = "Summon Phalanx guard: Grants 2,000 Shield and 800 retaliation.",
                loreQuote = "An unbreakable line of warrior brothers stood shoulder-to-shoulder.",
                effect = CardEffect(
                    summonShield = 2000,
                    summonCounterDamage = 800
                ),
                iconKey = "spears"
            ),
            Card(
                id = "c_heroic_rage",
                name = "Heroic Rage",
                cost = 3,
                type = CardType.SPELL,
                rarity = CardRarity.UNCOMMON,
                effectDescription = "Hercules gains +20% Attack for 1 turn and +20 Myth Power.",
                loreQuote = "The blood of Zeus surges with unrelenting fury.",
                effect = CardEffect(
                    attackBuffPercent = 20,
                    attackBuffTurns = 1,
                    mythPowerGain = 20
                ),
                iconKey = "fire"
            ),
            Card(
                id = "c_hydra_blade",
                name = "Hydra Venom Blade",
                cost = 3,
                type = CardType.ATTACK,
                rarity = CardRarity.RARE,
                effectDescription = "Deal 1,800 damage and inflict Poison (400/t for 2t).",
                loreQuote = "Coated in the corrosive bile of the multi-headed serpent.",
                effect = CardEffect(
                    damage = 1800,
                    poisonDamagePerTurn = 400,
                    poisonTurns = 2
                ),
                iconKey = "poison"
            ),
            Card(
                id = "c_nemean_hide",
                name = "Nemean Lion Hide",
                cost = 3,
                type = CardType.RELIC,
                rarity = CardRarity.RARE,
                effectDescription = "Gain 800 Shield and reduce incoming damage by 15%.",
                loreQuote = "Impervious pelt stripped from the golden beast of Nemea.",
                effect = CardEffect(
                    shield = 800,
                    damageReductionPercent = 15
                ),
                iconKey = "armor"
            ),
            Card(
                id = "c_divine_challenge",
                name = "Divine Challenge",
                cost = 4,
                type = CardType.SPELL,
                rarity = CardRarity.EPIC,
                effectDescription = "Deal 2,500 damage (+500 bonus against active Shield).",
                loreQuote = "Even the immortal gods cannot withstand direct defiance.",
                effect = CardEffect(
                    damage = 2500,
                    bonusDamageAgainstShield = 500
                ),
                iconKey = "lightning"
            ),
            Card(
                id = "c_zeus_thunderstone",
                name = "Zeus's Thunderstone",
                cost = 4,
                type = CardType.RELIC,
                rarity = CardRarity.MYTHIC,
                effectDescription = "Relic: Strikes for 1,000 lightning each turn and grants +10 Myth Power.",
                loreQuote = "A shard crystallized from the Father of Gods' sacred bolts.",
                effect = CardEffect(
                    turnStartLightningDamage = 1000,
                    turnStartMythPowerGain = 10
                ),
                iconKey = "thunder"
            )
        )
    }

    fun createEnemyDeck(): List<Card> {
        return listOf(
            Card(
                id = "e_spear_thrust",
                name = "Blood Spear",
                cost = 2,
                type = CardType.ATTACK,
                rarity = CardRarity.COMMON,
                effectDescription = "Deal 1,400 piercing damage.",
                loreQuote = "Ares' favored weapon never misses its quarry.",
                effect = CardEffect(damage = 1400),
                iconKey = "spears"
            ),
            Card(
                id = "e_war_shield",
                name = "Shield of Strife",
                cost = 2,
                type = CardType.DEFENSE,
                rarity = CardRarity.COMMON,
                effectDescription = "Gain 1,100 Shield.",
                loreQuote = "Hardened obsidian that repels blades.",
                effect = CardEffect(shield = 1100),
                iconKey = "shield"
            ),
            Card(
                id = "e_carnage_slash",
                name = "Carnage Cleave",
                cost = 3,
                type = CardType.ATTACK,
                rarity = CardRarity.UNCOMMON,
                effectDescription = "Deal 2,100 brutal damage.",
                loreQuote = "A savage blow fueled by raw malice.",
                effect = CardEffect(damage = 2100),
                iconKey = "strike"
            ),
            Card(
                id = "e_infernal_howl",
                name = "Infernal Roar",
                cost = 3,
                type = CardType.SPELL,
                rarity = CardRarity.RARE,
                effectDescription = "Gain +20% Attack for 1 turn and inflict 300 damage.",
                loreQuote = "A bellow that shakes the underworld.",
                effect = CardEffect(
                    attackBuffPercent = 20,
                    attackBuffTurns = 1,
                    damage = 300
                ),
                iconKey = "fire"
            ),
            Card(
                id = "e_underworld_strike",
                name = "Tartarus Ruin",
                cost = 4,
                type = CardType.ATTACK,
                rarity = CardRarity.EPIC,
                effectDescription = "Deal 2,800 dark damage.",
                loreQuote = "Chains of the abyss tear into the target.",
                effect = CardEffect(damage = 2800),
                iconKey = "crush"
            )
        )
    }
}
