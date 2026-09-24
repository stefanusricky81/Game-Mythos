package com.example.monetization

import com.example.data.Card
import com.example.data.CardRarity
import com.example.data.DeckFactory
import com.example.data.HerculesIdentity

/**
 * Data Model for Myth Gem Top-Up Products (Requirement #3).
 */
data class GemProduct(
    val productId: String,
    val displayName: String,
    val gemAmount: Int,
    val bonusGems: Int = 0,
    val priceDisplay: String,
    val priceAmountIdr: Long,
    val featured: Boolean = false,
    val badgeText: String? = null,
    val sortOrder: Int
) {
    val totalGems: Int get() = gemAmount + bonusGems
}

/**
 * Centralized Direct Card Pricing (Requirement #6).
 * Common & Uncommon are not sold directly.
 * Rare: Rp24.900
 * Epic: Rp49.900
 * Legendary: Rp99.900
 * Mythic: Rp199.900
 */
object DirectCardPricing {
    val PRICING_MAP: Map<CardRarity, Long?> = mapOf(
        CardRarity.COMMON to null,
        CardRarity.UNCOMMON to null,
        CardRarity.RARE to 24_900L,
        CardRarity.EPIC to 49_900L,
        CardRarity.LEGENDARY to 99_900L,
        CardRarity.MYTHIC to 199_900L
    )

    fun getPriceDisplay(rarity: CardRarity): String? = when (rarity) {
        CardRarity.COMMON -> null
        CardRarity.UNCOMMON -> null
        CardRarity.RARE -> "Rp24.900"
        CardRarity.EPIC -> "Rp49.900"
        CardRarity.LEGENDARY -> "Rp99.900"
        CardRarity.MYTHIC -> "Rp199.900"
    }

    fun isDirectlyPurchasable(rarity: CardRarity): Boolean = PRICING_MAP[rarity] != null
}

/**
 * Types of bundle content items.
 */
sealed class BundleItem {
    data class HeroEntitlement(
        val heroId: String,
        val name: String,
        val title: String,
        val rarity: CardRarity = CardRarity.LEGENDARY
    ) : BundleItem()
    data class Gems(val amount: Int) : BundleItem()
    data class Gold(val amount: Int) : BundleItem()
    data class Shards(val cardId: String, val cardName: String, val amount: Int) : BundleItem()
    data class SpecificCard(val card: Card) : BundleItem()
    data class RandomCard(val rarity: CardRarity, val count: Int = 1) : BundleItem()
    data class Frame(val frameId: String, val frameName: String) : BundleItem()
    data class Avatar(val avatarId: String, val avatarName: String) : BundleItem()
    data class Tokens(val count: Int, val name: String) : BundleItem()
    data class Cosmetic(val cosmeticId: String, val name: String) : BundleItem()
    data class MonthlyPassEntitlement(val durationDays: Int = 30) : BundleItem()
    data class BattlePassEntitlement(val seasonId: String = "season_1") : BundleItem()
}

/**
 * Premium Bundle Model (Requirement #11, #12, #13, #14, #15).
 */
data class PremiumBundle(
    val bundleId: String,
    val title: String,
    val subtitle: String,
    val priceDisplay: String,
    val priceAmountIdr: Long,
    val isOneTime: Boolean,
    val rarity: CardRarity,
    val description: String,
    val contents: List<BundleItem>,
    val badgeText: String? = null
)

/**
 * Cosmetic Item Model (Requirement #16).
 * Strictly visual-only; no stat modifications.
 */
enum class CosmeticType {
    SKIN,
    FRAME,
    AVATAR,
    SUMMON_EFFECT,
    VICTORY_ANIMATION,
    EMOTE
}

data class CosmeticItem(
    val id: String,
    val name: String,
    val type: CosmeticType,
    val priceGems: Int? = null,
    val priceDisplay: String,
    val description: String,
    val heroId: String? = null
)

/**
 * Centralized Catalog of all monetization offerings in MYTHOS.
 */
object MonetizationCatalog {

