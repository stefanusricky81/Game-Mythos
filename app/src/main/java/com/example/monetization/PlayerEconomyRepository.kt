package com.example.monetization

import android.content.Context
import android.content.SharedPreferences
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject

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
    val heroXp: Map<String, Int> = mapOf(HerculesIdentity.HERO_ID to 0),
    val playerProgress: PlayerProgress = PlayerProgress(),
    val dailyQuests: List<DailyQuest> = DailyQuestConfig.createDefaultQuests(),
    val dailyQuestDate: String = "",
    val loginRewardDay: Int = 1,
    val lastLoginRewardDate: String? = null,
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
    val highestUnlockedStage: Int = 1,
    val completedStageIds: Set<String> = emptySet(),
    val stageStars: Map<String, Int> = emptyMap(),
    val ownedBundleIds: Set<String> = emptySet(),
    val ownedCosmeticIds: Set<String> = emptySet(),
    val ownedFrames: Set<String> = setOf("frame_default_bronze"),
    val ownedAvatars: Set<String> = setOf("avatar_default_hercules"),
    val hasMonthlyPass: Boolean = false,
    val hasBattlePass: Boolean = false,
    val pityState: SummonPityState = SummonPityState(),
    val purchaseHistory: List<PurchaseRecord> = emptyList()
) {
    /**
     * Retrieves a single unified CardInstance for a specific card.
     * (Phase 6A Requirement #4)
     */
    fun getCardInstance(cardId: String): CardInstance? {
        val def = CardCatalog.getDefinition(cardId) ?: return null
        return CardInstance(
            cardId = cardId,
            quantity = ownedCardCounts[cardId] ?: 0,
            level = cardLevels[cardId] ?: 1,
            shards = cardShards[cardId] ?: 0,
            isUnlocked = ownedCardIds.contains(cardId),
            definition = def
        )
    }

    /**
     * Retrieves unified HeroProgress for a specific hero (Phase 7C Section 3).
     */
    fun getHeroProgress(heroId: String): HeroProgress {
        val level = heroProgression[heroId] ?: 1
        val xp = heroXp[heroId] ?: 0
        val shards = heroShards[heroId] ?: 0
        val isUnlocked = ownedHeroIds.contains(heroId)
        return HeroProgress(
            heroId = heroId,
            level = level,
            currentXp = xp,
            currentShards = shards,
            isUnlocked = isUnlocked
        )
    }

    /**
     * Retrieves all unified CardInstances in the catalog.
     * (Phase 6A Requirement #4)
     */
    fun getAllCardInstances(): List<CardInstance> {
        return CardCatalog.ALL_CARDS.map { def ->
            CardInstance(
                cardId = def.id,
                quantity = ownedCardCounts[def.id] ?: 0,
                level = cardLevels[def.id] ?: 1,
                shards = cardShards[def.id] ?: 0,
                isUnlocked = ownedCardIds.contains(def.id),
                definition = def
            )
        }
    }
}

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

    private var sharedPreferences: SharedPreferences? = null

    /**
     * Initializes persistence with Android context.
     * Survives app recreation, configuration changes, and navigation. (Requirement #12)
     */
    fun initPersistence(context: Context) {
        if (sharedPreferences != null) return
        sharedPreferences = context.applicationContext.getSharedPreferences("mythos_player_data", Context.MODE_PRIVATE)
        loadFromPersistence()
        checkDailyReset()
    }

    /**
     * Resets repository state and reconnects preferences for isolated testing.
     */
    fun resetForTesting(context: Context? = null) {
        _economyState.value = PlayerEconomyState()
        processedPurchaseIds.clear()
        if (context != null) {
            sharedPreferences = context.applicationContext.getSharedPreferences("mythos_player_data", Context.MODE_PRIVATE)
            loadFromPersistence()
        }
    }

    private fun loadFromPersistence() {
        val prefs = sharedPreferences ?: return
        if (!prefs.contains("saved_gold")) return

        try {
            val savedGold = prefs.getInt("saved_gold", 25_000)
            val savedGems = prefs.getInt("saved_gems", 500)
            val savedOwnedCards = prefs.getStringSet("saved_owned_cards", null)
            val savedCountsJson = prefs.getString("saved_card_counts", null)
            val savedLevelsJson = prefs.getString("saved_card_levels", null)
            val savedShardsJson = prefs.getString("saved_card_shards", null)
            val savedSelectedHero = prefs.getString("saved_selected_hero", null)
            val savedOwnedHeroes = prefs.getStringSet("saved_owned_heroes", null)
            val savedActiveDeckJson = prefs.getString("saved_active_deck", null)

            val savedPlayerLevel = prefs.getInt("saved_player_level", 1)
            val savedPlayerXp = prefs.getInt("saved_player_xp", 0)
            val savedTotalBattles = prefs.getInt("saved_total_battles", 0)
            val savedTotalVictories = prefs.getInt("saved_total_victories", 0)
            val savedTotalDefeats = prefs.getInt("saved_total_defeats", 0)
            val savedTotalCards = prefs.getInt("saved_total_cards_played", 0)
            val savedTotalDamageDealt = prefs.getLong("saved_total_damage_dealt", 0L)
            val savedTotalDamageTaken = prefs.getLong("saved_total_damage_taken", 0L)
            val savedTotalCampaignStages = prefs.getInt("saved_total_campaign_stages", 0)
            val savedClaimedLevelRewardsSet = prefs.getStringSet("saved_claimed_level_rewards", emptySet())
            val claimedLevelRewards = savedClaimedLevelRewardsSet?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()

            val playerProgress = PlayerProgress(
                playerLevel = savedPlayerLevel,
                playerXp = savedPlayerXp,
                totalBattles = savedTotalBattles,
                totalVictories = savedTotalVictories,
                totalDefeats = savedTotalDefeats,
                totalCardsPlayed = savedTotalCards,
                totalDamageDealt = savedTotalDamageDealt,
                totalDamageTaken = savedTotalDamageTaken,
                totalCampaignStagesCleared = savedTotalCampaignStages,
                claimedLevelRewards = claimedLevelRewards
            )

            val savedDailyQuestDate = prefs.getString("saved_daily_quest_date", "") ?: ""
            val savedQuestsJson = prefs.getString("saved_daily_quests", null)
            val dailyQuests = if (savedQuestsJson != null) {
                try {
                    val arr = org.json.JSONArray(savedQuestsJson)
                    val list = mutableListOf<DailyQuest>()
                    for (i in 0 until arr.length()) {
                        val qObj = arr.getJSONObject(i)
                        val qId = qObj.getString("quest_id")
                        val defaultQ = DailyQuestConfig.createDefaultQuests().find { it.questId == qId }
                        if (defaultQ != null) {
                            list.add(
                                defaultQ.copy(
                                    progress = qObj.optInt("progress", defaultQ.progress),
                                    isClaimed = qObj.optBoolean("is_claimed", defaultQ.isClaimed)
                                )
                            )
                        }
                    }
                    if (list.size == 5) list else DailyQuestConfig.createDefaultQuests()
                } catch (e: Exception) {
                    DailyQuestConfig.createDefaultQuests()
                }
            } else {
                DailyQuestConfig.createDefaultQuests()
            }

            val savedLoginRewardDay = prefs.getInt("saved_login_reward_day", 1)
            val savedLastLoginRewardDate = prefs.getString("saved_last_login_reward_date", null)

            _economyState.update { current ->
                val cardCounts = if (savedCountsJson != null) {
                    val map = current.ownedCardCounts.toMutableMap()
                    val json = JSONObject(savedCountsJson)
                    json.keys().forEach { key -> map[key] = json.getInt(key) }
                    map
                } else current.ownedCardCounts

                val cardLevels = if (savedLevelsJson != null) {
                    val map = current.cardLevels.toMutableMap()
                    val json = JSONObject(savedLevelsJson)
                    json.keys().forEach { key -> map[key] = json.getInt(key) }
                    map
                } else current.cardLevels

                val cardShards = if (savedShardsJson != null) {
                    val map = current.cardShards.toMutableMap()
                    val json = JSONObject(savedShardsJson)
                    json.keys().forEach { key -> map[key] = json.getInt(key) }
                    map
                } else current.cardShards

                val savedHighestStage = prefs.getInt("saved_highest_stage", current.highestUnlockedStage)
                val savedCompletedStages = prefs.getStringSet("saved_completed_stages", null)
                val savedStageStarsJson = prefs.getString("saved_stage_stars", null)
                val stageStars = if (savedStageStarsJson != null) {
                    val map = current.stageStars.toMutableMap()
                    val json = JSONObject(savedStageStarsJson)
                    json.keys().forEach { key -> map[key] = json.getInt(key) }
                    map
                } else current.stageStars

                val savedHeroLevelsJson = prefs.getString("saved_hero_levels", null)
                val heroProgression = if (savedHeroLevelsJson != null) {
                    val map = current.heroProgression.toMutableMap()
                    val json = JSONObject(savedHeroLevelsJson)
                    json.keys().forEach { key -> map[key] = json.getInt(key) }
                    map
                } else current.heroProgression

                val savedHeroShardsJson = prefs.getString("saved_hero_shards", null)
                val heroShards = if (savedHeroShardsJson != null) {
                    val map = current.heroShards.toMutableMap()
                    val json = JSONObject(savedHeroShardsJson)
                    json.keys().forEach { key -> map[key] = json.getInt(key) }
                    map
                } else current.heroShards

                val savedHeroXpJson = prefs.getString("saved_hero_xp", null)
                val heroXp = if (savedHeroXpJson != null) {
                    val map = current.heroXp.toMutableMap()
                    val json = JSONObject(savedHeroXpJson)
                    json.keys().forEach { key -> map[key] = json.getInt(key) }
                    map
                } else current.heroXp

                val restoredDeck = if (savedActiveDeckJson != null) {
                    try {
                        val deckObj = JSONObject(savedActiveDeckJson)
                        val deckId = deckObj.optString("deck_id", current.activeDeck.deckId)
                        val deckName = deckObj.optString("name", current.activeDeck.name)
                        val heroId = deckObj.optString("hero_id", current.activeDeck.heroId)
                        val updatedAt = deckObj.optLong("updated_at", current.activeDeck.updatedAt)
                        val cardIdsArr = deckObj.optJSONArray("card_ids")
                        val cardIds = mutableListOf<String>()
                        if (cardIdsArr != null) {
                            for (i in 0 until cardIdsArr.length()) {
                                cardIds.add(cardIdsArr.getString(i))
                            }
                        }
                        if (cardIds.size == ActiveDeck.REQUIRED_DECK_SIZE) {
                            ActiveDeck(
                                deckId = deckId,
                                name = deckName,
                                heroId = heroId,
                                cardIds = cardIds.toList(),
                                updatedAt = updatedAt
                            )
                        } else {
                            ActiveDeck.createDefaultOlympusDeck()
                        }
                    } catch (e: Exception) {
                        ActiveDeck.createDefaultOlympusDeck()
                    }
                } else {
                    current.activeDeck.createDefensiveCopy()
                }

                current.copy(
                    gold = savedGold,
                    mythGems = savedGems,
                    ownedCardIds = savedOwnedCards ?: current.ownedCardIds,
                    ownedCardCounts = cardCounts,
                    cardLevels = cardLevels,
                    cardShards = cardShards,
                    selectedHeroId = savedSelectedHero ?: current.selectedHeroId,
                    ownedHeroIds = savedOwnedHeroes ?: current.ownedHeroIds,
                    activeDeck = restoredDeck,
                    highestUnlockedStage = savedHighestStage,
                    completedStageIds = savedCompletedStages ?: current.completedStageIds,
                    stageStars = stageStars,
                    heroProgression = heroProgression,
                    heroShards = heroShards,
                    heroXp = heroXp,
                    playerProgress = playerProgress,
                    dailyQuests = dailyQuests,
                    dailyQuestDate = savedDailyQuestDate,
                    loginRewardDay = savedLoginRewardDay,
                    lastLoginRewardDate = savedLastLoginRewardDate
                )
            }
        } catch (e: Exception) {
            // Fallback gracefully to default state on any persistence parse failure
        }
    }

    private fun saveToPersistence() {
        val prefs = sharedPreferences ?: return
        val current = _economyState.value
        try {
            val countsJson = JSONObject()
            current.ownedCardCounts.forEach { (k, v) -> countsJson.put(k, v) }

            val levelsJson = JSONObject()
            current.cardLevels.forEach { (k, v) -> levelsJson.put(k, v) }

            val shardsJson = JSONObject()
            current.cardShards.forEach { (k, v) -> shardsJson.put(k, v) }

            val heroLevelsJson = JSONObject()
            current.heroProgression.forEach { (k, v) -> heroLevelsJson.put(k, v) }

            val heroShardsJson = JSONObject()
            current.heroShards.forEach { (k, v) -> heroShardsJson.put(k, v) }

            val heroXpJson = JSONObject()
            current.heroXp.forEach { (k, v) -> heroXpJson.put(k, v) }

            val activeDeck = current.activeDeck
            val deckJson = JSONObject().apply {
                put("deck_id", activeDeck.deckId)
                put("name", activeDeck.name)
                put("hero_id", activeDeck.heroId)
                put("updated_at", activeDeck.updatedAt)
                val cardIdsArr = org.json.JSONArray()
                activeDeck.cardIds.forEach { cardIdsArr.put(it) }
                put("card_ids", cardIdsArr)
            }

            val starsJson = JSONObject()
            current.stageStars.forEach { (k, v) -> starsJson.put(k, v) }

            val questsArray = org.json.JSONArray()
            current.dailyQuests.forEach { q ->
                val qObj = JSONObject().apply {
                    put("quest_id", q.questId)
                    put("progress", q.progress)
                    put("is_claimed", q.isClaimed)
                }
                questsArray.put(qObj)
            }

            val claimedRewardsSet = current.playerProgress.claimedLevelRewards.map { it.toString() }.toSet()

            prefs.edit()
                .putInt("saved_gold", current.gold)
                .putInt("saved_gems", current.mythGems)
                .putStringSet("saved_owned_cards", current.ownedCardIds)
                .putString("saved_card_counts", countsJson.toString())
                .putString("saved_card_levels", levelsJson.toString())
                .putString("saved_card_shards", shardsJson.toString())
                .putString("saved_selected_hero", current.selectedHeroId)
                .putStringSet("saved_owned_heroes", current.ownedHeroIds)
                .putString("saved_hero_levels", heroLevelsJson.toString())
                .putString("saved_hero_shards", heroShardsJson.toString())
                .putString("saved_hero_xp", heroXpJson.toString())
                .putString("saved_active_deck", deckJson.toString())
                .putInt("saved_highest_stage", current.highestUnlockedStage)
                .putStringSet("saved_completed_stages", current.completedStageIds)
                .putString("saved_stage_stars", starsJson.toString())
                .putInt("saved_player_level", current.playerProgress.playerLevel)
                .putInt("saved_player_xp", current.playerProgress.playerXp)
                .putInt("saved_total_battles", current.playerProgress.totalBattles)
                .putInt("saved_total_victories", current.playerProgress.totalVictories)
                .putInt("saved_total_defeats", current.playerProgress.totalDefeats)
                .putInt("saved_total_cards_played", current.playerProgress.totalCardsPlayed)
                .putLong("saved_total_damage_dealt", current.playerProgress.totalDamageDealt)
                .putLong("saved_total_damage_taken", current.playerProgress.totalDamageTaken)
                .putInt("saved_total_campaign_stages", current.playerProgress.totalCampaignStagesCleared)
                .putStringSet("saved_claimed_level_rewards", claimedRewardsSet)
                .putString("saved_daily_quests", questsArray.toString())
                .putString("saved_daily_quest_date", current.dailyQuestDate)
                .putInt("saved_login_reward_day", current.loginRewardDay)
                .putString("saved_last_login_reward_date", current.lastLoginRewardDate)
                .commit()
        } catch (e: Exception) {
            // Graceful error handling
        }
    }

    fun getCardInstance(cardId: String): CardInstance? = _economyState.value.getCardInstance(cardId)
    fun getAllCardInstances(): List<CardInstance> = _economyState.value.getAllCardInstances()

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
        saveToPersistence()
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
            // Already owned -> increment owned count AND convert duplicate to shards
            ownedCardCounts[cardId] = currentCount + 1
            val shardsToAdd = ShardConversion.getShardsForDuplicate(rarity)
            val curShards = shardsMap[cardId] ?: 0
            shardsMap[cardId] = curShards + shardsToAdd
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
        saveToPersistence()
        updateQuestProgress(QuestType.UPGRADE_CARD, 1)

        return Result.success(nextStep)
    }

    /**
     * Atomic Hero Level Upgrade (Phase 7C Sections 4, 5, 10).
     * Validates Gold and Hero Shards, deducts resources, increments level,
     * recalculates progression, persists state, and preserves ActiveDeck intact.
     */
    @Synchronized
    fun upgradeHero(heroId: String): Result<HeroProgress> {
        val state = _economyState.value
        if (!state.ownedHeroIds.contains(heroId)) {
            return Result.failure(IllegalStateException("Hero is locked and cannot be upgraded."))
        }
        val currentLevel = state.heroProgression[heroId] ?: 1
        if (currentLevel >= HeroProgressionConfig.MAX_HERO_LEVEL) {
            return Result.failure(IllegalStateException("Hero is already at maximum level ($currentLevel)."))
        }
        val cost = HeroProgressionConfig.getUpgradeCost(currentLevel)
            ?: return Result.failure(IllegalStateException("No upgrade configuration available for level $currentLevel."))

        val currentShards = state.heroShards[heroId] ?: 0
        if (state.gold < cost.goldCost) {
            val missing = cost.goldCost - state.gold
            return Result.failure(IllegalStateException("Insufficient Gold. Need $missing more Gold."))
        }
        if (currentShards < cost.shardCost) {
            val missing = cost.shardCost - currentShards
            return Result.failure(IllegalStateException("Insufficient Hero Shards. Need $missing more Shards."))
        }

        val nextLevel = currentLevel + 1
        val reqXp = HeroProgressionConfig.getXpRequiredForNextLevel(currentLevel)
        val curXp = state.heroXp[heroId] ?: 0
        val newXp = maxOf(0, curXp - reqXp)

        // Strict defensive copy of activeDeck ensures cardIds are NEVER mutated
        val preservedDeck = state.activeDeck.createDefensiveCopy()

        _economyState.update { current ->
            val updatedLevels = current.heroProgression.toMutableMap()
            updatedLevels[heroId] = nextLevel

            val updatedShards = current.heroShards.toMutableMap()
            updatedShards[heroId] = currentShards - cost.shardCost

            val updatedXp = current.heroXp.toMutableMap()
            updatedXp[heroId] = newXp

            current.copy(
                gold = current.gold - cost.goldCost,
                heroProgression = updatedLevels,
                heroShards = updatedShards,
                heroXp = updatedXp,
                activeDeck = preservedDeck
            )
        }
        saveToPersistence()
        updateQuestProgress(QuestType.UPGRADE_HERO, 1)
        return Result.success(_economyState.value.getHeroProgress(heroId))
    }

    @Synchronized
    fun addHeroXp(heroId: String, xp: Int) {
        if (xp <= 0) return
        _economyState.update { current ->
            val updatedXp = current.heroXp.toMutableMap()
            val curXp = updatedXp[heroId] ?: 0
            updatedXp[heroId] = curXp + xp
            current.copy(heroXp = updatedXp)
        }
        saveToPersistence()
    }

    @Synchronized
    fun addHeroShards(heroId: String, shards: Int) {
        if (shards <= 0) return
        _economyState.update { current ->
            val updatedShards = current.heroShards.toMutableMap()
            val curShards = updatedShards[heroId] ?: 0
            updatedShards[heroId] = curShards + shards
            current.copy(heroShards = updatedShards)
        }
        saveToPersistence()
    }

    fun setHeroLevelForTesting(heroId: String, level: Int) {
        _economyState.update { current ->
            val updated = current.heroProgression.toMutableMap()
            updated[heroId] = level
            current.copy(heroProgression = updated)
        }
    }

    fun setHeroUnlockForTesting(heroId: String, unlocked: Boolean) {
        _economyState.update { current ->
            val updated = current.ownedHeroIds.toMutableSet()
            if (unlocked) updated.add(heroId) else updated.remove(heroId)
            current.copy(ownedHeroIds = updated)
        }
    }

    /**
     * Adds Player XP, levels up the player if threshold is reached, and awards level-up rewards (Phase 7D Sections 4, 5, 6).
     * Never awards level-up rewards twice for the same level.
     */
    @Synchronized
    fun addPlayerXp(xp: Int): List<PlayerLevelReward> {
        if (xp <= 0) return emptyList()
        val awardedRewards = mutableListOf<PlayerLevelReward>()

        _economyState.update { current ->
            var curLevel = current.playerProgress.playerLevel
            var curXp = current.playerProgress.playerXp + xp
            val claimedRewards = current.playerProgress.claimedLevelRewards.toMutableSet()
            var currentGold = current.gold
            var currentGems = current.mythGems
            val currentCardShards = current.cardShards.toMutableMap()
            val currentHeroShards = current.heroShards.toMutableMap()

            while (curLevel < PlayerProgressionConfig.MAX_PLAYER_LEVEL) {
                val reqXp = PlayerProgressionConfig.getXpRequiredForNextLevel(curLevel)
                if (curXp >= reqXp && reqXp > 0) {
                    curXp -= reqXp
                    curLevel += 1

                    // Check level-up reward
                    val reward = PlayerProgressionConfig.getLevelReward(curLevel)
                    if (reward != null && !claimedRewards.contains(curLevel)) {
                        claimedRewards.add(curLevel)
                        awardedRewards.add(reward)
                        currentGold += reward.gold
                        currentGems += reward.mythGems
                        if (reward.cardShards > 0) {
                            val targetCard = "c_heroic_rage"
                            currentCardShards[targetCard] = (currentCardShards[targetCard] ?: 0) + reward.cardShards
                        }
                        if (reward.heroShards > 0) {
                            val targetHero = current.selectedHeroId
                            currentHeroShards[targetHero] = (currentHeroShards[targetHero] ?: 0) + reward.heroShards
                        }
                    }
                } else {
                    break
                }
            }

            val updatedProgress = current.playerProgress.copy(
                playerLevel = curLevel,
                playerXp = curXp,
                claimedLevelRewards = claimedRewards
            )

            current.copy(
                gold = currentGold,
                mythGems = currentGems,
                cardShards = currentCardShards,
                heroShards = currentHeroShards,
                playerProgress = updatedProgress,
                activeDeck = current.activeDeck.createDefensiveCopy()
            )
        }
        saveToPersistence()
        return awardedRewards
    }

    /**
     * Updates quest progress automatically for all quests matching the specified QuestType (Phase 7D Section 11).
     */
    @Synchronized
    fun updateQuestProgress(type: QuestType, amount: Int) {
        if (amount <= 0) return
        _economyState.update { current ->
            val updatedQuests = current.dailyQuests.map { quest ->
                if (quest.type == type && !quest.isClaimed) {
                    val newProgress = minOf(quest.target, quest.progress + amount)
                    quest.copy(progress = newProgress)
                } else {
                    quest
                }
            }
            current.copy(
                dailyQuests = updatedQuests,
                activeDeck = current.activeDeck.createDefensiveCopy()
            )
        }
        saveToPersistence()
    }

    /**
     * Claims a completed Daily Quest reward atomically (Phase 7D Section 12).
     */
    @Synchronized
    fun claimQuestReward(questId: String): Result<QuestReward> {
        val current = _economyState.value
        val quest = current.dailyQuests.find { it.questId == questId }
            ?: return Result.failure(IllegalArgumentException("Daily quest '$questId' not found."))

        if (!quest.isCompleted) {
            return Result.failure(IllegalStateException("Quest is not completed yet (${quest.progress}/${quest.target})."))
        }
        if (quest.isClaimed) {
            return Result.failure(IllegalStateException("Quest reward has already been claimed."))
        }

        val reward = quest.reward
        val preservedDeck = current.activeDeck.createDefensiveCopy()

        _economyState.update { state ->
            val updatedQuests = state.dailyQuests.map {
                if (it.questId == questId) it.copy(isClaimed = true) else it
            }
            val newGold = state.gold + reward.gold
            val newGems = state.mythGems + reward.mythGems
            val newCardShards = state.cardShards.toMutableMap()
            if (reward.cardShards > 0) {
                val cardId = reward.cardShardCardId ?: "c_power_strike"
                newCardShards[cardId] = (newCardShards[cardId] ?: 0) + reward.cardShards
            }
            val newHeroShards = state.heroShards.toMutableMap()
            if (reward.heroShards > 0) {
                val heroId = reward.heroShardHeroId ?: state.selectedHeroId
                newHeroShards[heroId] = (newHeroShards[heroId] ?: 0) + reward.heroShards
            }

            state.copy(
                gold = newGold,
                mythGems = newGems,
                cardShards = newCardShards,
                heroShards = newHeroShards,
                dailyQuests = updatedQuests,
                activeDeck = preservedDeck
            )
        }

        // Award Player XP if specified in quest reward
        if (reward.playerXp > 0) {
            addPlayerXp(reward.playerXp)
        } else {
            saveToPersistence()
        }

        return Result.success(reward)
    }

    /**
     * Checks calendar day change and resets daily quests once per day (Phase 7D Section 13).
     */
    @Synchronized
    fun checkDailyReset(forceDate: String? = null) {
        val currentDate = forceDate ?: MythosDateUtil.getCurrentLocalDate()
        val current = _economyState.value
        if (current.dailyQuestDate != currentDate) {
            _economyState.update {
                it.copy(
                    dailyQuests = DailyQuestConfig.createDefaultQuests(),
                    dailyQuestDate = currentDate,
                    activeDeck = it.activeDeck.createDefensiveCopy()
                )
            }
            saveToPersistence()
        }
    }

    /**
     * Checks if daily login reward is claimable for today (Phase 7D Section 16, 18).
     */
    fun canClaimDailyLoginReward(currentDateOverride: String? = null): Boolean {
        val today = currentDateOverride ?: MythosDateUtil.getCurrentLocalDate()
        return _economyState.value.lastLoginRewardDate != today
    }

    /**
     * Claims the daily login reward atomically (Phase 7D Section 16, 17).
     */
    @Synchronized
    fun claimDailyLoginReward(currentDateOverride: String? = null): Result<DailyLoginReward> {
        val today = currentDateOverride ?: MythosDateUtil.getCurrentLocalDate()
        if (!canClaimDailyLoginReward(today)) {
            return Result.failure(IllegalStateException("Daily login reward already claimed for today."))
        }

        val state = _economyState.value
        val day = state.loginRewardDay
        val reward = DailyLoginRewardConfig.getReward(day)

        val preservedDeck = state.activeDeck.createDefensiveCopy()
        val nextDay = if (day >= 7) 1 else day + 1

        _economyState.update { current ->
            val newGold = current.gold + reward.gold
            val newGems = current.mythGems + reward.mythGems
            val newCardShards = current.cardShards.toMutableMap()
            if (reward.cardShards > 0) {
                val cardId = reward.cardRewardId ?: "c_power_strike"
                newCardShards[cardId] = (newCardShards[cardId] ?: 0) + reward.cardShards
            }
            val newHeroShards = current.heroShards.toMutableMap()
            if (reward.heroShards > 0) {
                val heroId = current.selectedHeroId
                newHeroShards[heroId] = (newHeroShards[heroId] ?: 0) + reward.heroShards
            }

            val newOwnedCards = current.ownedCardIds.toMutableSet()
            val newCardCounts = current.ownedCardCounts.toMutableMap()
            if (!reward.cardRewardId.isNullOrBlank()) {
                addOrDuplicateCardInternal(
                    reward.cardRewardId,
                    CardRarity.RARE,
                    newOwnedCards,
                    newCardCounts,
                    newCardShards
                )
            }

            current.copy(
                gold = newGold,
                mythGems = newGems,
                cardShards = newCardShards,
                heroShards = newHeroShards,
                ownedCardIds = newOwnedCards,
                ownedCardCounts = newCardCounts,
                loginRewardDay = nextDay,
                lastLoginRewardDate = today,
                activeDeck = preservedDeck
            )
        }
        saveToPersistence()
        return Result.success(reward)
    }

    /**
     * Records card play event for Daily Quests and statistics.
     */
    @Synchronized
    fun onCardPlayed() {
        _economyState.update { current ->
            val p = current.playerProgress
            current.copy(
                playerProgress = p.copy(totalCardsPlayed = p.totalCardsPlayed + 1),
                activeDeck = current.activeDeck.createDefensiveCopy()
            )
        }
        updateQuestProgress(QuestType.PLAY_CARDS, 1)
        saveToPersistence()
    }

    /**
     * Records damage dealt for Daily Quests and statistics.
     */
    @Synchronized
    fun onDamageDealt(damage: Int) {
        if (damage <= 0) return
        _economyState.update { current ->
            val p = current.playerProgress
            current.copy(
                playerProgress = p.copy(totalDamageDealt = p.totalDamageDealt + damage),
                activeDeck = current.activeDeck.createDefensiveCopy()
            )
        }
        updateQuestProgress(QuestType.DEAL_DAMAGE, damage)
        saveToPersistence()
    }

    /**
     * Records battle completion statistics and triggers quests.
     */
    @Synchronized
    fun recordBattleFinished(isVictory: Boolean, stats: BattleStats, isCampaign: Boolean = false) {
        _economyState.update { current ->
            val p = current.playerProgress
            val newBattles = p.totalBattles + 1
            val newVictories = if (isVictory) p.totalVictories + 1 else p.totalVictories
            val newDefeats = if (!isVictory) p.totalDefeats + 1 else p.totalDefeats
            val newCardsPlayed = p.totalCardsPlayed + stats.cardsPlayedCount
            val newDamageDealt = p.totalDamageDealt + stats.totalDamageDealt
            val newDamageTaken = p.totalDamageTaken + stats.totalDamageTaken

            current.copy(
                playerProgress = p.copy(
                    totalBattles = newBattles,
                    totalVictories = newVictories,
                    totalDefeats = newDefeats,
                    totalCardsPlayed = newCardsPlayed,
                    totalDamageDealt = newDamageDealt,
                    totalDamageTaken = newDamageTaken
                ),
                activeDeck = current.activeDeck.createDefensiveCopy()
            )
        }
        if (stats.cardsPlayedCount > 0) {
            updateQuestProgress(QuestType.PLAY_CARDS, stats.cardsPlayedCount)
        }
        if (stats.totalDamageDealt > 0) {
            updateQuestProgress(QuestType.DEAL_DAMAGE, stats.totalDamageDealt)
        }
        if (isVictory) {
            updateQuestProgress(QuestType.WIN_BATTLE, 1)
            if (isCampaign) {
                updateQuestProgress(QuestType.WIN_CAMPAIGN_BATTLE, 1)
            }
        }
        saveToPersistence()
    }

    fun setPlayerLevelForTesting(level: Int, xp: Int = 0) {
        _economyState.update { current ->
            val updated = current.playerProgress.copy(playerLevel = level, playerXp = xp)
            current.copy(playerProgress = updated)
        }
    }

    fun setDailyQuestProgressForTesting(questId: String, progress: Int) {
        _economyState.update { current ->
            val updated = current.dailyQuests.map {
                if (it.questId == questId) it.copy(progress = progress) else it
            }
            current.copy(dailyQuests = updated)
        }
    }

    fun setLoginRewardDayForTesting(day: Int, lastDate: String?) {
        _economyState.update { current ->
            current.copy(loginRewardDay = day, lastLoginRewardDate = lastDate)
        }
    }

    /**
     * Saves Active Deck with validation (Requirements #10, #11).
     * Strictly creates an isolated defensive copy so persistent ActiveDeck cannot be mutated.
     */
     @Synchronized
     fun saveActiveDeck(deck: ActiveDeck): DeckValidationResult {
         val safeDeck = deck.copy(cardIds = deck.cardIds.toList())
         val validation = DeckValidator.validate(safeDeck, _economyState.value.ownedCardCounts)
         if (validation.isValid) {
             _economyState.update { it.copy(activeDeck = safeDeck) }
             saveToPersistence()
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
                activeDeck = current.activeDeck.copy(heroId = heroId, cardIds = current.activeDeck.cardIds.toList())
            )
        }
        saveToPersistence()
        return true
    }

    /**
     * Records a Campaign Stage Victory atomically (Phase 7B Requirements #1, #2, #3, #9, #10, #12).
     *
     * Features:
     * - Deterministic star conditions (Condition A: HP > 50%, Condition B: Turns <= limit).
     * - Stars can never decrease; best star rating is preserved and persisted.
     * - Strictly prevents duplicate first-clear rewards (Gold, XP, Card).
     * - Replays grant configured replayGold and replayXp only.
     * - First-clear card reward enters collection or converts duplicates to shards via ShardConversion.
     * - ActiveDeck remains 100% isolated and preserved (exactly 20/20 cards).
     */
    @Synchronized
    fun recordCampaignVictory(
        stage: CampaignStage,
        playerRemainingHp: Int,
        playerMaxHp: Int,
        turnsCount: Int
    ): StageVictoryResult {
        val stageId = stage.stageId
        val current = _economyState.value
        val isFirstClear = !current.completedStageIds.contains(stageId)
        val previousStars = current.stageStars[stageId] ?: 0

        val hpPercentage = if (playerMaxHp > 0) ((playerRemainingHp.toFloat() / playerMaxHp.toFloat()) * 100).toInt() else 0
        val hpConditionMet = playerRemainingHp > (playerMaxHp / 2)
        val turnsConditionMet = turnsCount <= stage.maxTurnsForStarCondition
        val starsEarned = 1 + (if (hpConditionMet) 1 else 0) + (if (turnsConditionMet) 1 else 0)
        val bestStars = maxOf(previousStars, starsEarned)

        val goldAwarded = if (isFirstClear) stage.firstClearGold else stage.replayGold
        val xpAwarded = if (isFirstClear) stage.firstClearXp else stage.replayXp

        val newOwnedCards = current.ownedCardIds.toMutableSet()
        val newCardCounts = current.ownedCardCounts.toMutableMap()
        val newShards = current.cardShards.toMutableMap()

        var cardIdAwarded: String? = null
        var cardNameAwarded: String? = null
        var cardRarityAwarded: CardRarity? = null
        var wasDuplicate = false
        var shardsForDuplicate = 0

        // Only grant card reward on FIRST CLEAR
        if (isFirstClear && stage.firstClearCardId.isNotBlank()) {
            cardIdAwarded = stage.firstClearCardId
            cardNameAwarded = stage.firstClearCardName
            cardRarityAwarded = stage.firstClearCardRarity

            val curCount = newCardCounts[cardIdAwarded] ?: 0
            if (newOwnedCards.contains(cardIdAwarded)) {
                // Card is already owned -> convert duplicate to shards
                wasDuplicate = true
                newCardCounts[cardIdAwarded] = curCount + 1
                shardsForDuplicate = ShardConversion.getShardsForDuplicate(stage.firstClearCardRarity)
                newShards[cardIdAwarded] = (newShards[cardIdAwarded] ?: 0) + shardsForDuplicate
            } else {
                wasDuplicate = false
                newOwnedCards.add(cardIdAwarded)
                newCardCounts[cardIdAwarded] = 1
            }
        }

        val newCompleted = current.completedStageIds + stageId
        val newStars = current.stageStars + (stageId to bestStars)
        val nextStageNum = stage.stageNumber + 1
        val newHighest = maxOf(current.highestUnlockedStage, nextStageNum)

        // Hero XP reward to currently selected hero (Phase 7C Section 11)
        val heroXpAwarded = if (isFirstClear) stage.firstClearHeroXp else stage.replayHeroXp
        val newHeroXp = current.heroXp.toMutableMap()
        val curHeroXp = newHeroXp[current.selectedHeroId] ?: 0
        newHeroXp[current.selectedHeroId] = curHeroXp + heroXpAwarded

        // Hero Shards reward protected on first clear (Phase 7C Section 12)
        val heroShardsAwarded = if (isFirstClear) stage.firstClearHeroShards else stage.replayHeroShards
        val newHeroShards = current.heroShards.toMutableMap()
        var heroShardsHeroId: String? = null
        if (heroShardsAwarded > 0) {
            heroShardsHeroId = stage.firstClearHeroShardHeroId
            val curShards = newHeroShards[heroShardsHeroId] ?: 0
            newHeroShards[heroShardsHeroId] = curShards + heroShardsAwarded
        }

        // Hero Unlock triggers (Phase 7C Sections 7, 8, 16):
        // Stage 2 (Wrath of the Arena) -> Unlocks Achilles permanently
        // Stage 5 (Wrath of Olympus) -> Unlocks Merlin permanently
        val newOwnedHeroes = current.ownedHeroIds.toMutableSet()
        var heroUnlocked: String? = null
        if (stage.stageId == "stage_1_2" && !current.completedStageIds.contains("stage_1_2")) {
            newOwnedHeroes.add("hero_achilles")
            heroUnlocked = "Achilles"
        } else if (stage.stageId == "stage_1_5" && !current.completedStageIds.contains("stage_1_5")) {
            newOwnedHeroes.add("hero_merlin")
            heroUnlocked = "Merlin"
        }

        // Strict defensive copy of activeDeck ensures deck is never mutated
        val preservedDeck = current.activeDeck.createDefensiveCopy()

        val p = current.playerProgress
        val updatedProgress = p.copy(
            totalCampaignStagesCleared = p.totalCampaignStagesCleared + (if (isFirstClear) 1 else 0),
            totalBattles = p.totalBattles + 1,
            totalVictories = p.totalVictories + 1
        )

        _economyState.update {
            it.copy(
                gold = it.gold + goldAwarded,
                ownedCardIds = newOwnedCards,
                ownedCardCounts = newCardCounts,
                cardShards = newShards,
                highestUnlockedStage = newHighest,
                completedStageIds = newCompleted,
                stageStars = newStars,
                activeDeck = preservedDeck,
                heroXp = newHeroXp,
                heroShards = newHeroShards,
                ownedHeroIds = newOwnedHeroes,
                playerProgress = updatedProgress
            )
        }
        saveToPersistence()

        // Trigger quests and award Player XP (Phase 7D Sections 5, 11)
        updateQuestProgress(QuestType.COMPLETE_CAMPAIGN_STAGE, 1)
        updateQuestProgress(QuestType.WIN_CAMPAIGN_BATTLE, 1)
        updateQuestProgress(QuestType.WIN_BATTLE, 1)
        addPlayerXp(xpAwarded)

        return StageVictoryResult(
            stageId = stageId,
            stageName = stage.name,
            stageNumber = stage.stageNumber,
            isBoss = stage.isBoss,
            isFirstClear = isFirstClear,
            starsEarned = starsEarned,
            previousStars = previousStars,
            bestStars = bestStars,
            hpConditionMet = hpConditionMet,
            turnsConditionMet = turnsConditionMet,
            playerRemainingHp = playerRemainingHp,
            playerMaxHp = playerMaxHp,
            hpPercentage = hpPercentage,
            turnsCount = turnsCount,
            maxTurnsAllowed = stage.maxTurnsForStarCondition,
            goldAwarded = goldAwarded,
            xpAwarded = xpAwarded,
            cardIdAwarded = cardIdAwarded,
            cardNameAwarded = cardNameAwarded,
            cardRarityAwarded = cardRarityAwarded,
            wasDuplicateCard = wasDuplicate,
            shardsAwardedForDuplicate = shardsForDuplicate,
            heroXpAwarded = heroXpAwarded,
            heroShardsAwarded = heroShardsAwarded,
            heroShardsHeroId = heroShardsHeroId,
            heroUnlocked = heroUnlocked
        )
    }

    /**
     * Completes a campaign stage, updates highest unlocked stage, and persists progress.
     */
    @Synchronized
    fun completeCampaignStage(stageId: String, stars: Int = 3) {
        val stage = CampaignCatalog.findStage(stageId)
        val stageNumber = stage?.stageNumber ?: 1
        _economyState.update { current ->
            val newCompleted = current.completedStageIds + stageId
            val existingStars = current.stageStars[stageId] ?: 0
            val newStars = current.stageStars + (stageId to maxOf(existingStars, stars))
            val nextStageNum = stageNumber + 1
            val newHighest = maxOf(current.highestUnlockedStage, nextStageNum)
            current.copy(
                completedStageIds = newCompleted,
                stageStars = newStars,
                highestUnlockedStage = newHighest
            )
        }
        saveToPersistence()
    }

    /**
     * Awards Battle Victory Rewards to inventory and collection (Requirement #16).
     * CRITICAL: Guaranteed never to mutate, alter, or remove cards from ActiveDeck.
     */
    @Synchronized
    fun claimBattleVictoryRewards(rewards: BattleRewards, customRewardCardId: String? = null) {
        _economyState.update { current ->
            val newGold = current.gold + rewards.gold
            val newOwnedCards = current.ownedCardIds.toMutableSet()
            val newCardCounts = current.ownedCardCounts.toMutableMap()
            val newShards = current.cardShards.toMutableMap()

            // Resolve reward card (e.g. stage specific reward or default Divine Aegis)
            val effectiveRewardCardId = if (!customRewardCardId.isNullOrBlank()) customRewardCardId else "c_divine_aegis"
            addOrDuplicateCardInternal(effectiveRewardCardId, rewards.cardRewardRarity, newOwnedCards, newCardCounts, newShards)

            // Strictly preserve the active deck as an immutable copy
            val preservedDeck = current.activeDeck.createDefensiveCopy()

            current.copy(
                gold = newGold,
                ownedCardIds = newOwnedCards,
                ownedCardCounts = newCardCounts,
                cardShards = newShards,
                activeDeck = preservedDeck
            )
        }
        saveToPersistence()
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
        saveToPersistence()

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
