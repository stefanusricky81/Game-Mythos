package com.example.data

/**
 * Represents the player's saved active deck (Requirement #11).
 */
data class ActiveDeck(
    val heroId: String = HerculesIdentity.HERO_ID,
    val cardIds: List<String> = HeroCatalog.HERCULES.defaultDeckCardIds,
    val deckName: String = "Olympus Deck"
) {
    val totalCards: Int get() = cardIds.size
    val isComplete: Boolean get() = cardIds.size == REQUIRED_DECK_SIZE

    companion object {
        const val REQUIRED_DECK_SIZE = 20
        const val MAX_DUPLICATES_PER_CARD = 2

        fun createDefaultOlympusDeck(): ActiveDeck = ActiveDeck()
    }
}

/**
 * Result of deck validation (Requirement #10).
 */
data class DeckValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
) {
    val primaryErrorMessage: String? get() = errors.firstOrNull()
}

/**
 * Centralized Deck Validator (Requirement #10).
 * Validates deck constraints outside of UI.
 */
object DeckValidator {

    fun validate(
        deck: ActiveDeck,
        ownedCardCounts: Map<String, Int>? = null
    ): DeckValidationResult {
        val errors = mutableListOf<String>()

        // 1. Hero validation
        val hero = HeroCatalog.findHero(deck.heroId)
        if (hero == null) {
            errors.add("Invalid hero specified: '${deck.heroId}'.")
        }

        // 2. Exact deck size validation
        if (deck.cardIds.size != ActiveDeck.REQUIRED_DECK_SIZE) {
            errors.add("Deck must contain exactly ${ActiveDeck.REQUIRED_DECK_SIZE} cards (currently ${deck.cardIds.size}/${ActiveDeck.REQUIRED_DECK_SIZE}).")
        }

        // 3. Card counts and duplicate validation
        val frequencies = deck.cardIds.groupingBy { it }.eachCount()

        frequencies.forEach { (cardId, count) ->
            val cardDef = CardCatalog.findDefinition(cardId)
            val cardName = cardDef?.name ?: cardId

            if (cardDef == null) {
                errors.add("Unknown card in deck: '$cardId'.")
            }

            if (count > ActiveDeck.MAX_DUPLICATES_PER_CARD) {
                errors.add("Maximum ${ActiveDeck.MAX_DUPLICATES_PER_CARD} copies allowed for '$cardName' (has $count).")
            }

            // 4. Ownership validation (if provided)
            if (ownedCardCounts != null) {
                val ownedCopies = ownedCardCounts[cardId] ?: 0
                if (count > ownedCopies) {
                    errors.add("You only own $ownedCopies copy of '$cardName', but deck requires $count.")
                }
            }
        }

        return DeckValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
}