    /**
     * Centralized Myth Gems Top-Up Catalog (Requirement #3)
     */
    val GEM_PRODUCTS: List<GemProduct> = listOf(
        GemProduct(
            productId = "gem_starter",
            displayName = "Gem Starter",
            gemAmount = 100,
            bonusGems = 0,
            priceDisplay = "Rp9.900",
            priceAmountIdr = 9_900L,
            sortOrder = 1
        ),
        GemProduct(
            productId = "gem_small",
            displayName = "Gem Small",
            gemAmount = 330,
            bonusGems = 30,
            priceDisplay = "Rp29.900",
            priceAmountIdr = 29_900L,
            sortOrder = 2
        ),
        GemProduct(
            productId = "gem_medium",
            displayName = "Gem Medium",
            gemAmount = 700,
            bonusGems = 100,
            priceDisplay = "Rp59.900",
            priceAmountIdr = 59_900L,
            featured = true,
            badgeText = "POPULAR",
            sortOrder = 3
        ),
        GemProduct(
            productId = "gem_large",
            displayName = "Gem Large",
            gemAmount = 1200,
            bonusGems = 200,
            priceDisplay = "Rp99.900",
            priceAmountIdr = 99_900L,
            sortOrder = 4
        ),
        GemProduct(
            productId = "gem_mega",
            displayName = "Gem Mega",
            gemAmount = 2500,
            bonusGems = 500,
            priceDisplay = "Rp199.900",
            priceAmountIdr = 199_900L,
            featured = true,
            badgeText = "BEST VALUE",
            sortOrder = 5
        ),
        GemProduct(
            productId = "gem_ultimate",
            displayName = "Gem Ultimate",
            gemAmount = 6500,
            bonusGems = 1500,
            priceDisplay = "Rp499.900",
            priceAmountIdr = 499_900L,
            badgeText = "VIP VAULT",
            sortOrder = 6
        )
    )

    /**
     * Direct Purchasable Cards Catalog (Requirement #6 & #7)
     */
    fun getDirectCardOffers(): List<Card> {
        val allCards = DeckFactory.createPrototypeDeck()
        // Only return cards with Rarity Rare, Epic, Legendary, Mythic (Base level only!)
        return allCards.filter { DirectCardPricing.isDirectlyPurchasable(it.rarity) }
    }

