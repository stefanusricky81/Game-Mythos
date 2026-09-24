package com.example.monetization

import com.example.data.CardRarity

/**
 * MYTHOS Monetization — Three Primary Currencies & Progression Shards
 *
 * 1. GOLD: Free gameplay currency (battles, quests, campaign, events). Used for upgrades & progression.
 * 2. MYTH GEMS: Premium currency (top-up, passes, special rewards). Used for summons, bundles, cosmetics.
 * 3. CARD SHARDS: Card-specific progression currency (duplicates, summons, campaign). Used for card rank-ups.
 */
enum class CurrencyType(val displayName: String, val iconSymbol: String) {
    GOLD("Gold", "🪙"),
    MYTH_GEMS("Myth Gems", "💎"),
    CARD_SHARDS("Card Shards", "💠")
}

/**
 * Shard balance for a specific card or hero entity.
 */
data class CardShard(
    val cardId: String,
    val cardName: String,
    val amount: Int
)

/**
 * Centralized Duplicate Shard Conversion Table (Requirement #8).
 * Converts duplicate cards drawn from summons into progression shards.
 *
 * Common: 5 shards
 * Uncommon: 8 shards
 * Rare: 12 shards
 * Epic: 16 shards
 * Legendary: 20 shards
 * Mythic: 30 shards
 */
object ShardConversion {
    const val COMMON_DUPLICATE_SHARDS = 5
    const val UNCOMMON_DUPLICATE_SHARDS = 8
    const val RARE_DUPLICATE_SHARDS = 12
    const val EPIC_DUPLICATE_SHARDS = 16
    const val LEGENDARY_DUPLICATE_SHARDS = 20
    const val MYTHIC_DUPLICATE_SHARDS = 30

    fun getShardsForDuplicate(rarity: CardRarity): Int = when (rarity) {
        CardRarity.COMMON -> COMMON_DUPLICATE_SHARDS
        CardRarity.UNCOMMON -> UNCOMMON_DUPLICATE_SHARDS
        CardRarity.RARE -> RARE_DUPLICATE_SHARDS
        CardRarity.EPIC -> EPIC_DUPLICATE_SHARDS
        CardRarity.LEGENDARY -> LEGENDARY_DUPLICATE_SHARDS
        CardRarity.MYTHIC -> MYTHIC_DUPLICATE_SHARDS
    }
}
