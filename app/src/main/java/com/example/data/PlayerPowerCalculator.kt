package com.example.data

/**
 * Authoritative Player Combat Power Calculator (Phase 7B Requirement #7).
 *
 * Derived deterministically from:
 * - Selected Hero attributes (Base HP, Attack, Defense)
 * - ActiveDeck composition (20 cards)
 * - Card upgrade levels
 * - Card energy costs and rarities
 * - Card effect values (Damage, Shield, Heal)
 *
 * Power is display-only and does not block stage entry.
 */
object PlayerPowerCalculator {

    fun calculate(
        hero: HeroDefinition,
        activeDeck: ActiveDeck,
        cardLevels: Map<String, Int>,
        heroLevel: Int = 1
    ): Int {
        // 1. Hero Power Contribution scaled by hero progression level (Phase 7C Section 15)
        val scaledStats = HeroProgressionConfig.getScaledStats(hero, heroLevel)
        val heroHpFactor = scaledStats.hp / 10
        val heroAtkFactor = scaledStats.attack
        val heroDefFactor = scaledStats.defense
        val heroPower = ((heroHpFactor + heroAtkFactor + heroDefFactor) * 0.35f).toInt()

        // 2. Active Deck Power Contribution (20 cards)
        var deckPower = 0
        for (cardId in activeDeck.cardIds) {
            val level = cardLevels[cardId] ?: 1
            val card = CardCatalog.getCard(cardId, level)

            val rarityBonus = when (card.rarity) {
                CardRarity.MYTHIC -> 160
                CardRarity.LEGENDARY -> 120
                CardRarity.EPIC -> 85
                CardRarity.RARE -> 55
                CardRarity.UNCOMMON -> 45
                CardRarity.COMMON -> 35
            }

            val levelBonus = (card.level - 1) * 25
            val costBonus = card.cost * 15

            val effectPower = when {
                card.effect.damage > 0 -> card.effect.damage / 25
                card.effect.shield > 0 -> card.effect.shield / 20
                card.effect.healAmount > 0 -> card.effect.healAmount / 20
                else -> 40
            }

            deckPower += rarityBonus + levelBonus + costBonus + effectPower
        }

        return heroPower + deckPower
    }
}
