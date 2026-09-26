package com.example.data

/**
 * Represents the player's saved active deck (Requirements #4, #11).
 * Exactly 20 cards, max 2 duplicates per card.
 */
data class ActiveDeck(
    val deckId: String = "active_deck_olympus",
    val name: String = "Olympus Deck",
    val heroId: String = HerculesIdentity.HERO_ID,
    val cardIds: List<String> = HeroCatalog.HERCULES.defaultDeckCardIds.toList(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val totalCards: Int get() = cardIds.size
    val isComplete: Boolean get() = cardIds.size == REQUIRED_DECK_SIZE
    val deckName: String get() = name

    /**
     * Creates a guaranteed deep defensive copy with isolated cardIds.
     */
    fun createDefensiveCopy(): ActiveDeck = copy(
        cardIds = cardIds.toList()
    )

    companion object {
        const val REQUIRED_DECK_SIZE = 20
        const val MAX_DUPLICATES_PER_CARD = 2

        fun createDefaultOlympusDeck(): ActiveDeck = ActiveDeck(
            deckId = "active_deck_olympus",
            name = "Olympus Deck",
            heroId = HerculesIdentity.HERO_ID,
            cardIds = HeroCatalog.HERCULES.defaultDeckCardIds.toList(),
            updatedAt = System.currentTimeMillis()
        )
    }
}

/**
 * Categorized error types for deck validation (Requirement #5).
 */
enum class DeckValidationErrorType {
    VALID,
    TOO_FEW_CARDS,
    TOO_MANY_CARDS,
    TOO_MANY_COPIES,
    CARD_NOT_OWNED,
    INVALID_CARD,
    INVALID_HERO
}

/**
 * Specific validation issue record.
 */
data class DeckValidationIssue(
    val type: DeckValidationErrorType,
    val message: String,
    val cardId: String? = null
)

/**
 * Structured result of deck validation (Requirement #5, #10).
 */
data class DeckValidationResult(
    val isValid: Boolean,
    val issues: List<DeckValidationIssue> = emptyList(),
    val errors: List<String> = issues.map { it.message }
) {
    val primaryErrorMessage: String? get() = issues.firstOrNull()?.message ?: errors.firstOrNull()
    val primaryIssueType: DeckValidationErrorType
        get() = issues.firstOrNull()?.type ?: if (isValid) DeckValidationErrorType.VALID else DeckValidationErrorType.TOO_FEW_CARDS

    companion object {
        fun valid(): DeckValidationResult = DeckValidationResult(isValid = true, issues = emptyList())
        fun error(type: DeckValidationErrorType, message: String, cardId: String? = null): DeckValidationResult =
            DeckValidationResult(isValid = false, issues = listOf(DeckValidationIssue(type, message, cardId)))
    }
}

/**
 * Centralized Authoritative Deck Validator (Requirement #5).
 * Validates deck constraints outside of UI components.
 */
object DeckValidator {

    fun validate(
        deck: ActiveDeck,
        ownedCardCounts: Map<String, Int>? = null
    ): DeckValidationResult {
        val issues = mutableListOf<DeckValidationIssue>()

        // 1. Hero validation
        val hero = HeroCatalog.findHero(deck.heroId)
        if (hero == null) {
            issues.add(
                DeckValidationIssue(
                    type = DeckValidationErrorType.INVALID_HERO,
                    message = "Invalid hero specified: '${deck.heroId}'."
                )
            )
        }

        // 2. Exact deck size validation (Requirement #5, #10)
        if (deck.cardIds.size < ActiveDeck.REQUIRED_DECK_SIZE) {
            val needed = ActiveDeck.REQUIRED_DECK_SIZE - deck.cardIds.size
            issues.add(
                DeckValidationIssue(
                    type = DeckValidationErrorType.TOO_FEW_CARDS,
                    message = "Need $needed more card${if (needed > 1) "s" else ""} (currently ${deck.cardIds.size}/${ActiveDeck.REQUIRED_DECK_SIZE})."
                )
            )
        } else if (deck.cardIds.size > ActiveDeck.REQUIRED_DECK_SIZE) {
            val excess = deck.cardIds.size - ActiveDeck.REQUIRED_DECK_SIZE
            issues.add(
                DeckValidationIssue(
                    type = DeckValidationErrorType.TOO_MANY_CARDS,
                    message = "Deck exceeds limit by $excess card${if (excess > 1) "s" else ""} (${deck.cardIds.size}/${ActiveDeck.REQUIRED_DECK_SIZE})."
                )
            )
        }

        // 3. Card counts and duplicate validation (max 2 copies)
        val frequencies = deck.cardIds.groupingBy { it }.eachCount()

        frequencies.forEach { (cardId, count) ->
            val cardDef = CardCatalog.findDefinition(cardId)
            val cardName = cardDef?.name ?: cardId

            if (cardDef == null) {
                issues.add(
                    DeckValidationIssue(
                        type = DeckValidationErrorType.INVALID_CARD,
                        message = "Unknown card in deck: '$cardId'.",
                        cardId = cardId
                    )
                )
            }

            if (count > ActiveDeck.MAX_DUPLICATES_PER_CARD) {
                issues.add(
                    DeckValidationIssue(
                        type = DeckValidationErrorType.TOO_MANY_COPIES,
                        message = "Maximum ${ActiveDeck.MAX_DUPLICATES_PER_CARD} copies allowed for '$cardName' (has $count).",
                        cardId = cardId
                    )
                )
            }

            // 4. Ownership validation (if provided)
            if (ownedCardCounts != null) {
                val ownedCopies = ownedCardCounts[cardId] ?: 0
                if (ownedCopies <= 0) {
                    issues.add(
                        DeckValidationIssue(
                            type = DeckValidationErrorType.CARD_NOT_OWNED,
                            message = "'$cardName' is locked and unowned.",
                            cardId = cardId
                        )
                    )
                } else if (count > ownedCopies) {
                    issues.add(
                        DeckValidationIssue(
                            type = DeckValidationErrorType.CARD_NOT_OWNED,
                            message = "You only own $ownedCopies cop${if (ownedCopies == 1) "y" else "ies"} of '$cardName', but deck requires $count.",
                            cardId = cardId
                        )
                    )
                }
            }
        }

        return DeckValidationResult(
            isValid = issues.isEmpty(),
            issues = issues
        )
    }
}

