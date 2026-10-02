package com.example.backend

import com.example.data.ActiveDeck
import com.example.data.CardCatalog
import com.example.data.HeroCatalog

/**
 * Server-Side Deck Validation Issues and Status (Requirement #9).
 */
enum class ServerDeckIssueType {
    VALID,
    INVALID_DECK_SIZE,
    EXCEEDED_DUPLICATE_LIMIT,
    UNKNOWN_CARD_ID,
    UNKNOWN_HERO_ID,
    UNOWNED_CARD,
    MALFORMED_PAYLOAD
}

data class ServerDeckIssue(
    val type: ServerDeckIssueType,
    val message: String,
    val cardId: String? = null
)

data class ServerDeckValidationResult(
    val isValid: Boolean,
    val issues: List<ServerDeckIssue> = emptyList()
) {
    val errorSummary: String
        get() = issues.joinToString("; ") { it.message }

    companion object {
        fun valid(): ServerDeckValidationResult = ServerDeckValidationResult(true, emptyList())
        fun invalid(type: ServerDeckIssueType, message: String, cardId: String? = null) =
            ServerDeckValidationResult(false, listOf(ServerDeckIssue(type, message, cardId)))
    }
}

/**
 * Authoritative Server-side Deck Validator (Requirement #4, #9).
 *
 * Enforces the strict ActiveDeck rule:
 * - EXACTLY 20 CARDS (never 19, never 21).
 * - Max 2 copies per unique card.
 * - All cards must exist in CardCatalog.
 * - Hero must exist in HeroCatalog.
 * - All cards must be legitimately owned with sufficient quantity.
 * - Security filters against malformed or duplicate exploit payloads.
 */
object ServerDeckValidator {

    const val REQUIRED_DECK_SIZE = ActiveDeck.REQUIRED_DECK_SIZE // Exactly 20
    const val MAX_COPIES_PER_CARD = ActiveDeck.MAX_DUPLICATES_PER_CARD // Max 2

    /**
     * Validates an ActiveDeck payload against server catalog and player card inventory.
     */
    fun validateDeck(
        deck: ActiveDeck,
        inventory: ServerCardInventory? = null
    ): ServerDeckValidationResult {
        val issues = mutableListOf<ServerDeckIssue>()

        // 1. Structural check: Exactly 20 cards
        if (deck.cardIds.size != REQUIRED_DECK_SIZE) {
            issues.add(
                ServerDeckIssue(
                    type = ServerDeckIssueType.INVALID_DECK_SIZE,
                    message = "Deck must contain exactly $REQUIRED_DECK_SIZE cards, but contained ${deck.cardIds.size}."
                )
            )
        }

        // 2. Validate Hero
        if (HeroCatalog.findHero(deck.heroId) == null) {
            issues.add(
                ServerDeckIssue(
                    type = ServerDeckIssueType.UNKNOWN_HERO_ID,
                    message = "Unknown hero ID: '${deck.heroId}'."
                )
            )
        }

        // 3. Count frequencies and validate individual cards
        val cardFrequencies = mutableMapOf<String, Int>()
        for (cardId in deck.cardIds) {
            if (cardId.isBlank()) {
                issues.add(
                    ServerDeckIssue(
                        type = ServerDeckIssueType.MALFORMED_PAYLOAD,
                        message = "Deck contains a blank or malformed card ID."
                    )
                )
                continue
            }

            // Check existence in official catalog
            if (CardCatalog.getDefinition(cardId) == null) {
                issues.add(
                    ServerDeckIssue(
                        type = ServerDeckIssueType.UNKNOWN_CARD_ID,
                        message = "Card ID '$cardId' does not exist in the official catalog.",
                        cardId = cardId
                    )
                )
            }

            val count = (cardFrequencies[cardId] ?: 0) + 1
            cardFrequencies[cardId] = count

            if (count > MAX_COPIES_PER_CARD) {
                issues.add(
                    ServerDeckIssue(
                        type = ServerDeckIssueType.EXCEEDED_DUPLICATE_LIMIT,
                        message = "Card '$cardId' exceeds maximum limit of $MAX_COPIES_PER_CARD copies (found $count).",
                        cardId = cardId
                    )
                )
            }
        }

        // 4. Validate ownership against ServerCardInventory if provided
        if (inventory != null) {
            for ((cardId, requiredCount) in cardFrequencies) {
                val ownedCount = inventory.getQuantity(cardId)
                if (ownedCount < requiredCount) {
                    issues.add(
                        ServerDeckIssue(
                            type = ServerDeckIssueType.UNOWNED_CARD,
                            message = "Player owns $ownedCount copies of card '$cardId', but deck requires $requiredCount.",
                            cardId = cardId
                        )
                    )
                }
            }
        }

        return if (issues.isEmpty()) {
            ServerDeckValidationResult.valid()
        } else {
            ServerDeckValidationResult(isValid = false, issues = issues)
        }
    }
}
