package com.example

import com.example.data.CardRarity
import com.example.monetization.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MonetizationUnitTest {

    private lateinit var billingProvider: MockBillingProvider
    private lateinit var repository: PlayerEconomyRepository

    @Before
    fun setUp() {
        billingProvider = MockBillingProvider()
        repository = PlayerEconomyRepository(billingProvider)
    }

    @Test
    fun testMockBillingSuccess() = runBlocking {
        val result = billingProvider.initiatePurchase(
            productId = "gem_starter",
            productType = "GEM",
            priceDisplay = "Rp9.900",
            grantedItems = listOf(BundleItem.Gems(100)),
            simulationMode = MockBillingResult.SUCCESS
        )

        assertTrue("Expected PurchaseResult.Success", result is PurchaseResult.Success)
        val success = result as PurchaseResult.Success
        assertEquals(PurchaseStatus.PURCHASED, success.record.status)
        assertTrue(success.record.acknowledged)
    }

    @Test
    fun testMockBillingModes() = runBlocking {
        val cancelled = billingProvider.initiatePurchase(
            productId = "test_cancel",
            productType = "GEM",
            priceDisplay = "Rp9.900",
            grantedItems = emptyList(),
            simulationMode = MockBillingResult.CANCELLED
        )
        assertTrue(cancelled is PurchaseResult.Cancelled)

        val pending = billingProvider.initiatePurchase(
            productId = "test_pending",
            productType = "GEM",
            priceDisplay = "Rp9.900",
            grantedItems = emptyList(),
            simulationMode = MockBillingResult.PENDING
        )
        assertTrue(pending is PurchaseResult.Pending)

        val failed = billingProvider.initiatePurchase(
            productId = "test_fail",
            productType = "GEM",
            priceDisplay = "Rp9.900",
            grantedItems = emptyList(),
            simulationMode = MockBillingResult.FAILED
        )
        assertTrue(failed is PurchaseResult.Failed)
    }

    @Test
    fun testIdempotencyEnforcement() {
        val record = PurchaseRecord(
            purchaseId = "unique_purchase_123",
            productId = "bundle_starter_pack",
            productType = "BUNDLE",
            status = PurchaseStatus.PURCHASED,
            currency = "IDR",
            priceDisplay = "Rp19.900",
            rewardSummary = "Starter Pack",
            acknowledged = true,
            consumed = true
        )
        val items = listOf(BundleItem.Gems(300), BundleItem.Gold(10000))

        val firstGrant = repository.grantEntitlements(record, items)
        assertTrue("First entitlement grant must succeed", firstGrant)

        val secondGrant = repository.grantEntitlements(record, items)
        assertFalse("Duplicate purchaseId grant must be rejected idempotently", secondGrant)
    }

    @Test
    fun testSummonPityEngine() {
        val pity = SummonPityState(
            totalSummons = 9,
            epicPity = 9,
            legendaryPity = 9,
            mythicPity = 9
        )
        val (results, newPity) = SummonEngine.executeSummon(
            count = 1,
            currentPity = pity,
            ownedCardIds = emptySet()
        )

        assertEquals(1, results.size)
        val pull = results[0]
        assertTrue("10th summon must guarantee minimum Epic", pull.card.rarity >= CardRarity.EPIC)
        assertEquals("Epic pity counter should reset to 0 after granting Epic", 0, newPity.epicPity)
    }

    @Test
    fun testDuplicateShardConversionRates() {
        assertEquals(5, ShardConversion.getShardsForDuplicate(CardRarity.COMMON))
        assertEquals(8, ShardConversion.getShardsForDuplicate(CardRarity.UNCOMMON))
        assertEquals(12, ShardConversion.getShardsForDuplicate(CardRarity.RARE))
        assertEquals(16, ShardConversion.getShardsForDuplicate(CardRarity.EPIC))
        assertEquals(20, ShardConversion.getShardsForDuplicate(CardRarity.LEGENDARY))
        assertEquals(30, ShardConversion.getShardsForDuplicate(CardRarity.MYTHIC))
    }

    @Test
    fun testHerculesLegendaryHeroBundleEntitlements() {
        val herculesBundle = MonetizationCatalog.BUNDLES.find { it.bundleId == "bundle_legendary_hercules" }
        assertNotNull("Legendary Hercules Hero Bundle must exist in catalog", herculesBundle)
        assertEquals("Rp149.900", herculesBundle!!.priceDisplay)

        // Verify primary reward is Hercules Hero Entitlement, NOT Titan's Wrath
        val heroItem = herculesBundle.contents.filterIsInstance<BundleItem.HeroEntitlement>().firstOrNull()
        assertNotNull("Bundle must contain BundleItem.HeroEntitlement", heroItem)
        assertEquals("hero_hercules", heroItem!!.heroId)
        assertEquals("Hercules", heroItem.name)
        assertEquals("Champion of Olympus", heroItem.title)

        // Grant bundle to player repository
        val record = PurchaseRecord(
            purchaseId = "hercules_purchase_test_1",
            productId = herculesBundle.bundleId,
            productType = "BUNDLE",
            status = PurchaseStatus.PURCHASED,
            currency = "IDR",
            priceDisplay = herculesBundle.priceDisplay,
            rewardSummary = "Hercules Legendary Hero Bundle",
            acknowledged = true,
            consumed = true
        )

        val beforeGold = repository.economyState.value.gold
        val beforeGems = repository.economyState.value.mythGems
        val beforeShards = repository.economyState.value.cardShards["hero_hercules"] ?: 0

        val success = repository.grantEntitlements(record, herculesBundle.contents)
        assertTrue(success)

        val state = repository.economyState.value
        assertTrue("Player must own hero_hercules", state.ownedHeroIds.contains("hero_hercules"))
        assertEquals(beforeGold + 30_000, state.gold)
        assertEquals(beforeGems + 1_000, state.mythGems)
        assertEquals(beforeShards + 50, state.cardShards["hero_hercules"])
    }
}
