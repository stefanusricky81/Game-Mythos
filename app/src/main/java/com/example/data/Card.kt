package com.example.data

enum class CardType(val label: String) {
    ATTACK("Attack"),
    DEFENSE("Defense"),
    SPELL("Spell"),
    SUMMON("Summon"),
    RELIC("Relic"),
    TRAP("Trap"),
    GOD("God")
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
    val turnStartMythPowerGain: Int = 0,
    val burnDamagePerTurn: Int = 0,
    val burnTurns: Int = 0,
    val stunTurns: Int = 0,
    val vulnerableTurns: Int = 0,
    val weakenTurns: Int = 0,
    val drawCardsCount: Int = 0,
    val energyGain: Int = 0,
    val lifestealPercent: Int = 0,
    val trueDamage: Int = 0,
    val isCleanse: Boolean = false,
    val executeThresholdPercent: Int = 0,
    val regenerationAmount: Int = 0,
    val regenerationTurns: Int = 0
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
        return HeroCatalog.HERCULES.defaultDeckCardIds.map { cardId ->
            CardCatalog.getCard(cardId)
        }
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