    /**
     * Centralized Premium Bundles (Requirement #11, #12, #13, #14, #15)
     */
    val BUNDLES: List<PremiumBundle> by lazy {
        val prototypeDeck = DeckFactory.createPrototypeDeck()
        val titansWrath = prototypeDeck.find { it.id == "c_titans_wrath" }!!
        val thunderstone = prototypeDeck.find { it.id == "c_zeus_thunderstone" }!!

        listOf(
            // 1. STARTER PACK (Requirement #11)
            PremiumBundle(
                bundleId = "bundle_starter_pack",
                title = "MYTHOS STARTER PACK",
                subtitle = "Divine Beginner Arsenal",
                priceDisplay = "Rp19.900",
                priceAmountIdr = 19_900L,
                isOneTime = true,
                rarity = CardRarity.EPIC,
                description = "Everything a burgeoning demigod needs to conquer early battles.",
                badgeText = "85% OFF • ONE TIME",
                contents = listOf(
                    BundleItem.Gems(300),
                    BundleItem.RandomCard(CardRarity.RARE, 1),
                    BundleItem.RandomCard(CardRarity.EPIC, 1),
                    BundleItem.Gold(10_000),
                    BundleItem.Tokens(3, "Upgrade Tokens"),
                    BundleItem.Frame("frame_hercules_starter", "Exclusive Hercules Frame"),
                    BundleItem.Avatar("avatar_hercules_starter", "Exclusive Hercules Avatar")
                )
            ),
            // 2. LEGENDARY HERO BUNDLE (Requirement #12)
            PremiumBundle(
                bundleId = "bundle_legendary_hercules",
                title = "LEGENDARY HERCULES HERO BUNDLE",
                subtitle = "Hercules — Champion of Olympus",
                priceDisplay = "Rp149.900",
                priceAmountIdr = 149_900L,
                isOneTime = false,
                rarity = CardRarity.LEGENDARY,
                description = "Harness the primal might of Olympus's greatest champion with hero unlock, gems, shards, and exclusive cosmetics.",
                badgeText = "GUARANTEED LEGENDARY",
                contents = listOf(
                    BundleItem.HeroEntitlement(
                        heroId = HerculesIdentity.HERO_ID,
                        name = HerculesIdentity.NAME,
                        title = HerculesIdentity.TITLE,
                        rarity = HerculesIdentity.RARITY
                    ),
                    BundleItem.Gems(1_000),
                    BundleItem.Shards("hero_hercules", "Hercules", 50),
                    BundleItem.Gold(30_000),
                    BundleItem.Frame("frame_olympus_gold", "Exclusive Hercules Frame"),
                    BundleItem.Avatar("avatar_olympus_gold", "Exclusive Hercules Avatar")
                )
            ),
            // 3. MYTHIC BUNDLE (Requirement #13)
            PremiumBundle(
                bundleId = "bundle_mythic_wrath",
                title = "MYTHIC PANTHEON BUNDLE",
                subtitle = "Wrath of Mount Olympus",
                priceDisplay = "Rp299.900",
                priceAmountIdr = 299_900L,
                isOneTime = false,
                rarity = CardRarity.MYTHIC,
                description = "Supreme power direct from the throne of Zeus. Features base-level Mythic card and divine materials.",
                badgeText = "CELESTIAL TIER",
                contents = listOf(
                    BundleItem.SpecificCard(thunderstone),
                    BundleItem.Gems(2_000),
                    BundleItem.Shards("c_zeus_thunderstone", "Zeus Thunderstone", 100),
                    BundleItem.Gold(100_000),
                    BundleItem.Frame("frame_celestial_mythic", "Exclusive Mythic Frame"),
                    BundleItem.Avatar("avatar_celestial_mythic", "Exclusive Zeus Avatar"),
                    BundleItem.Cosmetic("fx_celestial_lightning", "Summon Visual Effect")
                )
            ),
            // 4. MONTHLY PASS: BLESSING OF OLYMPUS (Requirement #15)
            PremiumBundle(
                bundleId = "pass_blessing_olympus",
                title = "BLESSING OF OLYMPUS",
                subtitle = "30-Day Demigod Daily Pass",
                priceDisplay = "Rp49.900",
                priceAmountIdr = 49_900L,
                isOneTime = false,
                rarity = CardRarity.RARE,
                description = "100 Gems upfront + 50 Gems delivered daily for 30 days (1,600 Total Gems) plus daily bonus summon.",
                badgeText = "DAILY VALUE",
                contents = listOf(
                    BundleItem.Gems(100),
                    BundleItem.MonthlyPassEntitlement(30),
                    BundleItem.Cosmetic("fx_aura_blessing", "Aura of Olympus Effect")
                )
            ),
            // 5. BATTLE PASS: SEASON OF HEROES (Requirement #14)
            PremiumBundle(
                bundleId = "pass_battle_pass_s1",
                title = "MYTHOS BATTLE PASS",
                subtitle = "Season I: Fall of the Titans",
                priceDisplay = "Rp79.900",
                priceAmountIdr = 79_900L,
                isOneTime = true,
                rarity = CardRarity.EPIC,
                description = "Unlock the Premium reward track with exclusive cards, gems, shards, and seasonal cosmetic rewards.",
                badgeText = "SEASON PASS",
                contents = listOf(
                    BundleItem.BattlePassEntitlement("season_1"),
                    BundleItem.Gems(500),
                    BundleItem.Frame("frame_titan_slayer", "Titan Slayer Frame")
                )
            )
        )
    }

    /**
     * Cosmetic Offerings Catalog (Requirement #16).
     * Visual only; no stats.
     */
    val COSMETICS: List<CosmeticItem> = listOf(
        CosmeticItem(
            id = "skin_golden_hercules",
            name = "Golden Champion Hercules",
            type = CosmeticType.SKIN,
            priceDisplay = "Rp99.900",
            description = "Gilded lion pelt and radiant divine armor for Hercules.",
            heroId = "hero_hercules"
        ),
        CosmeticItem(
            id = "skin_god_of_war_ares",
            name = "God of Olympus Ares",
            type = CosmeticType.SKIN,
            priceDisplay = "Rp199.900",
            description = "Crimson plate and flaming aura for Warlord Ares.",
            heroId = "hero_ares"
        ),
        CosmeticItem(
            id = "frame_spartan_laurel",
            name = "Spartan Laurel Frame",
            type = CosmeticType.FRAME,
            priceGems = 500,
            priceDisplay = "500 Gems",
            description = "Solid bronze woven laurel border for card and hero showcase."
        ),
        CosmeticItem(
            id = "fx_celestial_lightning",
            name = "Celestial Lightning Summon",
            type = CosmeticType.SUMMON_EFFECT,
            priceGems = 750,
            priceDisplay = "750 Gems",
            description = "Thunderbolts shatter the summoning altar upon card revelation."
        ),
        CosmeticItem(
            id = "emote_olympian_triumph",
            name = "Olympian Triumph Emote",
            type = CosmeticType.EMOTE,
            priceGems = 300,
            priceDisplay = "300 Gems",
            description = "Hercules raising the Nemean club in victorious glory."
        )
    )
}
