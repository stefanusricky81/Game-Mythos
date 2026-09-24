package com.example.monetization

import com.example.data.Card
import com.example.data.CardRarity
import com.example.data.DeckFactory
import kotlin.random.Random

/**
 * Centralized Summon Probabilities & Rates Configuration (Requirement #9).
 * - Common: 55.0%
 * - Uncommon: 25.0%
 * - Rare: 12.0%
 * - Epic: 6.0%
 * - Legendary: 1.8%
 * - Mythic: 0.2%
 */
object SummonRates {
    const val COMMON_RATE = 55.0
    const val UNCOMMON_RATE = 25.0
    const val RARE_RATE = 12.0
    const val EPIC_RATE = 6.0
    const val LEGENDARY_RATE = 1.8
    const val MYTHIC_RATE = 0.2

    // Summon costs in Myth Gems
    const val SINGLE_SUMMON_COST_GEMS = 100
    const val TEN_SUMMON_COST_GEMS = 900 // 10% discount

    val RATE_ENTRIES = listOf(
        Pair(CardRarity.MYTHIC, "0.2%"),
        Pair(CardRarity.LEGENDARY, "1.8%"),
        Pair(CardRarity.EPIC, "6.0%"),
        Pair(CardRarity.RARE, "12.0%"),
        Pair(CardRarity.UNCOMMON, "25.0%"),
        Pair(CardRarity.COMMON, "55.0%")
    )
}

/**
 * Pity System State (Requirement #10).
 * - 10 Summons: Guaranteed minimum Epic
 * - 50 Summons: Guaranteed Legendary
 * - 100 Summons: Guaranteed Mythic
 */
data class SummonPityState(
    val totalSummons: Int = 0,
    val epicPity: Int = 0,
    val legendaryPity: Int = 0,
    val mythicPity: Int = 0
) {
    companion object {
        const val EPIC_THRESHOLD = 10
        const val LEGENDARY_THRESHOLD = 50
        const val MYTHIC_THRESHOLD = 100
    }
}

/**
 * Result of a summoned card pull including duplicate conversion info.
 */
data class SummonedCardResult(
    val card: Card,
    val isDuplicate: Boolean,
    val shardsConverted: Int,
    val triggeredPity: String? = null
)

/**
 * Core Summoning Engine.
 */
object SummonEngine {

    fun executeSummon(
        count: Int,
        currentPity: SummonPityState,
        ownedCardIds: Set<String>
    ): Pair<List<SummonedCardResult>, SummonPityState> {
        val results = mutableListOf<SummonedCardResult>()
        var pity = currentPity
        val cardPool = DeckFactory.createPrototypeDeck()

        for (i in 0 until count) {
            val total = pity.totalSummons + 1
            val nextEpicPity = pity.epicPity + 1
            val nextLegendaryPity = pity.legendaryPity + 1
            val nextMythicPity = pity.mythicPity + 1

            // 1. Check Pity Triggers
            val (determinedRarity, triggeredPityName) = when {
                nextMythicPity >= SummonPityState.MYTHIC_THRESHOLD -> {
                    Pair(CardRarity.MYTHIC, "MYTHIC GUARANTEED (100th Pity)")
                }
                nextLegendaryPity >= SummonPityState.LEGENDARY_THRESHOLD -> {
                    Pair(CardRarity.LEGENDARY, "LEGENDARY GUARANTEED (50th Pity)")
                }
                nextEpicPity >= SummonPityState.EPIC_THRESHOLD -> {
                    Pair(CardRarity.EPIC, "EPIC GUARANTEED (10th Pity)")
                }
                else -> {
                    Pair(rollRarity(), null)
                }
            }

            // 2. Select a card matching determined rarity
            val candidates = cardPool.filter { it.rarity == determinedRarity }
            val chosenCard = if (candidates.isNotEmpty()) {
                candidates.random()
            } else {
                cardPool.random()
            }

            // 3. Duplicate check and shard conversion
            val isDuplicate = ownedCardIds.contains(chosenCard.id)
            val shards = if (isDuplicate) {
                ShardConversion.getShardsForDuplicate(chosenCard.rarity)
            } else {
                0
            }

            results.add(
                SummonedCardResult(
                    card = chosenCard,
                    isDuplicate = isDuplicate,
                    shardsConverted = shards,
                    triggeredPity = triggeredPityName
                )
            )

            // 4. Update Pity Counters
            pity = pity.copy(
                totalSummons = total,
                epicPity = if (determinedRarity >= CardRarity.EPIC) 0 else nextEpicPity,
                legendaryPity = if (determinedRarity >= CardRarity.LEGENDARY) 0 else nextLegendaryPity,
                mythicPity = if (determinedRarity == CardRarity.MYTHIC) 0 else nextMythicPity
            )
        }

        return Pair(results, pity)
    }

    private fun rollRarity(): CardRarity {
        val roll = Random.nextDouble(0.0, 100.0)
        var cumulative = 0.0

        cumulative += SummonRates.MYTHIC_RATE
        if (roll < cumulative) return CardRarity.MYTHIC

        cumulative += SummonRates.LEGENDARY_RATE
        if (roll < cumulative) return CardRarity.LEGENDARY

        cumulative += SummonRates.EPIC_RATE
        if (roll < cumulative) return CardRarity.EPIC

        cumulative += SummonRates.RARE_RATE
        if (roll < cumulative) return CardRarity.RARE

        cumulative += SummonRates.UNCOMMON_RATE
        if (roll < cumulative) return CardRarity.UNCOMMON

        return CardRarity.COMMON
    }
}
