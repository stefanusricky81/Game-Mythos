package com.example.monetization

import android.app.Activity
import android.content.Context
import com.example.MythosConfig
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.consumePurchase
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Real Google Play Billing Library integration, replacing [MockBillingProvider] for release
 * builds. Purchases now go through actual Play Billing, get checked against the Play
 * Developer API on the server (see FirebasePurchaseVerifier + functions/src/playBilling.ts),
 * and are only granted to the player once that check passes.
 *
 * The [BillingProvider] interface already carries `grantedItems` from the call site (e.g.
 * `PlayerEconomyRepository.purchaseGemProduct`), so this class's only job is to confirm the
 * purchase genuinely happened and then hand those same items back - it never decides on its
 * own what a productId is worth.
 *
 * Single-activity app, so instead of threading an Activity through the shared
 * [BillingProvider] interface (which would also touch MockBillingProvider and the debug
 * stub), the current Activity is supplied lazily via [activityProvider] and refreshed on
 * every onCreate (see MainActivity / PlayerEconomyRepository.configureBilling).
 */
class GooglePlayBillingProvider(
    private val appContext: Context,
    private val activityProvider: () -> Activity?,
    private val verifier: FirebasePurchaseVerifier = FirebasePurchaseVerifier(),
    private val onExternalGrant: (PurchaseRecord, List<BundleItem>) -> Unit,
    // Called once per connection with productId -> Play's own formattedPrice (e.g. "Rp9.900"),
    // for every ID in BillingCatalog.allProductIds() Play actually has a live offer for. Lets
    // the shop UI show what the player will really be charged instead of only the hand-written
    // Rp... strings in MonetizationCatalog.kt, which are business metadata (what a product
    // grants), not a promise about what Play will actually charge for it.
    private val onLivePricesUpdated: (Map<String, String>) -> Unit = {}
) : BillingProvider, PurchasesUpdatedListener {

    private data class PurchasesUpdatedEvent(val responseCode: Int, val purchases: List<Purchase>)

    private var billingClient: BillingClient? = null
    private var connectionState = CompletableDeferred<Boolean>()
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val productDetailsCache = mutableMapOf<String, ProductDetails>()

    // Play Billing only supports one flow at a time; this both serializes initiatePurchase
    // calls and correlates launchBillingFlow with the PurchasesUpdatedListener callback that
    // follows it (the listener has no other way to know which call it belongs to).
    private val purchaseMutex = Mutex()
    private var pendingPurchaseResult: CompletableDeferred<PurchasesUpdatedEvent>? = null

    fun connect() {
        if (billingClient?.isReady == true) return
        val client = BillingClient.newBuilder(appContext)
            .setListener(this)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build()
        billingClient = client
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                val ready = billingResult.responseCode == BillingClient.BillingResponseCode.OK
                if (!connectionState.isCompleted) connectionState.complete(ready)
                if (ready) {
                    ioScope.launch {
                        // Picks up purchases Google settled while the app was closed, or that
                        // completed after a process death between charge and consume/grant.
                        reconcileUnconsumedPurchases()
                        // Recovers entitlements the SERVER credited to this player UID but that
                        // never made it into local SharedPreferences (e.g. local data loss while
                        // the Firebase Anonymous Auth identity survived - see PlayerIdentity's
                        // documented limitation for what this does NOT cover, namely reinstall).
                        if (MythosConfig.SERVER_VERIFICATION_ENABLED) syncServerEntitlements()
                        // Best-effort: the shop can still show catalog fallback prices if this
                        // fails or hasn't completed yet (see ShopViewModel/ShopScreen).
                        refreshLivePrices()
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                if (connectionState.isCompleted) connectionState = CompletableDeferred()
            }
        })
    }

    fun disconnect() {
        billingClient?.endConnection()
        billingClient = null
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        val deferred = pendingPurchaseResult ?: return
        pendingPurchaseResult = null
        deferred.complete(PurchasesUpdatedEvent(billingResult.responseCode, purchases?.toList().orEmpty()))
    }

    override suspend fun initiatePurchase(
        productId: String,
        productType: String,
        priceDisplay: String,
        grantedItems: List<BundleItem>,
        simulationMode: MockBillingResult
    ): PurchaseResult = purchaseMutex.withLock {
        val activity = activityProvider()
            ?: return@withLock PurchaseResult.Failed("Game window is not ready. Please try again.")
        val client = billingClient
            ?: return@withLock PurchaseResult.Failed("Google Play Billing is not initialized.")
        if (connectionState.await() != true) {
            return@withLock PurchaseResult.Failed("Could not connect to Google Play Billing.")
        }

        val productDetails = queryProductDetails(client, productId)
            ?: return@withLock PurchaseResult.Failed(
                "'$productId' is not available for purchase on Google Play yet."
            )
        val offerToken = productDetails.oneTimePurchaseOfferDetailsList?.firstOrNull()?.offerToken
            ?: return@withLock PurchaseResult.Failed("Google Play has no active offer for '$productId' right now.")

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()

        val deferred = CompletableDeferred<PurchasesUpdatedEvent>()
        pendingPurchaseResult = deferred
        val launchResult = client.launchBillingFlow(activity, flowParams)
        if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
            pendingPurchaseResult = null
            return@withLock PurchaseResult.Failed(
                "Could not start Google Play purchase (code ${launchResult.responseCode})."
            )
        }

        val event = deferred.await()
        if (event.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            return@withLock PurchaseResult.Cancelled()
        }
        if (event.responseCode != BillingClient.BillingResponseCode.OK) {
            return@withLock PurchaseResult.Failed("Google Play purchase failed (code ${event.responseCode}).")
        }
        val purchase = event.purchases.firstOrNull { it.products.contains(productId) }
            ?: return@withLock PurchaseResult.Failed("Purchase result did not match '$productId'.")

        if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            // e.g. a cash/QRIS payment method awaiting confirmation. Google will redeliver
            // this purchase (as PURCHASED) via reconcileUnconsumedPurchases() once it settles.
            return@withLock PurchaseResult.Pending(
                record = PurchaseRecord(
                    purchaseId = purchase.orderId ?: purchase.purchaseToken,
                    productId = productId,
                    productType = productType,
                    status = PurchaseStatus.PENDING,
                    priceDisplay = priceDisplay,
                    rewardSummary = grantedItems.describeForBilling()
                )
            )
        }
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            return@withLock PurchaseResult.Failed("Purchase was not completed.")
        }

        // CreditedNow and AlreadyCredited both proceed to grant: whether this device is the
        // first to ever credit this token, or is retrying after e.g. a failed consume() last
        // time, only matters for the SERVER's bookkeeping. Whether local state actually gets
        // mutated a second time is decided by PlayerEconomyRepository.grantEntitlements()'s own
        // persisted purchaseId ledger, not by this status - see that function's docs for why
        // gating on "AlreadyCredited => never grant" here would risk a paid-but-never-delivered
        // purchase if THIS device's earlier attempt never got as far as actually granting.
        // Optional extra layer (see MythosConfig.SERVER_VERIFICATION_ENABLED): without it, the
        // PURCHASED state Play Billing itself reported above is what we act on. Google has
        // already taken the payment by the time a purchase reaches PURCHASED.
        if (MythosConfig.SERVER_VERIFICATION_ENABLED) {
            when (verifier.verify(productId, purchase.purchaseToken)) {
                is VerificationOutcome.CreditedNow, is VerificationOutcome.AlreadyCredited -> Unit
                VerificationOutcome.Pending -> return@withLock PurchaseResult.Pending(
                    record = PurchaseRecord(
                        purchaseId = purchase.orderId ?: purchase.purchaseToken,
                        productId = productId,
                        productType = productType,
                        status = PurchaseStatus.PENDING,
                        priceDisplay = priceDisplay,
                        rewardSummary = grantedItems.describeForBilling()
                    )
                )
                VerificationOutcome.Cancelled -> return@withLock PurchaseResult.Cancelled()
                VerificationOutcome.Invalid -> return@withLock PurchaseResult.Failed(
                    "This purchase could not be verified. If you were charged, contact support " +
                        "with order ${purchase.orderId ?: "unknown"}."
                )
                is VerificationOutcome.Error -> return@withLock PurchaseResult.Failed(
                    "Could not verify your purchase with our server. If you were charged, contact " +
                        "support with order ${purchase.orderId ?: "unknown"}."
                )
            }
        }

        val record = PurchaseRecord(
            purchaseId = purchase.orderId ?: purchase.purchaseToken,
            productId = productId,
            productType = productType,
            status = PurchaseStatus.PURCHASED,
            priceDisplay = priceDisplay,
            rewardSummary = grantedItems.describeForBilling(),
            acknowledged = true,
            consumed = true
        )

        // GRANT FIRST, THEN CONSUME. The reward is written to SharedPreferences (commit()) before
        // the purchase is marked consumed on Google's side. If the app dies before the grant, the
        // purchase is still unconsumed and reconcileUnconsumedPurchases() grants it on next
        // launch; if it dies after the grant but before consume, the persisted purchaseId ledger
        // makes the retry a no-op and consume is simply retried. Consuming first (the old order)
        // could lose a paid purchase in the gap between the two steps. grantEntitlements() is
        // idempotent, so the caller granting the same record again afterwards changes nothing.
        onExternalGrant(record, grantedItems)
        consumeInternal(client, purchase.purchaseToken)
        PurchaseResult.Success(record, grantedItems)
    }

    override suspend fun restorePurchases(): List<PurchaseRecord> = reconcileUnconsumedPurchases()

    // Already acknowledged/consumed inline as part of initiatePurchase / reconciliation.
    override suspend fun acknowledgePurchase(purchaseId: String): Boolean = true
    override suspend fun consumePurchase(purchaseId: String): Boolean = true

    /**
     * Verifies and grants any PURCHASED-but-unconsumed purchase Play Billing still knows
     * about. Runs once per successful connection and is also what backs "Restore Purchases".
     */
    private suspend fun reconcileUnconsumedPurchases(): List<PurchaseRecord> {
        val client = billingClient ?: return emptyList()
        if (connectionState.await() != true) return emptyList()

        val purchases = suspendCancellableCoroutine<List<Purchase>> { continuation ->
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
            client.queryPurchasesAsync(params) { billingResult, result ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(result) { _, _, _ -> }
                } else {
                    continuation.resume(emptyList()) { _, _, _ -> }
                }
            }
        }

        val records = mutableListOf<PurchaseRecord>()
        for (purchase in purchases) {
            if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) continue
            val productId = purchase.products.firstOrNull() ?: continue
            val entry = BillingCatalog.entryFor(productId) ?: continue

            if (MythosConfig.SERVER_VERIFICATION_ENABLED) {
                val outcome = verifier.verify(productId, purchase.purchaseToken)
                val isCredited = outcome is VerificationOutcome.CreditedNow || outcome is VerificationOutcome.AlreadyCredited
                if (!isCredited) continue // Pending/Cancelled/Invalid/Error: never grant, try again later
            }

            val record = PurchaseRecord(
                purchaseId = purchase.orderId ?: purchase.purchaseToken,
                productId = entry.productId,
                productType = entry.productType,
                status = PurchaseStatus.PURCHASED,
                priceDisplay = entry.priceDisplay,
                rewardSummary = entry.grantedItems.describeForBilling(),
                acknowledged = true,
                consumed = true
            )
            // Grant (persisted, idempotent) BEFORE consume - see initiatePurchase.
            onExternalGrant(record, entry.grantedItems)
            consumeInternal(client, purchase.purchaseToken)
            records.add(record)
        }
        return records
    }

    private suspend fun consumeInternal(client: BillingClient, purchaseToken: String) {
        try {
            withContext(Dispatchers.IO) {
                client.consumePurchase(ConsumeParams.newBuilder().setPurchaseToken(purchaseToken).build())
            }
        } catch (e: Exception) {
            // Not fatal: the purchase was already verified & granted. An unconsumed token
            // simply gets picked up again by reconcileUnconsumedPurchases() next connection -
            // the server-side token-hash guard stops the SERVER crediting it twice, and
            // PlayerEconomyRepository's own persisted purchaseId ledger stops this DEVICE
            // applying the reward to local state twice.
        }
    }

    /**
     * Applies any entitlement the server has on record for this player UID but that isn't yet
     * reflected locally - see the class doc on why AlreadyCredited/CreditedNow both flow through
     * grantEntitlements()'s own idempotency check rather than being gated here.
     */
    private suspend fun syncServerEntitlements() {
        val entitlements = verifier.fetchEntitlements()
        for (entitlement in entitlements) {
            val entry = BillingCatalog.entryFor(entitlement.productId) ?: continue
            val record = PurchaseRecord(
                purchaseId = entitlement.orderId ?: entitlement.purchaseTokenHash,
                productId = entry.productId,
                productType = entry.productType,
                status = PurchaseStatus.PURCHASED,
                priceDisplay = entry.priceDisplay,
                rewardSummary = entry.grantedItems.describeForBilling(),
                acknowledged = true,
                consumed = true
            )
            onExternalGrant(record, entry.grantedItems)
        }
    }

    private suspend fun queryProductDetails(client: BillingClient, productId: String): ProductDetails? {
        productDetailsCache[productId]?.let { return it }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()
        val details = suspendCancellableCoroutine<ProductDetails?> { continuation ->
            client.queryProductDetailsAsync(params) { billingResult, result ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(result.productDetailsList.firstOrNull()) { _, _, _ -> }
                } else {
                    continuation.resume(null) { _, _, _ -> }
                }
            }
        }
        if (details != null) productDetailsCache[productId] = details
        return details
    }

    /**
     * Batch-fetches ProductDetails for every catalog product in one call and publishes their
     * live formattedPrice via [onLivePricesUpdated]. Also warms [productDetailsCache] so a
     * purchase started right after this doesn't need its own extra round trip.
     */
    private suspend fun refreshLivePrices() {
        val client = billingClient ?: return
        val productIds = BillingCatalog.allProductIds()
        if (productIds.isEmpty()) return

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                productIds.map { id ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(id)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                }
            )
            .build()
        val allDetails = suspendCancellableCoroutine<List<ProductDetails>> { continuation ->
            client.queryProductDetailsAsync(params) { billingResult, result ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(result.productDetailsList) { _, _, _ -> }
                } else {
                    continuation.resume(emptyList()) { _, _, _ -> }
                }
            }
        }

        val prices = mutableMapOf<String, String>()
        for (details in allDetails) {
            productDetailsCache[details.productId] = details
            val formatted = details.oneTimePurchaseOfferDetailsList?.firstOrNull()?.formattedPrice
            if (formatted != null) prices[details.productId] = formatted
        }
        if (prices.isNotEmpty()) onLivePricesUpdated(prices)
    }
}
