package com.example.monetization

import com.example.data.CardRarity
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * Purchase Status Enum (Requirement #20).
 */
enum class PurchaseStatus {
    PENDING,
    PURCHASED,
    FAILED,
    CANCELLED,
    REFUNDED
}

/**
 * Purchase / Transaction Record Data Model (Requirement #20).
 * Compatible with future Google Play Billing purchase history & token validation.
 */
data class PurchaseRecord(
    val purchaseId: String,
    val productId: String,
    val productType: String, // "GEM", "BUNDLE", "CARD", "COSMETIC"
    val status: PurchaseStatus,
    val currency: String = "IDR",
    val priceDisplay: String,
    val rewardSummary: String,
    val timestamp: Long = System.currentTimeMillis(),
    val acknowledged: Boolean = false,
    val consumed: Boolean = false
)

/**
 * Mock Simulation Modes for testing during development (Requirement #18, #36).
 */
enum class MockBillingResult {
    SUCCESS,
    CANCELLED,
    PENDING,
    FAILED
}

/**
 * Result of a purchase operation.
 */
sealed class PurchaseResult {
    data class Success(
        val record: PurchaseRecord,
        val grantedItems: List<BundleItem>
    ) : PurchaseResult()

    data class Cancelled(
        val message: String = "Purchase cancelled."
    ) : PurchaseResult()

    data class Pending(
        val record: PurchaseRecord,
        val message: String = "Purchase is pending verification."
    ) : PurchaseResult()

    data class Failed(
        val message: String = "Purchase failed. Please try again."
    ) : PurchaseResult()
}

/**
 * Billing Provider Interface (Requirement #18, #27).
 * Clean abstraction separating UI from payment gateway implementation.
 */
interface BillingProvider {
    suspend fun initiatePurchase(
        productId: String,
        productType: String,
        priceDisplay: String,
        grantedItems: List<BundleItem>,
        simulationMode: MockBillingResult = MockBillingResult.SUCCESS
    ): PurchaseResult

    suspend fun restorePurchases(): List<PurchaseRecord>
    suspend fun acknowledgePurchase(purchaseId: String): Boolean
    suspend fun consumePurchase(purchaseId: String): Boolean
}

/**
 * Mock Billing Provider for Prototype Phase (Requirement #18).
 * Simulates real payment latency and the 4 primary billing states:
 * SUCCESS, CANCELLED, PENDING, FAILED.
 */
class MockBillingProvider : BillingProvider {

    private val transactionHistory = mutableListOf<PurchaseRecord>()

    override suspend fun initiatePurchase(
        productId: String,
        productType: String,
        priceDisplay: String,
        grantedItems: List<BundleItem>,
        simulationMode: MockBillingResult
    ): PurchaseResult {
        // Simulate real store latency (400ms)
        delay(400)

        val purchaseId = "mythos_mock_${UUID.randomUUID()}"
        val summary = grantedItems.joinToString(", ") { item ->
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

        return when (simulationMode) {
            MockBillingResult.SUCCESS -> {
                val record = PurchaseRecord(
                    purchaseId = purchaseId,
                    productId = productId,
                    productType = productType,
                    status = PurchaseStatus.PURCHASED,
                    priceDisplay = priceDisplay,
                    rewardSummary = summary,
                    acknowledged = true,
                    consumed = true
                )
                transactionHistory.add(record)
                PurchaseResult.Success(record, grantedItems)
            }
            MockBillingResult.CANCELLED -> {
                val record = PurchaseRecord(
                    purchaseId = purchaseId,
                    productId = productId,
                    productType = productType,
                    status = PurchaseStatus.CANCELLED,
                    priceDisplay = priceDisplay,
                    rewardSummary = summary
                )
                transactionHistory.add(record)
                PurchaseResult.Cancelled("Purchase was cancelled by user.")
            }
            MockBillingResult.PENDING -> {
                val record = PurchaseRecord(
                    purchaseId = purchaseId,
                    productId = productId,
                    productType = productType,
                    status = PurchaseStatus.PENDING,
                    priceDisplay = priceDisplay,
                    rewardSummary = summary
                )
                transactionHistory.add(record)
                PurchaseResult.Pending(record, "Purchase is pending bank verification.")
            }
            MockBillingResult.FAILED -> {
                val record = PurchaseRecord(
                    purchaseId = purchaseId,
                    productId = productId,
                    productType = productType,
                    status = PurchaseStatus.FAILED,
                    priceDisplay = priceDisplay,
                    rewardSummary = summary
                )
                transactionHistory.add(record)
                PurchaseResult.Failed("Purchase declined by provider. Please try again.")
            }
        }
    }

    override suspend fun restorePurchases(): List<PurchaseRecord> {
        delay(200)
        return transactionHistory.filter { it.status == PurchaseStatus.PURCHASED }
    }

    override suspend fun acknowledgePurchase(purchaseId: String): Boolean {
        val index = transactionHistory.indexOfFirst { it.purchaseId == purchaseId }
        if (index != -1) {
            transactionHistory[index] = transactionHistory[index].copy(acknowledged = true)
            return true
        }
        return false
    }

    override suspend fun consumePurchase(purchaseId: String): Boolean {
        val index = transactionHistory.indexOfFirst { it.purchaseId == purchaseId }
        if (index != -1) {
            transactionHistory[index] = transactionHistory[index].copy(consumed = true)
            return true
        }
        return false
    }
}

/**
 * Future Google Play Billing Integration Stub (Requirement #27, #28).
 * Ready to be connected to com.android.billingclient.api when backend token verification is live.
 */
class GooglePlayBillingProviderStub : BillingProvider {
    override suspend fun initiatePurchase(
        productId: String,
        productType: String,
        priceDisplay: String,
        grantedItems: List<BundleItem>,
        simulationMode: MockBillingResult
    ): PurchaseResult {
        // Will delegate to BillingClient.launchBillingFlow(activity, billingFlowParams)
        throw UnsupportedOperationException("Google Play Billing requires backend token verification server.")
    }

    override suspend fun restorePurchases(): List<PurchaseRecord> = emptyList()
    override suspend fun acknowledgePurchase(purchaseId: String): Boolean = true
    override suspend fun consumePurchase(purchaseId: String): Boolean = true
}
