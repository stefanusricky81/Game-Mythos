package com.example.data

/**
 * Deck Synergy Definition (Requirement #10).
 */
data class DeckSynergy(
    val id: String,
    val name: String,
    val faction: String,
    val requiredCardCount: Int,
    val currentCardCount: Int,
    val isActive: Boolean,
    val iconSymbol: String,
    val description: String,
    val bonusDescription: String
)

/**
 * Full Deck Analysis snapshot (Requirement #9).
 */
data class DeckAnalysis(
    val totalCards: Int,
    val deckPower: Int,
    val averageEnergyCost: Float,
    val typeDistribution: Map<CardType, Int>,
    val rarityDistribution: Map<CardRarity, Int>,
    val factionDistribution: Map<String, Int>,
    val activeSynergies: List<DeckSynergy>,
    val availableSynergies: List<DeckSynergy>
)

/**
 * Centralized Synergy & Deck Analysis Engine (Requirement #9, #10, #13).
 */
object SynergyCatalog {

    val SYNERGY_OLYMPUS = "syn_olympus"
    val SYNERGY_WAR = "syn_war"
    val SYNERGY_UNDERWORLD = "syn_underworld"
    val SYNERGY_STORM = "syn_storm"
    val SYNERGY_VALHALLA = "syn_valhalla"
    val SYNERGY_EGYPT = "syn_egypt"

    fun analyzeDeck(cards: List<Card>, hero: HeroDefinition): DeckAnalysis {
        val total = cards.size
        val avgEnergy = if (total > 0) cards.sumOf { it.cost }.toFloat() / total.toFloat() else 0f

        val typeDist = CardType.values().associateWith { type ->
            cards.count { it.type == type }
        }

        val rarityDist = CardRarity.values().associateWith { rarity ->
            cards.count { it.rarity == rarity }
        }

        val factionDist = cards.groupBy { it.faction }.mapValues { it.value.size }

        // Synergies Check
        val olympusCount = cards.count { it.faction.contains("Olympus", ignoreCase = true) || it.id.contains("olympian") }
        val valhallaCount = cards.count { it.faction.contains("Valhalla", ignoreCase = true) || it.id.contains("thor") || it.id.contains("loki") }
        val egyptCount = cards.count { it.faction.contains("Egypt", ignoreCase = true) || it.id.contains("anubis") || it.id.contains("ra") }
        val underworldCount = cards.count { it.faction.contains("Underworld", ignoreCase = true) || it.id.contains("hades") || it.id.contains("medusa") || it.id.contains("hydra") }
        val warCount = cards.count { it.name.contains("War", ignoreCase = true) || it.name.contains("Spartan", ignoreCase = true) || it.name.contains("Rage", ignoreCase = true) || it.id.contains("ares") }
        val stormCount = cards.count { it.name.contains("Thunder", ignoreCase = true) || it.name.contains("Storm", ignoreCase = true) || it.name.contains("Lightning", ignoreCase = true) || it.id.contains("zeus") || it.id.contains("thor") }

        val synergies = listOf(
            DeckSynergy(
                id = SYNERGY_OLYMPUS,
                name = "Olympus Ascendance",
                faction = "Olympus",
                requiredCardCount = 4,
                currentCardCount = olympusCount,
                isActive = olympusCount >= 4,
                iconSymbol = "🏛️",
                description = "4+ Olympus cards in deck.",
                bonusDescription = "+10% damage to all Olympian abilities & +200 bonus Shield."
            ),
            DeckSynergy(
                id = SYNERGY_WAR,
                name = "War Bloodlust",
                faction = "Olympus / Sparta",
                requiredCardCount = 3,
                currentCardCount = warCount,
                isActive = warCount >= 3,
                iconSymbol = "⚔️",
                description = "3+ War or Spartan cards in deck.",
                bonusDescription = "+15% physical damage and +5 Myth Power on critical strikes."
            ),
            DeckSynergy(
                id = SYNERGY_UNDERWORLD,
                name = "Stygian Harvest",
                faction = "Underworld",
                requiredCardCount = 3,
                currentCardCount = underworldCount,
                isActive = underworldCount >= 3,
                iconSymbol = "💀",
                description = "3+ Underworld/Death cards in deck.",
                bonusDescription = "Gain +15% Lifesteal on all damage dealt."
            ),
            DeckSynergy(
                id = SYNERGY_STORM,
                name = "Tempest Wrath",
                faction = "Storm / Celestial",
                requiredCardCount = 3,
                currentCardCount = stormCount,
                isActive = stormCount >= 3,
                iconSymbol = "⚡",
                description = "3+ Lightning or Storm cards in deck.",
                bonusDescription = "Deal +300 bonus Shock damage whenever an Attack card is played."
            ),
            DeckSynergy(
                id = SYNERGY_VALHALLA,
                name = "Einherjar Honor",
                faction = "Valhalla",
                requiredCardCount = 4,
                currentCardCount = valhallaCount,
                isActive = valhallaCount >= 4,
                iconSymbol = "🛡️",
                description = "4+ Valhalla/Norse cards in deck.",
                bonusDescription = "Retaliates for 400 damage whenever the hero takes physical hits."
            ),
            DeckSynergy(
                id = SYNERGY_EGYPT,
                name = "Sun & Soul",
                faction = "Egypt",
                requiredCardCount = 4,
                currentCardCount = egyptCount,
                isActive = egyptCount >= 4,
                iconSymbol = "☀️",
                description = "4+ Egyptian cards in deck.",
                bonusDescription = "Restores 400 HP at the beginning of every turn."
            )
        )

        // Estimated Deck Power: hero base + card values + level multipliers + active synergies
        var power = (hero.baseHp / 5) + (hero.baseAttack * 2) + (hero.baseDefense * 2)
        power += cards.sumOf { card ->
            val base = (card.effect.damage / 2) + (card.effect.shield / 2) + (card.cost * 150) + (card.level * 200)
            base
        }
        val activeCount = synergies.count { it.isActive }
        power += activeCount * 1500

        return DeckAnalysis(
            totalCards = total,
            deckPower = power,
            averageEnergyCost = Math.round(avgEnergy * 10f) / 10f,
            typeDistribution = typeDist,
            rarityDistribution = rarityDist,
            factionDistribution = factionDist,
            activeSynergies = synergies.filter { it.isActive },
            availableSynergies = synergies
        )
    }
}
