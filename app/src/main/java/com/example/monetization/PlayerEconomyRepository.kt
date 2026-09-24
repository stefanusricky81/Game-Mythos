package com.example.monetization

import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Player Economy & Inventory State (Requirements #14, #16, #20).
 */
data class PlayerEconomyState(
    val gold: Int = 25_000,
    val mythGems: Int = 500,
    val ownedHeroIds: Set<String> = setOf(HerculesIdentity.HERO_ID),
    val selectedHeroId: String = HerculesIdentity.HERO_ID,
    val heroProgression: Map<String, Int> = mapOf(HerculesIdentity.HERO_ID to 1),
    val heroShards: Map<String, Int> = mapOf(HerculesIdentity.HERO_ID to 20),
    val ownedCardIds: Set<String> = setOf(
        "c_olympian_guard", "c_titans_wrath", "c_power_strike", "c_heroic_rage",
        "c_nectar_gods", "c_spartan_phalanx", "c_hydra_blade", "c_nemean_hide",
        "c_divine_challenge", "c_zeus_thunderstone", "c_celestial_arrow",
        "c_divine_aegis", "c_athena_blessing", "c_cyclops_hammer", "c_ares_retribution",
        "c_ambrosia_draft", "c_shield_bash", "c_medusa_gaze", "c_cerberus_bite", "c_phoenix_rebirth"
    ),
    val ownedCardCounts: Map<String, Int> = mapOf(
        "c_olympian_guard" to 2, "c_titans_wrath" to 1, "c_power_strike" to 2, "c_heroic_rage" to 2,
        "c_nectar_gods" to 2, "c_spartan_phalanx" to 2, "c_hydra_blade" to 2, "c_nemean_hide" to 2,
        "c_divine_challenge" to 2, "c_zeus_thunderstone" to 1, "c_celestial_arrow" to 2,
        "c_divine_aegis" to 1, "c_athena_blessing" to 1, "c_cyclops_hammer" to 1, "c_ares_retribution" to 1,
        "c_ambrosia_draft" to 2, "c_shield_bash" to 2, "c_medusa_gaze" to 1, "c_cerberus_bite" to 1, "c_phoenix_rebirth" to 1
    ),
    val cardLevels: Map<String, Int> = mapOf(
        "c_olympian_guard" to 1,
        "c_titans_wrath" to 1,
        "c_power_strike" to 1
    ),
    val cardShards: Map<String, Int> = mapOf(
        "hero_hercules" to 20,
        "c_titans_wrath" to 25,
        "c_olympian_guard" to 40,
        "c_power_strike" to 35,
        "c_heroic_rage" to 20,
        "c_nectar_gods" to 15,
        "c_spartan_phalanx" to 20,
        "c_hydra_blade" to 15
    ),
    val activeDeck: ActiveDeck = ActiveDeck.createDefaultOlympusDeck(),
    val ownedBundleIds: Set<String> = emptySet(),
    val ownedCosmeticIds: Set<String> = emptySet(),
    val ownedFrames: Set<String> = setOf("frame_default_bronze"),
    val ownedAvatars: Set<String> = setOf("avatar_default_hercules"),
    val hasMonthlyPass: Boolean = false,
    val hasBattlePass: Boolean = false,
    val pityState: SummonPityState = SummonPityState(),
    val purchaseHistory: List<PurchaseRecord> = emptyList()
)

/**
 * Centralized Entitlement Manager & Player Economy Repository (Requirements #8, #14, #15, #16, #20).
 *
 * Responsibilities:
 * - Grants Gems, Gold, Cards, Shards, Frames, Avatars, Cosmetics, Bundles.
 * - Manages atomic card progression and deck persistence.
 * - Idempotency Enforcement: Prevents duplicate rewards or double-processing purchaseIds.
 */
class PlayerEconomyRepository(
    private val billingProvider: BillingProvider = MockBillingProvider()
) {
    companion object {
        val instance by lazy { PlayerEconomyRepository() }
    }
    private val _economyState = MutableStateFlow(PlayerEconomyState())
    val economyState: StateFlow<PlayerEconomyState> = _economyState.asStateFlow()

    // Idempotency tracking
    private val processedPurchaseIds = mutableSetOf<String>()

    /**
     * Centralized Entitlement Granting (Requirements #14, #21 & #26).
     * Automatically converts duplicate card grants to card shards!
     */
    @Synchronized
    fun grantEntitlements(record: PurchaseRecord, items: List<BundleItem>): Boolean {
        if (processedPurchaseIds.contains(record.purchaseId)) {
            return false
        }
        processedPurchaseIds.add(record.purchaseId)

        _economyState.update { current ->
            var newGold = current.gold
            var newGems = current.mythGems
            val newOwnedCards = current.ownedCardIds.toMutableSet()
            val newCardCounts = current.ownedCardCounts.toMutableMap()
            val newHeroes = current.ownedHeroIds.toMutableSet()
            val newShards = current.cardShards.toMutableMap()
            val newBundles = current.ownedBundleIds.toMutableSet()
            val newCosmetics = current.ownedCosmeticIds.toMutableSet()
            val newFrames = current.ownedFrames.toMutableSet()
            val newAvatars = current.ownedAvatars.toMutableSet()
            var monthlyPass = current.hasMonthlyPass
            var battlePass = current.hasBattlePass

            items.forEach { item ->
                when (item) {
                    is BundleItem.HeroEntitlement -> newHeroes.add(item.heroId)
                    is BundleItem.Gems -> newGems += item.amount
                    is BundleItem.Gold -> newGold += item.amount
                    is BundleItem.Shards -> {
                        val currentAmt = newShards[item.cardId] ?: 0
                        newShards[item.cardId] = currentAmt + item.amount
                    }
                    is BundleItem.SpecificCard -> {
                        addOrDuplicateCardInternal(item.card.id, item.card.rarity, newOwnedCards, newCardCounts, newShards)
                    }
                    is BundleItem.RandomCard -> {
                        val candidates = CardCatalog.ALL_CARDS.filter { it.rarity == item.rarity }
                        val picked = candidates.firstOrNull() ?: candidates.random()
                        addOrDuplicateCardInternal(picked.id, picked.rarity, newOwnedCards, newCardCounts, newShards)
                    }
                    is BundleItem.Frame -> newFrames.add(item.frameId)
                    is BundleItem.Avatar -> newAvatars.add(item.avatarId)
                    is BundleItem.Cosmetic -> newCosmetics.add(item.cosmeticId)
                    is BundleItem.Tokens -> {}
                    is BundleItem.MonthlyPassEntitlement -> monthlyPass = true
                    is BundleItem.BattlePassEntitlement -> battlePass = true
                }
            }

            if (record.productType == "BUNDLE") {
                newBundles.add(record.productId)
            }

            current.copy(
                gold = newGold,
                mythGems = newGems,
                ownedHeroIds = newHeroes,
                ownedCardIds = newOwnedCards,
                ownedCardCounts = newCardCounts,
                cardShards = newShards,
                ownedBundleIds = newBundles,
                ownedCosmeticIds = newCosmetics,
                ownedFrames = newFrames,
                ownedAvatars = newAvatars,
                hasMonthlyPass = monthlyPass,
                hasBattlePass = battlePass,
                purchaseHistory = listOf(record) + current.purchaseHistory
            )
        }
        return true
    }

    /**
     * Handles adding a card or converting duplicate to shards (Requirements #14, #15, #16).
     */
    private fun addOrDuplicateCardInternal(
        cardId: String,
        rarity: CardRarity,
        ownedCardIds: MutableSet<String>,
        ownedCardCounts: MutableMap<String, Int>,
        shardsMap: MutableMap<String, Int>
    ) {
        val currentCount = ownedCardCounts[cardId] ?: 0
        if (ownedCardIds.contains(cardId)) {
            if (currentCount < ActiveDeck.MAX_DUPLICATES_PER_CARD) {
                // Add 2nd copy for deck builder
                ownedCardCounts[cardId] = currentCount + 1
            } else {
                // Duplicate converts to shards
                val shardsToAdd = ShardConversion.getShardsForDuplicate(rarity)
                val curShards = shardsMap[cardId] ?: 0
                shardsMap[cardId] = curShards + shardsToAdd
            }
        } else {
            ownedCardIds.add(cardId)
            ownedCardCounts[cardId] = 1
        }
    }

    /**
     * Atomic Card Upgrade (Requirements #7, #8).
     * Deducts Gold and Card Shards, increments Card Level, updates stats.
     */
    @Synchronized
    fun upgradeCard(cardId: String): Result<CardProgressionStep> {
        val state = _economyState.value
        val cardDef = CardCatalog.findDefinition(cardId)
            ?: return Result.failure(IllegalArgumentException("Card definition not found for '$cardId'."))

        val currentLevel = state.cardLevels[cardId] ?: 1
        if (currentLevel >= cardDef.maxLevel) {
            return Result.failure(IllegalStateException("${cardDef.name} is already at maximum level ($currentLevel)."))
        }

        val upgradeCost = cardDef.getUpgradeCost(currentLevel)
            ?: return Result.failure(IllegalStateException("No upgrade configuration available for level $currentLevel."))

        val (goldCost, shardCost) = upgradeCost

        val currentShards = state.cardShards[cardId] ?: 0
        if (state.gold < goldCost) {
            return Result.failure(IllegalStateException("Insufficient Gold. Need ${goldCost - state.gold} more Gold."))
        }
        if (currentShards < shardCost) {
            return Result.failure(IllegalStateException("Insufficient Shards. Need ${shardCost - currentShards} more Shards."))
        }

        val nextLevel = currentLevel + 1
        val nextStep = cardDef.progression[nextLevel]
            ?: return Result.failure(IllegalStateException("Progression data missing for level $nextLevel."))

        // Atomic state mutation
        _economyState.update { current ->
            val updatedShards = current.cardShards.toMutableMap()
            updatedShards[cardId] = (updatedShards[cardId] ?: 0) - shardCost

            val updatedLevels = current.cardLevels.toMutableMap()
            updatedLevels[cardId] = nextLevel

            current.copy(
                gold = current.gold - goldCost,
                cardShards = updatedShards,
                cardLevels = updatedLevels
            )
        }

        return Result.success(nextStep)
    }

    /**
     * Saves Active Deck with validation (Requirements #10, #11).
     */
    @Synchronized
    fun saveActiveDeck(deck: ActiveDeck): DeckValidationResult {
        val validation = DeckValidator.validate(deck, _economyState.value.ownedCardCounts)
        if (validation.isValid) {
            _economyState.update { it.copy(activeDeck = deck) }
        }
        return validation
    }

    /**
     * Select Active Hero (Requirements #17, #18).
     */
    @Synchronized
    fun selectHero(heroId: String): Boolean {
        if (!_economyState.value.ownedHeroIds.contains(heroId)) {
            return false
        }
        _economyState.update { current ->
            current.copy(
                selectedHeroId = heroId,
                activeDeck = current.activeDeck.copy(heroId = heroId)
            )
        }
        return true
    }

    /**
     * Awards Battle Victory Rewards to inventory and collection (Requirement #16).
     */
    @Synchronized
    fun claimBattleVictoryRewards(rewards: BattleRewards) {
        _economyState.update { current ->
            val newGold = current.gold + rewards.gold
            val newOwnedCards = current.ownedCardIds.toMutableSet()
            val newCardCounts = current.ownedCardCounts.toMutableMap()
            val newShards = current.cardShards.toMutableMap()

            // Resolve reward card (e.g. Divine Aegis of Olympus)
            val rewardCardId = "c_divine_aegis"
            addOrDuplicateCardInternal(rewardCardId, rewards.cardRewardRarity, newOwnedCards, newCardCounts, newShards)

            current.copy(
                gold = newGold,
                ownedCardIds = newOwnedCards,
                ownedCardCounts = newCardCounts,
                cardShards = newShards
            )
        }
    }

    suspend fun purchaseGemProduct(
        product: GemProduct,
        simulationMode: MockBillingResult = MockBillingResult.SUCCESS
    ): PurchaseResult {
        val grantedItems = listOf(
            BundleItem.Gems(product.totalGems)
        )

        val result = billingProvider.initiatePurchase(
            productId = product.productId,
            productType = "GEM",
            priceDisplay = product.priceDisplay,
            grantedItems = grantedItems,
            simulationMode = simulationMode
        )

        if (result is PurchaseResult.Success) {
            grantEntitlements(result.record, grantedItems)
        }

        return result
    }

    suspend fun purchaseBundle(
        bundle: PremiumBundle,
        simulationMode: MockBillingResult = MockBillingResult.SUCCESS
    ): PurchaseResult {
        if (bundle.isOneTime && _economyState.value.ownedBundleIds.contains(bundle.bundleId)) {
            return PurchaseResult.Failed("You already own this one-time bundle.")
        }

        val result = billingProvider.initiatePurchase(
            productId = bundle.bundleId,
            productType = "BUNDLE",
            priceDisplay = bundle.priceDisplay,
            grantedItems = bundle.contents,
            simulationMode = simulationMode
        )

        if (result is PurchaseResult.Success) {
            grantEntitlements(result.record, bundle.contents)
        }

        return result
    }

    suspend fun purchaseDirectCard(
        card: Card,
        simulationMode: MockBillingResult = MockBillingResult.SUCCESS
    ): PurchaseResult {
        val priceDisplay = DirectCardPricing.getPriceDisplay(card.rarity)
            ?: return PurchaseResult.Failed("This card rarity cannot be directly purchased.")

        val grantedItems = listOf(BundleItem.SpecificCard(card))

        val result = billingProvider.initiatePurchase(
            productId = card.id,
            productType = "CARD",
            priceDisplay = priceDisplay,
            grantedItems = grantedItems,
            simulationMode = simulationMode
        )

        if (result is PurchaseResult.Success) {
            grantEntitlements(result.record, grantedItems)
        }

        return result
    }

    suspend fun purchaseCosmetic(
        cosmetic: CosmeticItem,
        simulationMode: MockBillingResult = MockBillingResult.SUCCESS
    ): PurchaseResult {
        if (_economyState.value.ownedCosmeticIds.contains(cosmetic.id)) {
            return PurchaseResult.Failed("Cosmetic already unlocked.")
        }

        if (cosmetic.priceGems != null) {
            if (_economyState.value.mythGems < cosmetic.priceGems) {
                return PurchaseResult.Failed("Insufficient Myth Gems. Need ${cosmetic.priceGems - _economyState.value.mythGems} more.")
            }
            _economyState.update { current ->
                current.copy(
                    mythGems = current.mythGems - cosmetic.priceGems,
                    ownedCosmeticIds = current.ownedCosmeticIds + cosmetic.id
                )
            }
            val record = PurchaseRecord(
                purchaseId = "gem_cosmetic_${System.currentTimeMillis()}",
                productId = cosmetic.id,
                productType = "COSMETIC",
                status = PurchaseStatus.PURCHASED,
                currency = "GEMS",
                priceDisplay = cosmetic.priceDisplay,
                rewardSummary = cosmetic.name,
                acknowledged = true,
                consumed = true
            )
            return PurchaseResult.Success(record, listOf(BundleItem.Cosmetic(cosmetic.id, cosmetic.name)))
        }

        val grantedItems = listOf(BundleItem.Cosmetic(cosmetic.id, cosmetic.name))
        val result = billingProvider.initiatePurchase(
            productId = cosmetic.id,
            productType = "COSMETIC",
            priceDisplay = cosmetic.priceDisplay,
            grantedItems = grantedItems,
            simulationMode = simulationMode
        )

        if (result is PurchaseResult.Success) {
            grantEntitlements(result.record, grantedItems)
        }

        return result
    }

    fun performSummon(count: Int): Pair<Boolean, List<SummonedCardResult>> {
        val cost = if (count == 1) SummonRates.SINGLE_SUMMON_COST_GEMS else SummonRates.TEN_SUMMON_COST_GEMS
        val currentGems = _economyState.value.mythGems
        if (currentGems < cost) {
            return Pair(false, emptyList())
        }

        val (pulledCards, newPity) = SummonEngine.executeSummon(
            count = count,
            currentPity = _economyState.value.pityState,
            ownedCardIds = _economyState.value.ownedCardIds
        )

        _economyState.update { current ->
            var newGems = current.mythGems - cost
            val newOwnedCards = current.ownedCardIds.toMutableSet()
            val newCardCounts = current.ownedCardCounts.toMutableMap()
            val newShards = current.cardShards.toMutableMap()

            pulledCards.forEach { pull ->
                addOrDuplicateCardInternal(pull.card.id, pull.card.rarity, newOwnedCards, newCardCounts, newShards)
            }

            current.copy(
                mythGems = newGems,
                ownedCardIds = newOwnedCards,
                ownedCardCounts = newCardCounts,
                cardShards = newShards,
                pityState = newPity
            )
        }

        return Pair(true, pulledCards)
    }

    // DEBUG / DEVELOPMENT CHEATS
    fun debugAddGems(amount: Int) {
        _economyState.update { it.copy(mythGems = it.mythGems + amount) }
    }

    fun debugAddGold(amount: Int) {
        _economyState.update { it.copy(gold = it.gold + amount) }
    }

    fun debugAddShards(cardId: String, amount: Int) {
        _economyState.update { current ->
            val updated = current.cardShards.toMutableMap()
            updated[cardId] = (updated[cardId] ?: 0) + amount
            current.copy(cardShards = updated)
        }
    }

    fun debugResetEconomy() {
        processedPurchaseIds.clear()
        _economyState.value = PlayerEconomyState()
    }
}
