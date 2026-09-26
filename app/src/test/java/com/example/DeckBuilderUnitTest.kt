package com.example

import com.example.data.*
import com.example.monetization.PlayerEconomyRepository
import org.junit.Assert.*
import org.junit.Test

class DeckBuilderUnitTest {

    @Test
    fun testDefaultOlympusDeck_isValid() {
        val defaultDeck = ActiveDeck.createDefaultOlympusDeck()
        assertEquals(20, defaultDeck.totalCards)
        assertTrue(defaultDeck.isComplete)

        // Ownership map where all default cards have at least 2 copies
        val ownedCounts = defaultDeck.cardIds.associateWith { 2 }
        val result = DeckValidator.validate(defaultDeck, ownedCounts)

        assertTrue(result.isValid)
        assertEquals(DeckValidationErrorType.VALID, result.primaryIssueType)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun testDeckValidation_tooFewCards() {
        val shortDeck = ActiveDeck(
            cardIds = listOf("c_olympian_guard", "c_titans_wrath")
        )
        val result = DeckValidator.validate(shortDeck)

        assertFalse(result.isValid)
        assertEquals(DeckValidationErrorType.TOO_FEW_CARDS, result.primaryIssueType)
        assertTrue(result.primaryErrorMessage?.contains("Need 18 more cards") == true)
    }

    @Test
    fun testDeckValidation_tooManyCards() {
        val longDeck = ActiveDeck(
            cardIds = List(22) { "c_olympian_guard" }
        )
        val result = DeckValidator.validate(longDeck)

        assertFalse(result.isValid)
        val issueTypes = result.issues.map { it.type }
        assertTrue(issueTypes.contains(DeckValidationErrorType.TOO_MANY_CARDS))
        assertTrue(issueTypes.contains(DeckValidationErrorType.TOO_MANY_COPIES))
    }

    @Test
    fun testDeckValidation_tooManyCopiesOfSingleCard() {
        // 20 cards, but 3 copies of Titan's Wrath
        val cardList = mutableListOf(
            "c_titans_wrath", "c_titans_wrath", "c_titans_wrath",
            "c_power_strike", "c_power_strike",
            "c_heroic_rage", "c_heroic_rage",
            "c_nectar_gods", "c_nectar_gods",
            "c_spartan_phalanx", "c_spartan_phalanx",
            "c_hydra_blade", "c_hydra_blade",
            "c_nemean_hide", "c_nemean_hide",
            "c_divine_challenge", "c_divine_challenge",
            "c_celestial_arrow", "c_celestial_arrow",
            "c_olympian_guard"
        )
        assertEquals(20, cardList.size)

        val deck = ActiveDeck(cardIds = cardList)
        val result = DeckValidator.validate(deck)

        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.type == DeckValidationErrorType.TOO_MANY_COPIES })
        assertTrue(result.issues.any { it.cardId == "c_titans_wrath" })
    }

    @Test
    fun testDeckValidation_cardNotOwnedOrLocked() {
        val defaultDeck = ActiveDeck.createDefaultOlympusDeck()
        // Player only owns 0 copies of titans wrath
        val ownedCounts = defaultDeck.cardIds.associateWith { 2 }.toMutableMap()
        ownedCounts["c_titans_wrath"] = 0

        val result = DeckValidator.validate(defaultDeck, ownedCounts)
        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.type == DeckValidationErrorType.CARD_NOT_OWNED })
    }

    @Test
    fun testDeckValidation_insufficientOwnedCopies() {
        val defaultDeck = ActiveDeck.createDefaultOlympusDeck()
        // Player owns only 1 copy of olympian guard, but deck has 2
        val ownedCounts = defaultDeck.cardIds.associateWith { 2 }.toMutableMap()
        ownedCounts["c_olympian_guard"] = 1

        val result = DeckValidator.validate(defaultDeck, ownedCounts)
        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.type == DeckValidationErrorType.CARD_NOT_OWNED && it.cardId == "c_olympian_guard" })
    }

    @Test
    fun testDeckValidation_invalidCardId() {
        val badDeckList = HeroCatalog.HERCULES.defaultDeckCardIds.toMutableList()
        badDeckList[0] = "c_non_existent_card_999"

        val deck = ActiveDeck(cardIds = badDeckList)
        val result = DeckValidator.validate(deck)

        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.type == DeckValidationErrorType.INVALID_CARD })
    }

    @Test
    fun testActiveDeckSavingInEconomyRepository() {
        val repo = PlayerEconomyRepository.instance
        val defaultDeck = ActiveDeck.createDefaultOlympusDeck()
        val saveResult = repo.saveActiveDeck(defaultDeck)

        assertTrue(saveResult.isValid)
        assertEquals(20, repo.economyState.value.activeDeck.totalCards)
        assertEquals(defaultDeck.cardIds, repo.economyState.value.activeDeck.cardIds)
    }
}
