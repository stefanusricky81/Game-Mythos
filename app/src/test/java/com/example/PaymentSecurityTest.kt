package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.monetization.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression tests for the payment security audit fixes:
 *  - the double-grant bug (in-memory-only processedPurchaseIds not surviving app restart)
 *  - one-time bundle / cosmetic entitlements not being re-grantable
 *  - direct-card duplicate purchases converting to shards instead of silently no-oping
 *
 * What this file deliberately does NOT cover (documented rather than faked):
 *  - GooglePlayBillingProvider's VerificationOutcome state-machine handling itself, since that
 *    class is tightly coupled to the real android.billingclient BillingClient (a Google-owned
 *    class not designed for easy mocking without Mockito's inline mock maker, which isn't a
 *    project dependency). That logic is covered by manual verification against the compiled
 *    bytecode/code review, and should additionally be exercised via Play Console license
 *    testing before release (see SETUP_PLAY_BILLING.md).
 *  - PlayerIdentity's Firebase Anonymous Auth call, which needs a real (or emulated) Firebase
 *    project - not available in a local Robolectric unit test without google-services.json.
 *  - The Cloud Function's idempotency/race-condition/ownership-conflict logic, which has its
 *    own dedicated Jest suite in functions/src/playBilling.test.ts (20 passing tests).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PaymentSecurityTest {

    private lateinit var context: Context
    private lateinit var repository: PlayerEconomyRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("mythos_player_data", Context.MODE_PRIVATE).edit().clear().commit()
        repository = PlayerEconomyRepository.instance
        repository.resetForTesting(context)
    }

    // Scenario 12/13: app restart after credit before consume / reconciliation after restart.
    // This is the exact regression test for the PRODUCTION BLOCKER found in the security audit:
    // processedPurchaseIds used to be in-memory only, so a purchase already granted and
    // persisted could be granted AGAIN if reconcileUnconsumedPurchases() re-verified the same
    // still-unconsumed token after a process restart.
    @Test
    fun testGrantedPurchaseIsNotRegrantedAfterSimulatedRestart() {
        val record = PurchaseRecord(
            purchaseId = "restart_test_token_abc",
            productId = "gem_starter",
            productType = "GEM",
            status = PurchaseStatus.PURCHASED,
            priceDisplay = "Rp9.900",
            rewardSummary = "100 Myth Gems",
            acknowledged = true,
            consumed = true
        )
        val items = listOf(BundleItem.Gems(100))

        assertTrue("First grant for a new purchaseId must succeed", repository.grantEntitlements(record, items))
        val gemsAfterFirstGrant = repository.economyState.value.mythGems

        // Simulate an app restart: fresh in-memory state, reloaded from the SAME
        // SharedPreferences the first grant persisted to (this is exactly what
        // PlayerEconomyRepository.initPersistence() does in MainActivity.onCreate()).
        repository.resetForTesting(context)
        assertEquals(
            "Persisted gems must survive the simulated restart",
            gemsAfterFirstGrant,
            repository.economyState.value.mythGems
        )

        val secondGrant = repository.grantEntitlements(record, items)
        assertFalse(
            "A purchase already granted+persisted before 'restart' must NOT be granted again " +
                "when reconciliation re-verifies the same purchaseId",
            secondGrant
        )
        assertEquals(
            "Gems must not increase a second time for the same purchaseId",
            gemsAfterFirstGrant,
            repository.economyState.value.mythGems
        )
    }

    // Scenario 14: bundle one-time entitlement.
    @Test
    fun testOneTimeBundleCannotBeRepurchased() = runBlocking {
        val repo = PlayerEconomyRepository(MockBillingProvider())
        val starterPack = MonetizationCatalog.BUNDLES.first { it.bundleId == "bundle_starter_pack" }
        assertTrue("Test assumes bundle_starter_pack is configured as one-time", starterPack.isOneTime)

        val first = repo.purchaseBundle(starterPack)
        assertTrue("First purchase of a one-time bundle must succeed", first is PurchaseResult.Success)

        val second = repo.purchaseBundle(starterPack)
        assertTrue(
            "Repurchasing a one-time bundle already owned must be rejected before even reaching billing",
            second is PurchaseResult.Failed
        )
    }

    // Scenario 15: cosmetic one-time entitlement.
    @Test
    fun testCosmeticCannotBeRepurchasedOnceOwned() = runBlocking {
        val repo = PlayerEconomyRepository(MockBillingProvider())
        val skin = MonetizationCatalog.COSMETICS.first { it.id == "skin_golden_hercules" }

        val first = repo.purchaseCosmetic(skin)
        assertTrue("First purchase of a cosmetic must succeed", first is PurchaseResult.Success)

        val second = repo.purchaseCosmetic(skin)
        assertTrue(
            "Repurchasing a cosmetic already owned must be rejected",
            second is PurchaseResult.Failed
        )
    }

    // Scenario 16: direct card duplicate -> shard behavior.
    @Test
    fun testDirectCardPurchaseOfAlreadyOwnedCardConvertsToShards() {
        val repo = PlayerEconomyRepository(MockBillingProvider())
        val card = MonetizationCatalog.getDirectCardOffers().first { it.id == "c_titans_wrath" }
        assertTrue(
            "Test assumes this card starts already owned by a fresh player (matches default state)",
            repo.economyState.value.ownedCardIds.contains(card.id)
        )
        val shardsBefore = repo.economyState.value.cardShards[card.id] ?: 0

        val record = PurchaseRecord(
            purchaseId = "duplicate_card_purchase",
            productId = card.id,
            productType = "CARD",
            status = PurchaseStatus.PURCHASED,
            priceDisplay = "Rp99.900",
            rewardSummary = card.name,
            acknowledged = true,
            consumed = true
        )
        assertTrue(repo.grantEntitlements(record, listOf(BundleItem.SpecificCard(card))))

        val shardsAfter = repo.economyState.value.cardShards[card.id] ?: 0
        assertTrue(
            "Buying a card the player already owns must convert the duplicate into shards, " +
                "not silently do nothing",
            shardsAfter > shardsBefore
        )
    }
}
