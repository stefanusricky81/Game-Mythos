package com.example.backend

import com.example.data.CardCatalog

/**
 * Server-authoritative Card Ownership Record (Requirement #8).
 *
 * Represents players/{uid}/cards/{cardId}.
 * The server tracks quantity, level, and upgrade state authoritatively.
 */
data class ServerCardRecord(
    val cardId: String,
    val quantity: Int = 1,
    val level: Int = 1,
    val upgradeState: String = "BASE",
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(cardId.isNotBlank()) { "cardId cannot be blank" }
        require(quantity >= 0) { "quantity cannot be negative" }
        require(level >= 1) { "card level must be at least 1" }
    }
}

/**
 * Server Card Inventory Authority (Requirement #8).
 *
 * Validates card existence, ownership, upgrade costs, and quantities.
 * Prevents client injection attacks or unowned card claims.
 */
class ServerCardInventory(
    initialCards: Map<String, ServerCardRecord> = emptyMap()
) {
    private val _cards = initialCards.toMutableMap()

    val cards: Map<String, ServerCardRecord>
        get() = _cards.toMap()

    fun getCard(cardId: String): ServerCardRecord? = _cards[cardId]

    fun getQuantity(cardId: String): Int = _cards[cardId]?.quantity ?: 0

    /**
     * Validates if a card is legally registered in the game catalog.
     */
    fun isCardValid(cardId: String): Boolean {
        return CardCatalog.getDefinition(cardId) != null
    }

    /**
     * Authoritatively grants card copies to the player inventory.
     */
    fun grantCard(cardId: String, count: Int, now: Long = System.currentTimeMillis()): ServerCardRecord {
        require(isCardValid(cardId)) { "Unknown or invalid card ID: $cardId" }
        require(count > 0) { "Grant count must be positive, got: $count" }

        val existing = _cards[cardId]
        val newRecord = if (existing != null) {
            existing.copy(quantity = existing.quantity + count, updatedAt = now)
        } else {
            ServerCardRecord(cardId = cardId, quantity = count, level = 1, updatedAt = now)
        }
        _cards[cardId] = newRecord
        return newRecord
    }

    /**
     * Authoritatively consumes card copies for upgrades or trades.
     */
    fun consumeCard(cardId: String, count: Int, now: Long = System.currentTimeMillis()): ServerCardRecord {
        require(count > 0) { "Consume count must be positive, got: $count" }
        val existing = _cards[cardId] ?: throw IllegalStateException("Card not owned: $cardId")
        check(existing.quantity >= count) {
            "Insufficient card quantity: required $count, owned ${existing.quantity} for $cardId"
        }

        val updated = existing.copy(quantity = existing.quantity - count, updatedAt = now)
        _cards[cardId] = updated
        return updated
    }

    /**
     * Server-side card upgrade validation and execution.
     * Level N requires N * 2 card shards/copies.
     */
    fun upgradeCard(cardId: String, now: Long = System.currentTimeMillis()): ServerCardRecord {
        require(isCardValid(cardId)) { "Unknown card ID: $cardId" }
        val existing = _cards[cardId] ?: throw IllegalStateException("Card not owned: $cardId")
        val requiredCopies = existing.level * 2
        check(existing.quantity >= requiredCopies) {
            "Insufficient copies for upgrade: requires $requiredCopies, has ${existing.quantity}"
        }

        val updated = existing.copy(
            quantity = existing.quantity - requiredCopies,
            level = existing.level + 1,
            upgradeState = "TIER_${existing.level + 1}",
            updatedAt = now
        )
        _cards[cardId] = updated
        return updated
    }
}
