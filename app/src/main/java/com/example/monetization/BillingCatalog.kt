package com.example.monetization

/**
 * Single source of truth mapping every real-money Google Play product ID used by MYTHOS
 * to what it grants. [MonetizationCatalog] already carries this per-purchase (each
 * `purchaseXxx()` call in [PlayerEconomyRepository] builds its own `grantedItems`), but
 * that context only exists at the moment the player taps "Buy". Reconciling a purchase
 * that Google delivers later - after the app was killed mid-flow, or was offline when the
 * purchase settled - needs to look up "what does this productId grant" from the productId
 * alone. This object is that lookup, and it is also used to build the one list of IDs
 * queried from Play for product details / live prices.
 *
 * IMPORTANT: every ID returned by [allProductIds] must exist in Play Console under
 * Monetize > Products > In-app products, type "Consumable", with the exact same string.
 * See SETUP_PLAY_BILLING.md for the full list to create.
 */
object BillingCatalog {

    data class CatalogEntry(
        val productId: String,
        val productType: String,
        val priceDisplay: String,
        val grantedItems: List<BundleItem>
    )

    fun allProductIds(): List<String> = allEntries().map { it.productId }

    fun entryFor(productId: String): CatalogEntry? = allEntries().find { it.productId == productId }

    private fun allEntries(): List<CatalogEntry> {
        val gemEntries = MonetizationCatalog.GEM_PRODUCTS.map { product ->
            CatalogEntry(
                productId = product.productId,
                productType = "GEM",
                priceDisplay = product.priceDisplay,
                grantedItems = listOf(BundleItem.Gems(product.totalGems))
            )
        }

        val bundleEntries = MonetizationCatalog.BUNDLES.map { bundle ->
            CatalogEntry(
                productId = bundle.bundleId,
                productType = "BUNDLE",
                priceDisplay = bundle.priceDisplay,
                grantedItems = bundle.contents
            )
        }

        val cosmeticEntries = MonetizationCatalog.COSMETICS
            .filter { it.priceGems == null } // gem-priced cosmetics never touch real-money billing
            .map { cosmetic ->
                CatalogEntry(
                    productId = cosmetic.id,
                    productType = "COSMETIC",
                    priceDisplay = cosmetic.priceDisplay,
                    grantedItems = listOf(BundleItem.Cosmetic(cosmetic.id, cosmetic.name))
                )
            }

        val cardEntries = MonetizationCatalog.getDirectCardOffers().mapNotNull { card ->
            val priceDisplay = DirectCardPricing.getPriceDisplay(card.rarity) ?: return@mapNotNull null
            CatalogEntry(
                productId = card.id,
                productType = "CARD",
                priceDisplay = priceDisplay,
                grantedItems = listOf(BundleItem.SpecificCard(card))
            )
        }

        return gemEntries + bundleEntries + cosmeticEntries + cardEntries
    }
}

/**
 * Single rule for "what price does the shop show": prefer Google Play's own live price for
 * [productId] when available, otherwise fall back to the catalog's hand-written price.
 * MonetizationCatalog's Rp... strings are development/offline metadata, not the production
 * source of truth for what a player is actually charged (see SETUP_PLAY_BILLING.md).
 */
fun resolvePriceDisplay(livePrices: Map<String, String>, productId: String, catalogPriceDisplay: String): String =
    livePrices[productId] ?: catalogPriceDisplay

/** Human-readable summary of a purchase's contents, for [PurchaseRecord.rewardSummary]. */
fun List<BundleItem>.describeForBilling(): String = joinToString(", ") { item ->
    when (item) {
        is BundleItem.HeroEntitlement -> "${item.name} — ${item.title}"
        is BundleItem.Gems -> "+${item.amount} Myth Gems"
        is BundleItem.Gold -> "+${item.amount} Gold"
        is BundleItem.Shards -> "+${item.amount} ${item.cardName} Shards"
        is BundleItem.SpecificCard -> item.card.name
        is BundleItem.RandomCard -> "1x ${item.rarity.label} Card"
        is BundleItem.Frame -> item.frameName
        is BundleItem.Avatar -> item.avatarName
        is BundleItem.Tokens -> "${item.count}x ${item.name}"
        is BundleItem.Cosmetic -> item.name
        is BundleItem.MonthlyPassEntitlement -> "Blessing of Olympus (30d)"
        is BundleItem.BattlePassEntitlement -> "Battle Pass (${item.seasonId})"
    }
}
