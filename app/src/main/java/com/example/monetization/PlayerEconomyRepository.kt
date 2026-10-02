package com.example.monetization

import android.app.Activity
import androidx.activity.ComponentActivity
import android.content.Context
import android.content.SharedPreferences
import com.example.MythosConfig
import com.example.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.lang.ref.WeakReference

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
    val purchaseHistory: List<PurchaseRecord> = emptyList(),
    val alliances: List<Alliance> = AllianceCatalog.createDefaultAlliances(),
    val playerAllianceId: String? = null,
    val activeEvents: List<MythosEvent> = EventCatalog.getDefaultEvents(),
    val notifications: List<MythosNotification> = emptyList(),
    // Phase 10 Endgame PvE Systems
    val eventTokens: Int = 100,
    val worldBossCurrentHp: Long = EndgameCatalog.KRONOS.maxHp,
    val worldBossContributions: Map<String, WorldBossContribution> = emptyMap(),
    val raidDailyAttempts: Int = 3,
    val lastRaidAttemptDate: String = "",
    val raidStageProgress: Map<String, Int> = emptyMap(),
    val trialStars: Map<String, Int> = emptyMap(),
    val trialClearedDifficulties: Map<String, Set<String>> = emptyMap(),
    val weeklyObjectives: List<WeeklyObjective> = EndgameCatalog.createDefaultWeeklyObjectives(),
    val weeklyObjectiveWeek: String = "",
    val eventShopPurchases: Map<String, Int> = emptyMap(),
    // Phase 11 PvP Arena & Competitive Systems
    val arenaRating: Int = ArenaRatingCalculator.STARTING_RATING,
    val arenaPeakRating: Int = ArenaRatingCalculator.STARTING_RATING,
    val arenaWins: Int = 0,
    val arenaLosses: Int = 0,
    val arenaCurrentStreak: Int = 0,
    val arenaHighestStreak: Int = 0,
    val arenaDailyAttempts: Int = ArenaCatalog.MAX_DAILY_ATTEMPTS,
    val lastArenaAttemptDate: String = "",
    val arenaPoints: Int = 0,
    val arenaFirstWinClaimedDate: String = "",
    val arenaCurrentSeason: ArenaSeason = ArenaSeason.createDefaultSeason(),
    val arenaSeasonRewardsClaimed: Set<String> = emptySet(),
    val arenaMatchHistory: List<ArenaMatchRecord> = emptyList()
) {
    val arenaTier: ArenaRankTier get() = ArenaRankTier.fromRating(arenaRating)
    val arenaTotalMatches: Int get() = arenaWins + arenaLosses
    val arenaWinRate: Float get() = if (arenaTotalMatches > 0) (arenaWins.toFloat() / arenaTotalMatches) * 100f else 0f

    val playerAlliance: Alliance?
        get() = alliances.find { it.allianceId == playerAllianceId }

    val playerMemberRecord: AllianceMember?
        get() = playerAlliance?.members?.find { it.playerId == "player_local" }

    val isLeaderOfAlliance: Boolean
        get() = playerMemberRecord?.role == AllianceRole.LEADER

    val isOfficerOfAlliance: Boolean
        get() = playerMemberRecord?.role == AllianceRole.OFFICER || playerMemberRecord?.role == AllianceRole.LEADER
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
    initialBillingProvider: BillingProvider = MockBillingProvider()
) {
    companion object {
        val instance by lazy { PlayerEconomyRepository() }
    }
    private val _economyState = MutableStateFlow(PlayerEconomyState())
    val economyState: StateFlow<PlayerEconomyState> = _economyState.asStateFlow()

    // productId -> Play's own formattedPrice, e.g. "Rp9.900". Empty until GooglePlayBillingProvider
    // finishes its first refreshLivePrices() call; the shop UI falls back to MonetizationCatalog's
    // hardcoded priceDisplay for any productId missing from this map (dev/offline/debug builds
    // never populate it at all, since MockBillingProvider never calls onLivePricesUpdated).
    private val _livePrices = MutableStateFlow<Map<String, String>>(emptyMap())
    val livePrices: StateFlow<Map<String, String>> = _livePrices.asStateFlow()

    // Idempotency tracking
    private val processedPurchaseIds = mutableSetOf<String>()

    private var sharedPreferences: SharedPreferences? = null

    private var billingProvider: BillingProvider = initialBillingProvider
    private var currentActivityRef: WeakReference<Activity>? = null
    private var billingConfigured = false
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Wires up real Google Play Billing for release builds (debug builds keep using
     * [MockBillingProvider] so the shop can be tested without spending real money - see
     * MythosConfig.DEBUG_BUILD). Safe to call every onCreate: the Activity reference is
     * refreshed each time (needed after rotation/recreation), but the BillingClient
     * connection itself is only opened once.
     */
    fun configureBilling(activity: ComponentActivity) {
        currentActivityRef = WeakReference(activity)
        if (billingConfigured) return
        billingConfigured = true
        if (MythosConfig.DEBUG_BUILD) return

        if (MythosConfig.SERVER_VERIFICATION_ENABLED) {
            repoScope.launch {
                try {
                    PlayerIdentity.ensureSignedIn()
                } catch (e: Exception) {
                    // Non-fatal here: a purchase attempt will retry sign-in
                }
            }
        }

        val provider = GooglePlayBillingProvider(
            appContext = activity.applicationContext,
            activityProvider = { currentActivityRef?.get() },
            onExternalGrant = { record, items -> grantEntitlements(record, items) },
            onLivePricesUpdated = { prices -> _livePrices.value = prices }
        )
        billingProvider = provider
        provider.connect()
    }

    fun endBillingConnection() {
        (billingProvider as? GooglePlayBillingProvider)?.disconnect()
    }

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
            // Idempotency ledger for grantEntitlements()
            processedPurchaseIds.addAll(prefs.getStringSet("saved_processed_purchase_ids", emptySet()) ?: emptySet())

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
            val savedLoginStreak = prefs.getInt("saved_login_streak", 1)
            val savedHighestLoginStreak = prefs.getInt("saved_highest_login_streak", 1)
            val savedPlayerAllianceId = prefs.getString("saved_player_alliance_id", null)
            val savedAllianceName = prefs.getString("saved_player_alliance_name", null)
            val savedAllianceRole = prefs.getString("saved_player_alliance_role", null)
            val savedAllianceContribution = prefs.getLong("saved_alliance_contribution", 0L)

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
                claimedLevelRewards = claimedLevelRewards,
                loginStreak = savedLoginStreak,
                highestLoginStreak = savedHighestLoginStreak,
                allianceId = savedPlayerAllianceId,
                allianceName = savedAllianceName,
                allianceRole = savedAllianceRole,
                allianceContribution = savedAllianceContribution
            )

            val savedAlliancesJson = prefs.getString("saved_alliances_json", null)
            val restoredAlliances = if (savedAlliancesJson != null) {
                try {
                    val arr = org.json.JSONArray(savedAlliancesJson)
                    val list = mutableListOf<Alliance>()
                    for (i in 0 until arr.length()) {
                        val aObj = arr.getJSONObject(i)
                        val mArr = aObj.optJSONArray("members")
                        val members = mutableListOf<AllianceMember>()
                        if (mArr != null) {
                            for (j in 0 until mArr.length()) {
                                val mObj = mArr.getJSONObject(j)
                                members.add(
                                    AllianceMember(
                                        playerId = mObj.getString("player_id"),
                                        name = mObj.getString("name"),
                                        playerLevel = mObj.optInt("player_level", 1),
                                        heroId = mObj.optString("hero_id", "hero_hercules"),
                                        combatPower = mObj.optInt("combat_power", 1200),
                                        role = try { AllianceRole.valueOf(mObj.getString("role")) } catch (e: Exception) { AllianceRole.MEMBER },
                                        joinedDate = mObj.optString("joined_date", "2026-09-20"),
                                        battlesWon = mObj.optInt("battles_won", 0),
                                        damageDealt = mObj.optLong("damage_dealt", 0L),
                                        campaignClears = mObj.optInt("campaign_clears", 0),
                                        questsCompleted = mObj.optInt("quests_completed", 0),
                                        contributionScore = mObj.optLong("contribution_score", 0L)
                                    )
                                )
                            }
                        }
                        list.add(
                            Alliance(
                                allianceId = aObj.getString("alliance_id"),
                                name = aObj.getString("name"),
                                emblem = aObj.optString("emblem", "emblem_gold_eagle"),
                                description = aObj.optString("description", "Warriors of Mount Olympus united in divine glory."),
                                level = aObj.optInt("level", 1),
                                xp = aObj.optInt("xp", 0),
                                createdDate = aObj.optString("created_date", "2026-09-01"),
                                members = members
                            )
                        )
                    }
                    if (list.isNotEmpty()) list else AllianceCatalog.createDefaultAlliances()
                } catch (e: Exception) {
                    AllianceCatalog.createDefaultAlliances()
                }
            } else {
                AllianceCatalog.createDefaultAlliances()
            }

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

                val savedEventTokens = prefs.getInt("saved_event_tokens", 100)
                val savedWbHp = prefs.getLong("saved_wb_hp", EndgameCatalog.KRONOS.maxHp)
                val savedRaidAttempts = prefs.getInt("saved_raid_attempts", 3)
                val savedRaidDate = prefs.getString("saved_raid_date", "") ?: ""
                val savedWeeklyWeek = prefs.getString("saved_weekly_week", "") ?: ""

                val savedRaidProgressJson = prefs.getString("saved_raid_progress", null)
                val raidProgressMap = mutableMapOf<String, Int>()
                if (savedRaidProgressJson != null) {
                    try {
                        val json = JSONObject(savedRaidProgressJson)
                        json.keys().forEach { raidProgressMap[it] = json.getInt(it) }
                    } catch (e: Exception) {}
                }

                val savedTrialStarsJson = prefs.getString("saved_trial_stars", null)
                val trialStarsMap = mutableMapOf<String, Int>()
                if (savedTrialStarsJson != null) {
                    try {
                        val json = JSONObject(savedTrialStarsJson)
                        json.keys().forEach { trialStarsMap[it] = json.getInt(it) }
                    } catch (e: Exception) {}
                }

                val savedShopPurchasesJson = prefs.getString("saved_shop_purchases", null)
                val shopPurchasesMap = mutableMapOf<String, Int>()
                if (savedShopPurchasesJson != null) {
                    try {
                        val json = JSONObject(savedShopPurchasesJson)
                        json.keys().forEach { shopPurchasesMap[it] = json.getInt(it) }
                    } catch (e: Exception) {}
                }

                val savedWbContribJson = prefs.getString("saved_wb_contributions", null)
                val wbContribMap = mutableMapOf<String, WorldBossContribution>()
                if (savedWbContribJson != null) {
                    try {
                        val json = JSONObject(savedWbContribJson)
                        json.keys().forEach { bossId ->
                            val bObj = json.getJSONObject(bossId)
                            wbContribMap[bossId] = WorldBossContribution(
                                bossId = bossId,
                                totalDamage = bObj.optLong("total_damage", 0L),
                                highestSingleHit = bObj.optLong("highest_hit", 0L),
                                battlesCompleted = bObj.optInt("battles", 0),
                                victories = bObj.optInt("victories", 0),
                                participationCount = bObj.optInt("participation", 0),
                                contributionScore = bObj.optLong("score", 0L),
                                claimedRewardTier = if (bObj.has("claimed_tier") && !bObj.isNull("claimed_tier")) bObj.getString("claimed_tier") else null
                            )
                        }
                    } catch (e: Exception) {}
                }

                val savedWeeklyObjectivesJson = prefs.getString("saved_weekly_objectives", null)
                val weeklyObjectivesList = mutableListOf<WeeklyObjective>()
                if (savedWeeklyObjectivesJson != null) {
                    try {
                        val arr = org.json.JSONArray(savedWeeklyObjectivesJson)
                        val defaults = EndgameCatalog.createDefaultWeeklyObjectives()
                        for (i in 0 until arr.length()) {
                            val oObj = arr.getJSONObject(i)
                            val id = oObj.getString("id")
                            val defObj = defaults.find { it.id == id }
                            if (defObj != null) {
                                weeklyObjectivesList.add(
                                    defObj.copy(
                                        progress = oObj.optLong("progress", defObj.progress),
                                        isClaimed = oObj.optBoolean("is_claimed", defObj.isClaimed)
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {}
                }
                val finalWeeklyObjectives = if (weeklyObjectivesList.size == 5) weeklyObjectivesList else EndgameCatalog.createDefaultWeeklyObjectives()

                val savedArenaRating = prefs.getInt("saved_arena_rating", ArenaRatingCalculator.STARTING_RATING)
                val savedArenaPeakRating = prefs.getInt("saved_arena_peak_rating", maxOf(ArenaRatingCalculator.STARTING_RATING, savedArenaRating))
                val savedArenaWins = prefs.getInt("saved_arena_wins", 0)
                val savedArenaLosses = prefs.getInt("saved_arena_losses", 0)
                val savedArenaCurrentStreak = prefs.getInt("saved_arena_current_streak", 0)
                val savedArenaHighestStreak = prefs.getInt("saved_arena_highest_streak", 0)
                val savedArenaAttempts = prefs.getInt("saved_arena_attempts", ArenaCatalog.MAX_DAILY_ATTEMPTS)
                val savedArenaAttemptDate = prefs.getString("saved_arena_attempt_date", "") ?: ""
                val savedArenaPoints = prefs.getInt("saved_arena_points", 0)
                val savedArenaFirstWinDate = prefs.getString("saved_arena_first_win_date", "") ?: ""
                val savedArenaSeasonClaimed = prefs.getStringSet("saved_arena_season_claimed", null) ?: emptySet()
                val savedArenaHistoryJson = prefs.getString("saved_arena_history", null)
                val arenaHistoryList = mutableListOf<ArenaMatchRecord>()
                if (savedArenaHistoryJson != null) {
                    try {
                        val arr = org.json.JSONArray(savedArenaHistoryJson)
                        for (i in 0 until arr.length()) {
                            val o = arr.getJSONObject(i)
                            arenaHistoryList.add(
                                ArenaMatchRecord(
                                    matchId = o.getString("match_id"),
                                    timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                                    dateFormatted = o.optString("date_formatted", ""),
                                    opponentName = o.optString("opp_name", ""),
                                    opponentHeroName = o.optString("opp_hero", ""),
                                    playerHeroName = o.optString("player_hero", ""),
                                    opponentArchetype = o.optString("opp_archetype", ""),
                                    result = ArenaMatchResult.valueOf(o.optString("result", "VICTORY")),
                                    ratingBefore = o.optInt("rating_before", 1000),
                                    ratingAfter = o.optInt("rating_after", 1000),
                                    ratingChange = o.optInt("rating_change", 0),
                                    turns = o.optInt("turns", 5),
                                    durationSeconds = o.optInt("duration", 45),
                                    goldAwarded = o.optInt("gold", 0),
                                    xpAwarded = o.optInt("xp", 0),
                                    shardsAwarded = o.optInt("shards", 0),
                                    arenaPointsAwarded = o.optInt("arena_points", 0)
                                )
                            )
                        }
                    } catch (e: Exception) {}
                }

                val savedSeasonId = prefs.getString("saved_arena_season_id", "season_1") ?: "season_1"
                val savedSeasonNumber = prefs.getInt("saved_arena_season_number", 1)
                val savedSeasonStart = prefs.getLong("saved_arena_season_start", 0L)
                val savedSeasonEnd = prefs.getLong("saved_arena_season_end", Long.MAX_VALUE)
                val restoredSeason = ArenaSeason(
                    seasonId = savedSeasonId,
                    seasonNumber = savedSeasonNumber,
                    startDateMs = savedSeasonStart,
                    endDateMs = savedSeasonEnd,
                    rating = savedArenaRating,
                    peakRating = savedArenaPeakRating,
                    wins = savedArenaWins,
                    losses = savedArenaLosses,
                    matches = savedArenaWins + savedArenaLosses
                )

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
                    lastLoginRewardDate = savedLastLoginRewardDate,
                    alliances = restoredAlliances,
                    playerAllianceId = savedPlayerAllianceId,
                    eventTokens = savedEventTokens,
                    worldBossCurrentHp = savedWbHp,
                    worldBossContributions = wbContribMap,
                    raidDailyAttempts = savedRaidAttempts,
                    lastRaidAttemptDate = savedRaidDate,
                    raidStageProgress = raidProgressMap,
                    trialStars = trialStarsMap,
                    weeklyObjectives = finalWeeklyObjectives,
                    weeklyObjectiveWeek = savedWeeklyWeek,
                    eventShopPurchases = shopPurchasesMap,
                    arenaRating = savedArenaRating,
                    arenaPeakRating = savedArenaPeakRating,
                    arenaWins = savedArenaWins,
                    arenaLosses = savedArenaLosses,
                    arenaCurrentStreak = savedArenaCurrentStreak,
                    arenaHighestStreak = savedArenaHighestStreak,
                    arenaDailyAttempts = savedArenaAttempts,
                    lastArenaAttemptDate = savedArenaAttemptDate,
                    arenaPoints = savedArenaPoints,
                    arenaFirstWinClaimedDate = savedArenaFirstWinDate,
                    arenaSeasonRewardsClaimed = savedArenaSeasonClaimed,
                    arenaMatchHistory = arenaHistoryList,
                    arenaCurrentSeason = restoredSeason
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

            val alliancesArray = org.json.JSONArray()
            current.alliances.forEach { a ->
                val aObj = JSONObject().apply {
                    put("alliance_id", a.allianceId)
                    put("name", a.name)
                    put("emblem", a.emblem)
                    put("description", a.description)
                    put("level", a.level)
                    put("xp", a.xp)
                    put("created_date", a.createdDate)
                    val mArr = org.json.JSONArray()
                    a.members.forEach { m ->
                        mArr.put(JSONObject().apply {
                            put("player_id", m.playerId)
                            put("name", m.name)
                            put("player_level", m.playerLevel)
                            put("hero_id", m.heroId)
                            put("combat_power", m.combatPower)
                            put("role", m.role.name)
                            put("joined_date", m.joinedDate)
                            put("battles_won", m.battlesWon)
                            put("damage_dealt", m.damageDealt)
                            put("campaign_clears", m.campaignClears)
                            put("quests_completed", m.questsCompleted)
                            put("contribution_score", m.contributionScore)
                        })
                    }
                    put("members", mArr)
                }
                alliancesArray.put(aObj)
            }

            val claimedRewardsSet = current.playerProgress.claimedLevelRewards.map { it.toString() }.toSet()

            val raidProgressJson = JSONObject()
            current.raidStageProgress.forEach { (k, v) -> raidProgressJson.put(k, v) }

            val trialStarsJson = JSONObject()
            current.trialStars.forEach { (k, v) -> trialStarsJson.put(k, v) }

            val shopPurchasesJson = JSONObject()
            current.eventShopPurchases.forEach { (k, v) -> shopPurchasesJson.put(k, v) }

            val wbContribJson = JSONObject()
            current.worldBossContributions.forEach { (k, v) ->
                val bObj = JSONObject()
                bObj.put("total_damage", v.totalDamage)
                bObj.put("highest_hit", v.highestSingleHit)
                bObj.put("battles", v.battlesCompleted)
                bObj.put("victories", v.victories)
                bObj.put("participation", v.participationCount)
                bObj.put("score", v.contributionScore)
                bObj.put("claimed_tier", v.claimedRewardTier)
                wbContribJson.put(k, bObj)
            }

            val weeklyArr = org.json.JSONArray()
            current.weeklyObjectives.forEach { obj ->
                val oObj = JSONObject()
                oObj.put("id", obj.id)
                oObj.put("progress", obj.progress)
                oObj.put("is_claimed", obj.isClaimed)
                weeklyArr.put(oObj)
            }

            val historyArr = org.json.JSONArray()
            current.arenaMatchHistory.take(20).forEach { m ->
                val o = JSONObject()
                o.put("match_id", m.matchId)
                o.put("timestamp", m.timestamp)
                o.put("date_formatted", m.dateFormatted)
                o.put("opp_name", m.opponentName)
                o.put("opp_hero", m.opponentHeroName)
                o.put("player_hero", m.playerHeroName)
                o.put("opp_archetype", m.opponentArchetype)
                o.put("result", m.result.name)
                o.put("rating_before", m.ratingBefore)
                o.put("rating_after", m.ratingAfter)
                o.put("rating_change", m.ratingChange)
                o.put("turns", m.turns)
                o.put("duration", m.durationSeconds)
                o.put("gold", m.goldAwarded)
                o.put("xp", m.xpAwarded)
                o.put("shards", m.shardsAwarded)
                o.put("arena_points", m.arenaPointsAwarded)
                historyArr.put(o)
            }

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
                .putInt("saved_login_streak", current.playerProgress.loginStreak)
                .putInt("saved_highest_login_streak", current.playerProgress.highestLoginStreak)
                .putString("saved_player_alliance_id", current.playerAllianceId)
                .putString("saved_player_alliance_name", current.playerProgress.allianceName)
                .putString("saved_player_alliance_role", current.playerProgress.allianceRole)
                .putLong("saved_alliance_contribution", current.playerProgress.allianceContribution)
                .putString("saved_alliances_json", alliancesArray.toString())
                .putInt("saved_event_tokens", current.eventTokens)
                .putLong("saved_wb_hp", current.worldBossCurrentHp)
                .putInt("saved_raid_attempts", current.raidDailyAttempts)
                .putString("saved_raid_date", current.lastRaidAttemptDate)
                .putString("saved_weekly_week", current.weeklyObjectiveWeek)
                .putString("saved_raid_progress", raidProgressJson.toString())
                .putString("saved_trial_stars", trialStarsJson.toString())
                .putString("saved_shop_purchases", shopPurchasesJson.toString())
                .putString("saved_wb_contributions", wbContribJson.toString())
                .putString("saved_weekly_objectives", weeklyArr.toString())
                .putInt("saved_arena_rating", current.arenaRating)
                .putInt("saved_arena_peak_rating", current.arenaPeakRating)
                .putInt("saved_arena_wins", current.arenaWins)
                .putInt("saved_arena_losses", current.arenaLosses)
                .putInt("saved_arena_current_streak", current.arenaCurrentStreak)
                .putInt("saved_arena_highest_streak", current.arenaHighestStreak)
                .putInt("saved_arena_attempts", current.arenaDailyAttempts)
                .putString("saved_arena_attempt_date", current.lastArenaAttemptDate)
                .putInt("saved_arena_points", current.arenaPoints)
                .putString("saved_arena_first_win_date", current.arenaFirstWinClaimedDate)
                .putStringSet("saved_arena_season_claimed", current.arenaSeasonRewardsClaimed)
                .putString("saved_arena_history", historyArr.toString())
                .putString("saved_arena_season_id", current.arenaCurrentSeason.seasonId)
                .putInt("saved_arena_season_number", current.arenaCurrentSeason.seasonNumber)
                .putLong("saved_arena_season_start", current.arenaCurrentSeason.startDateMs)
                .putLong("saved_arena_season_end", current.arenaCurrentSeason.endDateMs)
                // Idempotency ledger for grantEntitlements() - written on every save (not just
                // after a purchase) so it's never stale relative to whatever else is persisted.
                .putStringSet("saved_processed_purchase_ids", processedPurchaseIds.toSet())
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
     * Adds Player XP and levels up the player if threshold is reached (Phase 7D Sections 4, 5, 6).
     */
    @Synchronized
    fun addPlayerXp(xp: Int): List<PlayerLevelReward> {
        if (xp <= 0) return emptyList()
        val unlockedRewards = mutableListOf<PlayerLevelReward>()

        _economyState.update { current ->
            var curLevel = current.playerProgress.playerLevel
            var curXp = current.playerProgress.playerXp + xp

            while (curLevel < PlayerProgressionConfig.MAX_PLAYER_LEVEL) {
                val reqXp = PlayerProgressionConfig.getXpRequiredForNextLevel(curLevel)
                if (curXp >= reqXp && reqXp > 0) {
                    curXp -= reqXp
                    curLevel += 1

                    val reward = PlayerProgressionConfig.getLevelReward(curLevel)
                    if (reward != null) {
                        unlockedRewards.add(reward)
                    }
                } else {
                    break
                }
            }

            val updatedProgress = current.playerProgress.copy(
                playerLevel = curLevel,
                playerXp = curXp
            )

            current.copy(
                playerProgress = updatedProgress,
                activeDeck = current.activeDeck.createDefensiveCopy()
            )
        }
        saveToPersistence()
        return unlockedRewards
    }

    /**
     * Claims configured level-up reward atomically (Phase 7D Section 6, 21, 26).
     * Never grants a level-up reward twice for the same player level.
     */
    @Synchronized
    fun claimPlayerLevelReward(level: Int): Result<PlayerLevelReward> {
        val current = _economyState.value
        if (current.playerProgress.playerLevel < level) {
            return Result.failure(IllegalStateException("Player level $level not reached yet (current: ${current.playerProgress.playerLevel})."))
        }
        if (current.playerProgress.claimedLevelRewards.contains(level)) {
            return Result.failure(IllegalStateException("Level $level reward has already been claimed."))
        }
        val reward = PlayerProgressionConfig.getLevelReward(level)
            ?: return Result.failure(IllegalArgumentException("No reward configured for level $level."))

        _economyState.update { state ->
            val newClaimed = state.playerProgress.claimedLevelRewards + level
            val newGold = state.gold + reward.gold
            val newGems = state.mythGems + reward.mythGems
            val newCardShards = state.cardShards.toMutableMap()
            if (reward.cardShards > 0) {
                val targetCard = "c_heroic_rage"
                newCardShards[targetCard] = (newCardShards[targetCard] ?: 0) + reward.cardShards
            }
            val newHeroShards = state.heroShards.toMutableMap()
            if (reward.heroShards > 0) {
                val targetHero = state.selectedHeroId
                newHeroShards[targetHero] = (newHeroShards[targetHero] ?: 0) + reward.heroShards
            }

            state.copy(
                gold = newGold,
                mythGems = newGems,
                cardShards = newCardShards,
                heroShards = newHeroShards,
                playerProgress = state.playerProgress.copy(claimedLevelRewards = newClaimed),
                activeDeck = state.activeDeck.createDefensiveCopy()
            )
        }
        saveToPersistence()
        return Result.success(reward)
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

        if (_economyState.value.playerAllianceId != null) {
            addAllianceContribution(battlesWon = 0, damageDealt = 0L, campaignClears = 0, questsCompleted = 1)
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
                    raidDailyAttempts = 3,
                    lastRaidAttemptDate = currentDate,
                    arenaDailyAttempts = ArenaCatalog.MAX_DAILY_ATTEMPTS,
                    lastArenaAttemptDate = currentDate,
                    activeDeck = it.activeDeck.createDefensiveCopy()
                )
            }
            saveToPersistence()
        }
        checkWeeklyReset()
        checkSeasonReset()
    }

    /**
     * Calendar-based weekly objective reset (Phase 10 Section 13).
     * Does NOT interfere with daily quests or daily login rewards.
     */
    @Synchronized
    fun checkWeeklyReset(forceWeek: String? = null) {
        val currentWeek = forceWeek ?: EndgameCatalog.getCurrentWeekId()
        val current = _economyState.value
        if (current.weeklyObjectiveWeek != currentWeek) {
            _economyState.update {
                it.copy(
                    weeklyObjectives = EndgameCatalog.createDefaultWeeklyObjectives(),
                    weeklyObjectiveWeek = currentWeek,
                    activeDeck = it.activeDeck.createDefensiveCopy()
                )
            }
            saveToPersistence()
        }
    }

    /**
     * Updates weekly objective progress idempotently (Phase 10 Section 13).
     */
    @Synchronized
    fun updateWeeklyObjectiveProgress(type: WeeklyObjectiveType, amount: Long) {
        if (amount <= 0L) return
        _economyState.update { current ->
            val updated = current.weeklyObjectives.map { obj ->
                if (obj.type == type && !obj.isClaimed) {
                    val newProgress = minOf(obj.target, obj.progress + amount)
                    obj.copy(progress = newProgress)
                } else obj
            }
            current.copy(
                weeklyObjectives = updated,
                activeDeck = current.activeDeck.createDefensiveCopy()
            )
        }
        saveToPersistence()
    }

    /**
     * Claims a completed Weekly Objective reward atomically (Phase 10 Section 13).
     */
    @Synchronized
    fun claimWeeklyObjective(objectiveId: String): Result<WeeklyObjective> {
        val current = _economyState.value
        val obj = current.weeklyObjectives.find { it.id == objectiveId }
            ?: return Result.failure(IllegalArgumentException("Weekly objective '$objectiveId' not found."))

        if (!obj.isCompleted) {
            return Result.failure(IllegalStateException("Objective not completed yet (${obj.progress}/${obj.target})."))
        }
        if (obj.isClaimed) {
            return Result.failure(IllegalStateException("Weekly objective reward already claimed."))
        }

        val updatedObjectives = current.weeklyObjectives.map {
            if (it.id == objectiveId) it.copy(isClaimed = true) else it
        }

        val targetHero = current.selectedHeroId
        val updatedHeroShards = current.heroShards.toMutableMap()
        if (obj.heroShardsReward > 0) {
            updatedHeroShards[targetHero] = (updatedHeroShards[targetHero] ?: 0) + obj.heroShardsReward
        }

        val preservedDeck = current.activeDeck.createDefensiveCopy()

        _economyState.update { state ->
            state.copy(
                gold = state.gold + obj.goldReward,
                eventTokens = state.eventTokens + obj.eventTokensReward,
                heroShards = updatedHeroShards,
                weeklyObjectives = updatedObjectives,
                activeDeck = preservedDeck
            )
        }
        saveToPersistence()
        return Result.success(obj.copy(isClaimed = true))
    }

    /**
     * Adds Event Currency (EVENT_TOKENS) safely (Phase 10 Section 11).
     * Separate from Gold and Gems. Never connects to real-money payments.
     */
    @Synchronized
    fun addEventTokens(amount: Int) {
        if (amount <= 0) return
        _economyState.update { current ->
            current.copy(
                eventTokens = current.eventTokens + amount,
                activeDeck = current.activeDeck.createDefensiveCopy()
            )
        }
        saveToPersistence()
    }

    /**
     * Records World Boss battle outcome, deals persistent boss damage, and tracks contribution (Phase 10 Sections 3, 4, 5).
     */
    @Synchronized
    fun recordWorldBossBattle(bossId: String, damageDealt: Long, isVictory: Boolean = false) {
        val current = _economyState.value
        val curHp = current.worldBossCurrentHp
        val remainingHp = maxOf(0L, curHp - damageDealt)

        val existingContrib = current.worldBossContributions[bossId] ?: WorldBossContribution(bossId = bossId)
        val newTotalDamage = existingContrib.totalDamage + damageDealt
        val newHighestHit = maxOf(existingContrib.highestSingleHit, damageDealt)
        val newBattles = existingContrib.battlesCompleted + 1
        val newVictories = if (isVictory) existingContrib.victories + 1 else existingContrib.victories
        val newParticipation = existingContrib.participationCount + 1
        val newScore = newTotalDamage + (newBattles * 1_000L) + (newVictories * 5_000L)

        val updatedContrib = existingContrib.copy(
            totalDamage = newTotalDamage,
            highestSingleHit = newHighestHit,
            battlesCompleted = newBattles,
            victories = newVictories,
            participationCount = newParticipation,
            contributionScore = newScore
        )

        val updatedMap = current.worldBossContributions.toMutableMap()
        updatedMap[bossId] = updatedContrib

        // Award 25 Event Tokens for participation
        val newTokens = current.eventTokens + 25

        _economyState.update { state ->
            state.copy(
                worldBossCurrentHp = remainingHp,
                worldBossContributions = updatedMap,
                eventTokens = newTokens,
                activeDeck = state.activeDeck.createDefensiveCopy()
            )
        }

        updateWeeklyObjectiveProgress(WeeklyObjectiveType.DEAL_WORLD_BOSS_DAMAGE, damageDealt)
        addAllianceContribution(
            battlesWon = if (isVictory) 1 else 0,
            damageDealt = damageDealt
        )
        saveToPersistence()
    }

    /**
     * Claims World Boss reward tier based on accumulated personal damage contribution (Phase 10 Section 6).
     */
    @Synchronized
    fun claimWorldBossReward(bossId: String): Result<WorldBossRewardTier> {
        val current = _economyState.value
        val contrib = current.worldBossContributions[bossId]
            ?: return Result.failure(IllegalStateException("No contribution recorded for boss '$bossId'."))

        if (contrib.participationCount <= 0) {
            return Result.failure(IllegalStateException("No participation recorded for boss '$bossId'."))
        }
        if (contrib.claimedRewardTier != null) {
            return Result.failure(IllegalStateException("World Boss rewards have already been claimed."))
        }

        val tier = WorldBossRewardTier.determineTier(contrib.totalDamage)
        val updatedContrib = contrib.copy(claimedRewardTier = tier.name)
        val updatedMap = current.worldBossContributions.toMutableMap()
        updatedMap[bossId] = updatedContrib

        val targetHero = current.selectedHeroId
        val updatedHeroShards = current.heroShards.toMutableMap()
        if (tier.heroShardsReward > 0) {
            updatedHeroShards[targetHero] = (updatedHeroShards[targetHero] ?: 0) + tier.heroShardsReward
        }

        val targetCard = "c_titans_wrath"
        val updatedCardShards = current.cardShards.toMutableMap()
        if (tier.cardShardsReward > 0) {
            updatedCardShards[targetCard] = (updatedCardShards[targetCard] ?: 0) + tier.cardShardsReward
        }

        _economyState.update { state ->
            state.copy(
                gold = state.gold + tier.goldReward,
                eventTokens = state.eventTokens + tier.eventTokensReward,
                heroShards = updatedHeroShards,
                cardShards = updatedCardShards,
                worldBossContributions = updatedMap,
                activeDeck = state.activeDeck.createDefensiveCopy()
            )
        }
        saveToPersistence()
        return Result.success(tier)
    }

    /**
     * Consumes one daily raid attempt (Phase 10 Section 9).
     * Returns true if attempt consumed successfully, false if no attempts left.
     */
    @Synchronized
    fun consumeRaidAttempt(): Boolean {
        val current = _economyState.value
        if (current.raidDailyAttempts <= 0) {
            return false
        }
        _economyState.update { state ->
            state.copy(
                raidDailyAttempts = state.raidDailyAttempts - 1,
                lastRaidAttemptDate = MythosDateUtil.getCurrentLocalDate(),
                activeDeck = state.activeDeck.createDefensiveCopy()
            )
        }
        updateWeeklyObjectiveProgress(WeeklyObjectiveType.PARTICIPATE_RAIDS, 1L)
        saveToPersistence()
        return true
    }

    /**
     * Records Raid Stage clear and awards configured rewards (Phase 10 Section 8, 9).
     */
    @Synchronized
    fun recordRaidStageClear(raidId: String, stageNumber: Int, goldReward: Int, tokenReward: Int): Boolean {
        val current = _economyState.value
        val curProgress = current.raidStageProgress[raidId] ?: 0
        val newProgress = maxOf(curProgress, stageNumber)
        val updatedMap = current.raidStageProgress.toMutableMap()
        updatedMap[raidId] = newProgress

        _economyState.update { state ->
            state.copy(
                gold = state.gold + goldReward,
                eventTokens = state.eventTokens + tokenReward,
                raidStageProgress = updatedMap,
                activeDeck = state.activeDeck.createDefensiveCopy()
            )
        }
        updateWeeklyObjectiveProgress(WeeklyObjectiveType.CLEAR_ENDGAME_STAGES, 1L)
        saveToPersistence()
        return true
    }

    /**
     * Records Endgame Trial clear and awards stars and rewards (Phase 10 Section 2).
     */
    @Synchronized
    fun recordTrialClear(
        stageId: String,
        difficulty: EndgameDifficulty,
        stars: Int,
        goldReward: Int,
        xpReward: Int,
        tokenReward: Int
    ): Boolean {
        val current = _economyState.value
        val curStars = current.trialStars[stageId] ?: 0
        val newStars = maxOf(curStars, stars)
        val updatedStars = current.trialStars.toMutableMap()
        updatedStars[stageId] = newStars

        val clearedDiffs = current.trialClearedDifficulties[stageId] ?: emptySet()
        val updatedDiffs = current.trialClearedDifficulties.toMutableMap()
        updatedDiffs[stageId] = clearedDiffs + difficulty.id

        _economyState.update { state ->
            state.copy(
                gold = state.gold + goldReward,
                eventTokens = state.eventTokens + tokenReward,
                trialStars = updatedStars,
                trialClearedDifficulties = updatedDiffs,
                activeDeck = state.activeDeck.createDefensiveCopy()
            )
        }
        addPlayerXp(xpReward)
        updateWeeklyObjectiveProgress(WeeklyObjectiveType.CLEAR_ENDGAME_STAGES, 1L)
        saveToPersistence()
        return true
    }

    /**
     * Purchases an item from the Event Reward Shop using Event Tokens (Phase 10 Section 12).
     * 100% IN-GAME REWARD SHOP — ABSOLUTE PAYMENT RULE ENFORCED.
     */
    @Synchronized
    fun purchaseEventShopItem(itemId: String): Result<EventShopItem> {
        val item = EndgameCatalog.DEFAULT_EVENT_SHOP_ITEMS.find { it.itemId == itemId }
            ?: return Result.failure(IllegalArgumentException("Event shop item '$itemId' not found."))

        val current = _economyState.value
        val purchasedCount = current.eventShopPurchases[itemId] ?: 0

        if (purchasedCount >= item.purchaseLimit) {
            return Result.failure(IllegalStateException("Purchase limit reached for '${item.name}'."))
        }
        if (current.eventTokens < item.tokenPrice) {
            val needed = item.tokenPrice - current.eventTokens
            return Result.failure(IllegalStateException("Insufficient Event Tokens. Need $needed more."))
        }

        val updatedPurchases = current.eventShopPurchases.toMutableMap()
        updatedPurchases[itemId] = purchasedCount + 1

        val newTokens = current.eventTokens - item.tokenPrice
        var newGold = current.gold + item.rewardGold
        val updatedHeroShards = current.heroShards.toMutableMap()
        val updatedCardShards = current.cardShards.toMutableMap()
        val updatedOwnedCards = current.ownedCardIds.toMutableSet()
        val updatedCardCounts = current.ownedCardCounts.toMutableMap()
        val updatedFrames = current.ownedFrames.toMutableSet()

        if (item.rewardHeroShards > 0) {
            val hId = item.rewardHeroId
            updatedHeroShards[hId] = (updatedHeroShards[hId] ?: 0) + item.rewardHeroShards
        }
        if (item.rewardCardShards > 0) {
            val cId = item.rewardCardId
            updatedCardShards[cId] = (updatedCardShards[cId] ?: 0) + item.rewardCardShards
        }
        if (item.category == "EXCLUSIVE_CARD") {
            addOrDuplicateCardInternal(item.rewardCardId, CardRarity.LEGENDARY, updatedOwnedCards, updatedCardCounts, updatedCardShards)
        }
        if (item.category == "COSMETIC" && item.rewardCosmeticId.isNotBlank()) {
            updatedFrames.add(item.rewardCosmeticId)
        }

        _economyState.update { state ->
            state.copy(
                eventTokens = newTokens,
                gold = newGold,
                heroShards = updatedHeroShards,
                cardShards = updatedCardShards,
                ownedCardIds = updatedOwnedCards,
                ownedCardCounts = updatedCardCounts,
                ownedFrames = updatedFrames,
                eventShopPurchases = updatedPurchases,
                activeDeck = state.activeDeck.createDefensiveCopy()
            )
        }
        saveToPersistence()
        return Result.success(item)
    }

    /**
     * Checks if daily login reward is claimable for today (Phase 7D Section 16, 18).
     */
    fun canClaimDailyLoginReward(currentDateOverride: String? = null): Boolean {
        val today = currentDateOverride ?: MythosDateUtil.getCurrentLocalDate()
        return _economyState.value.lastLoginRewardDate != today
    }

    /**
     * Claims the daily login reward atomically (Phase 7D Section 16, 17 & Phase 8 Section 5).
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

        val isConsecutive = MythosDateUtil.isConsecutiveDay(state.lastLoginRewardDate, today)
        val curStreak = state.playerProgress.loginStreak
        val newStreak = if (isConsecutive) curStreak + 1 else 1
        val newHighestStreak = maxOf(state.playerProgress.highestLoginStreak, newStreak)

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
                activeDeck = preservedDeck,
                playerProgress = current.playerProgress.copy(
                    loginStreak = newStreak,
                    highestLoginStreak = newHighestStreak
                )
            )
        }
        saveToPersistence()
        return Result.success(reward)
    }

    private fun createLocalPlayerMember(role: AllianceRole): AllianceMember {
        val state = _economyState.value
        val p = state.playerProgress
        val hero = HeroCatalog.findHero(state.selectedHeroId) ?: HeroCatalog.HERCULES
        val scaled = HeroProgressionConfig.getScaledStats(hero, state.heroProgression[state.selectedHeroId] ?: 1)
        val power = (scaled.hp / 10) + (scaled.attack / 2) + (scaled.defense / 2)
        return AllianceMember(
            playerId = "player_local",
            name = "Champion",
            playerLevel = p.playerLevel,
            heroId = state.selectedHeroId,
            combatPower = power,
            role = role,
            joinedDate = MythosDateUtil.getCurrentLocalDate(),
            battlesWon = p.totalVictories,
            damageDealt = p.totalDamageDealt,
            campaignClears = p.totalCampaignStagesCleared,
            questsCompleted = 0,
            contributionScore = (p.totalVictories * 50L) + (p.totalCampaignStagesCleared * 100L)
        )
    }

    /**
     * Creates a new Alliance (Phase 8 Section 1).
     * Costs 5,000 Gold. Caller becomes LEADER.
     */
    @Synchronized
    fun createAlliance(name: String, emblem: String, description: String): Result<Alliance> {
        val current = _economyState.value
        if (current.playerAllianceId != null) {
            return Result.failure(IllegalStateException("Already in an Alliance. Leave your current Alliance first."))
        }
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Alliance name cannot be blank."))
        }
        if (current.gold < Alliance.CREATE_GOLD_COST) {
            return Result.failure(IllegalStateException("Insufficient Gold. Need ${Alliance.CREATE_GOLD_COST} Gold to establish an Alliance."))
        }

        val newAlliance = Alliance(
            allianceId = "alliance_" + java.util.UUID.randomUUID().toString().take(8),
            name = trimmedName,
            emblem = emblem.ifBlank { "emblem_gold_eagle" },
            description = description.ifBlank { "Warriors of Mount Olympus united in divine glory." },
            level = 1,
            xp = 0,
            createdDate = MythosDateUtil.getCurrentLocalDate(),
            members = listOf(createLocalPlayerMember(AllianceRole.LEADER))
        )

        _economyState.update { state ->
            state.copy(
                gold = state.gold - Alliance.CREATE_GOLD_COST,
                alliances = state.alliances + newAlliance,
                playerAllianceId = newAlliance.allianceId,
                playerProgress = state.playerProgress.copy(
                    allianceId = newAlliance.allianceId,
                    allianceName = newAlliance.name,
                    allianceRole = AllianceRole.LEADER.title
                ),
                activeDeck = state.activeDeck.createDefensiveCopy()
            )
        }
        saveToPersistence()
        return Result.success(newAlliance)
    }

    /**
     * Joins an existing Alliance (Phase 8 Section 1).
     * Enforces maximum 20 members rule.
     */
    @Synchronized
    fun joinAlliance(allianceId: String): Result<Alliance> {
        val current = _economyState.value
        if (current.playerAllianceId != null) {
            return Result.failure(IllegalStateException("Already in an Alliance. Leave your current Alliance first."))
        }
        val targetAlliance = current.alliances.find { it.allianceId == allianceId }
            ?: return Result.failure(IllegalArgumentException("Alliance not found."))

        if (targetAlliance.isFull) {
            return Result.failure(IllegalStateException("Alliance is full (maximum ${Alliance.MAX_MEMBERS} members)."))
        }

        val localMember = createLocalPlayerMember(AllianceRole.MEMBER)
        val updatedMembers = targetAlliance.members + localMember
        val updatedAlliance = targetAlliance.copy(members = updatedMembers)

        _economyState.update { state ->
            val updatedList = state.alliances.map { if (it.allianceId == allianceId) updatedAlliance else it }
            state.copy(
                alliances = updatedList,
                playerAllianceId = allianceId,
                playerProgress = state.playerProgress.copy(
                    allianceId = allianceId,
                    allianceName = targetAlliance.name,
                    allianceRole = AllianceRole.MEMBER.title
                ),
                activeDeck = state.activeDeck.createDefensiveCopy()
            )
        }
        saveToPersistence()
        return Result.success(updatedAlliance)
    }

    /**
     * Leaves current Alliance (Phase 8 Section 1).
     * If player is LEADER and other members exist, passes leadership to next senior member.
     */
    @Synchronized
    fun leaveAlliance(): Result<Unit> {
        val current = _economyState.value
        val allianceId = current.playerAllianceId
            ?: return Result.failure(IllegalStateException("Not currently in an Alliance."))

        val alliance = current.alliances.find { it.allianceId == allianceId }
            ?: return Result.failure(IllegalArgumentException("Alliance not found."))

        val isLeader = current.isLeaderOfAlliance
        val remainingMembers = alliance.members.filter { it.playerId != "player_local" }

        val updatedAlliances = if (remainingMembers.isEmpty()) {
            current.alliances.filter { it.allianceId != allianceId }
        } else {
            val finalMembers = if (isLeader) {
                val newLeader = remainingMembers.maxByOrNull { it.contributionScore } ?: remainingMembers.first()
                remainingMembers.map {
                    if (it.playerId == newLeader.playerId) it.copy(role = AllianceRole.LEADER) else it
                }
            } else {
                remainingMembers
            }
            current.alliances.map {
                if (it.allianceId == allianceId) it.copy(members = finalMembers) else it
            }
        }

        _economyState.update { state ->
            state.copy(
                alliances = updatedAlliances,
                playerAllianceId = null,
                playerProgress = state.playerProgress.copy(
                    allianceId = null,
                    allianceName = null,
                    allianceRole = null,
                    allianceContribution = 0L
                ),
                activeDeck = state.activeDeck.createDefensiveCopy()
            )
        }
        saveToPersistence()
        return Result.success(Unit)
    }

    /**
     * Promotes an Alliance member to Officer (Leader only).
     */
    @Synchronized
    fun promoteMember(targetPlayerId: String): Result<Alliance> {
        val current = _economyState.value
        val alliance = current.playerAlliance
            ?: return Result.failure(IllegalStateException("Not currently in an Alliance."))

        if (!current.isLeaderOfAlliance) {
            return Result.failure(IllegalStateException("Only the Alliance Leader can promote members."))
        }

        val target = alliance.members.find { it.playerId == targetPlayerId }
            ?: return Result.failure(IllegalArgumentException("Member not found."))

        if (target.role == AllianceRole.LEADER) {
            return Result.failure(IllegalStateException("Leader cannot be promoted."))
        }
        if (target.role == AllianceRole.OFFICER) {
            return Result.failure(IllegalStateException("Member is already an Officer."))
        }

        val updatedMembers = alliance.members.map {
            if (it.playerId == targetPlayerId) it.copy(role = AllianceRole.OFFICER) else it
        }
        val updatedAlliance = alliance.copy(members = updatedMembers)

        _economyState.update { state ->
            state.copy(
                alliances = state.alliances.map { if (it.allianceId == alliance.allianceId) updatedAlliance else it },
                activeDeck = state.activeDeck.createDefensiveCopy()
            )
        }
        saveToPersistence()
        return Result.success(updatedAlliance)
    }

    /**
     * Demotes an Alliance Officer to Member (Leader only).
     */
    @Synchronized
    fun demoteMember(targetPlayerId: String): Result<Alliance> {
        val current = _economyState.value
        val alliance = current.playerAlliance
            ?: return Result.failure(IllegalStateException("Not currently in an Alliance."))

        if (!current.isLeaderOfAlliance) {
            return Result.failure(IllegalStateException("Only the Alliance Leader can demote officers."))
        }

        val target = alliance.members.find { it.playerId == targetPlayerId }
            ?: return Result.failure(IllegalArgumentException("Member not found."))

        if (target.role != AllianceRole.OFFICER) {
            return Result.failure(IllegalStateException("Member is not an Officer."))
        }

        val updatedMembers = alliance.members.map {
            if (it.playerId == targetPlayerId) it.copy(role = AllianceRole.MEMBER) else it
        }
        val updatedAlliance = alliance.copy(members = updatedMembers)

        _economyState.update { state ->
            state.copy(
                alliances = state.alliances.map { if (it.allianceId == alliance.allianceId) updatedAlliance else it },
                activeDeck = state.activeDeck.createDefensiveCopy()
            )
        }
        saveToPersistence()
        return Result.success(updatedAlliance)
    }

    /**
     * Transfers Leadership to another member (Leader only).
     */
    @Synchronized
    fun transferLeadership(newLeaderPlayerId: String): Result<Alliance> {
        val current = _economyState.value
        val alliance = current.playerAlliance
            ?: return Result.failure(IllegalStateException("Not currently in an Alliance."))

        if (!current.isLeaderOfAlliance) {
            return Result.failure(IllegalStateException("Only the Alliance Leader can transfer leadership."))
        }

        if (newLeaderPlayerId == "player_local") {
            return Result.failure(IllegalArgumentException("You are already the leader."))
        }

        val target = alliance.members.find { it.playerId == newLeaderPlayerId }
            ?: return Result.failure(IllegalArgumentException("Target member not found."))

        val updatedMembers = alliance.members.map {
            when (it.playerId) {
                newLeaderPlayerId -> it.copy(role = AllianceRole.LEADER)
                "player_local" -> it.copy(role = AllianceRole.OFFICER)
                else -> it
            }
        }
        val updatedAlliance = alliance.copy(members = updatedMembers)

        _economyState.update { state ->
            state.copy(
                alliances = state.alliances.map { if (it.allianceId == alliance.allianceId) updatedAlliance else it },
                playerProgress = state.playerProgress.copy(allianceRole = AllianceRole.OFFICER.title),
                activeDeck = state.activeDeck.createDefensiveCopy()
            )
        }
        saveToPersistence()
        return Result.success(updatedAlliance)
    }

    /**
     * Kicks a member from the Alliance (Leader or Officer).
     */
    @Synchronized
    fun kickMember(targetPlayerId: String): Result<Alliance> {
        val current = _economyState.value
        val alliance = current.playerAlliance
            ?: return Result.failure(IllegalStateException("Not currently in an Alliance."))

        val callerRecord = current.playerMemberRecord
            ?: return Result.failure(IllegalStateException("Caller is not a member."))

        if (!callerRecord.role.canKick) {
            return Result.failure(IllegalStateException("You do not have permission to kick members."))
        }

        val target = alliance.members.find { it.playerId == targetPlayerId }
            ?: return Result.failure(IllegalArgumentException("Target member not found."))

        if (target.role == AllianceRole.LEADER) {
            return Result.failure(IllegalStateException("Cannot kick the Alliance Leader."))
        }
        if (callerRecord.role == AllianceRole.OFFICER && target.role == AllianceRole.OFFICER) {
            return Result.failure(IllegalStateException("Officers cannot kick other officers."))
        }

        val updatedMembers = alliance.members.filter { it.playerId != targetPlayerId }
        val updatedAlliance = alliance.copy(members = updatedMembers)

        _economyState.update { state ->
            state.copy(
                alliances = state.alliances.map { if (it.allianceId == alliance.allianceId) updatedAlliance else it },
                activeDeck = state.activeDeck.createDefensiveCopy()
            )
        }
        saveToPersistence()
        return Result.success(updatedAlliance)
    }

    /**
     * Adds Alliance XP and handles level progression (Phase 8 Section 3).
     */
    @Synchronized
    fun addAllianceXp(xpGained: Int) {
        if (xpGained <= 0) return
        val current = _economyState.value
        val allianceId = current.playerAllianceId ?: return
        val alliance = current.playerAlliance ?: return

        var curLevel = alliance.level
        var curXp = alliance.xp + xpGained

        while (curLevel < AllianceProgressionConfig.MAX_ALLIANCE_LEVEL) {
            val req = AllianceProgressionConfig.getXpRequiredForNextLevel(curLevel)
            if (curXp >= req && req > 0) {
                curXp -= req
                curLevel += 1
            } else {
                break
            }
        }

        val updatedAlliance = alliance.copy(level = curLevel, xp = curXp)
        _economyState.update { state ->
            state.copy(
                alliances = state.alliances.map { if (it.allianceId == allianceId) updatedAlliance else it },
                activeDeck = state.activeDeck.createDefensiveCopy()
            )
        }
        saveToPersistence()
    }

    /**
     * Records member contribution and grants Alliance XP (Phase 8 Section 4).
     */
    @Synchronized
    fun addAllianceContribution(battlesWon: Int = 0, damageDealt: Long = 0L, campaignClears: Int = 0, questsCompleted: Int = 0) {
        val current = _economyState.value
        val allianceId = current.playerAllianceId ?: return
        val alliance = current.playerAlliance ?: return

        val addedScore = (battlesWon * 50L) + (campaignClears * 100L) + (questsCompleted * 30L) + (damageDealt / 200L)

        val updatedMembers = alliance.members.map { m ->
            if (m.playerId == "player_local") {
                m.copy(
                    battlesWon = m.battlesWon + battlesWon,
                    damageDealt = m.damageDealt + damageDealt,
                    campaignClears = m.campaignClears + campaignClears,
                    questsCompleted = m.questsCompleted + questsCompleted,
                    contributionScore = m.contributionScore + addedScore
                )
            } else m
        }

        val updatedAlliance = alliance.copy(members = updatedMembers)
        val curContrib = current.playerProgress.allianceContribution + addedScore

        _economyState.update { state ->
            state.copy(
                alliances = state.alliances.map { if (it.allianceId == allianceId) updatedAlliance else it },
                playerProgress = state.playerProgress.copy(allianceContribution = curContrib),
                activeDeck = state.activeDeck.createDefensiveCopy()
            )
        }

        val allianceXpToAdd = (battlesWon * 50) + (campaignClears * 100) + (questsCompleted * 30)
        if (allianceXpToAdd > 0) {
            addAllianceXp(allianceXpToAdd)
        } else {
            saveToPersistence()
        }
    }

    fun setEventActiveForTesting(eventId: String, active: Boolean) {
        _economyState.update { current ->
            val updated = current.activeEvents.map {
                if (it.eventId == eventId) it.copy(isActive = active) else it
            }
            current.copy(activeEvents = updated)
        }
    }

    fun setAllianceForTesting(alliance: Alliance) {
        _economyState.update { current ->
            val updated = current.alliances.filter { it.allianceId != alliance.allianceId } + alliance
            current.copy(alliances = updated)
        }
    }

    @Synchronized
    fun addGold(amount: Int) {
        if (amount <= 0) return
        _economyState.update { current ->
            current.copy(gold = current.gold + amount)
        }
        saveToPersistence()
    }

    fun setPlayerAllianceIdForTesting(id: String?) {
        _economyState.update { current ->
            current.copy(
                playerAllianceId = id,
                playerProgress = current.playerProgress.copy(allianceId = id)
            )
        }
    }

    fun setLoginStreakForTesting(streak: Int, lastDate: String? = null) {
        _economyState.update { current ->
            current.copy(
                lastLoginRewardDate = lastDate,
                playerProgress = current.playerProgress.copy(loginStreak = streak)
            )
        }
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
        updateWeeklyObjectiveProgress(WeeklyObjectiveType.DEAL_DAMAGE, damage.toLong())
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
            updateWeeklyObjectiveProgress(WeeklyObjectiveType.DEAL_DAMAGE, stats.totalDamageDealt.toLong())
        }
        if (isVictory) {
            updateQuestProgress(QuestType.WIN_BATTLE, 1)
            updateWeeklyObjectiveProgress(WeeklyObjectiveType.WIN_BATTLES, 1L)
            if (isCampaign) {
                updateQuestProgress(QuestType.WIN_CAMPAIGN_BATTLE, 1)
            }
        }

        // Accrue Alliance Contribution and XP (Phase 8 Section 3, 4)
        addAllianceContribution(
            battlesWon = if (isVictory) 1 else 0,
            damageDealt = stats.totalDamageDealt.toLong(),
            campaignClears = if (isVictory && isCampaign) 1 else 0
        )

        saveToPersistence()
    }

    fun setEventTokensForTesting(tokens: Int) {
        _economyState.update { it.copy(eventTokens = tokens) }
    }

    fun setRaidDailyAttemptsForTesting(attempts: Int) {
        _economyState.update { it.copy(raidDailyAttempts = attempts) }
    }

    fun setWeeklyObjectiveProgressForTesting(id: String, progress: Long) {
        _economyState.update { current ->
            val updated = current.weeklyObjectives.map {
                if (it.id == id) it.copy(progress = progress) else it
            }
            current.copy(weeklyObjectives = updated)
        }
    }

    fun setWorldBossHpForTesting(hp: Long) {
        _economyState.update { it.copy(worldBossCurrentHp = hp) }
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

    // =========================================================================
    // PHASE 11: PVP ARENA & COMPETITIVE SYSTEM
    // =========================================================================

    /**
     * Consumes one arena attempt when a PvP match officially starts (Requirement #7).
     * Returns true if attempt was available and consumed, false otherwise.
     */
    @Synchronized
    fun consumeArenaAttempt(): Boolean {
        checkDailyReset()
        val current = _economyState.value
        if (current.arenaDailyAttempts <= 0) return false
        _economyState.update {
            it.copy(
                arenaDailyAttempts = it.arenaDailyAttempts - 1,
                activeDeck = it.activeDeck.createDefensiveCopy()
            )
        }
        saveToPersistence()
        return true
    }

    /**
     * Records completion of an Arena PvP match (Requirement #2, #4, #8, #12, #13, #14, #18).
     * Integrates ELO rating change, win streaks, first win of day, rewards, and progression.
     */
    @Synchronized
    fun recordArenaBattleFinished(
        opponent: ArenaOpponent,
        isVictory: Boolean,
        stats: BattleStats
    ): ArenaBattleResultSummary {
        checkDailyReset()
        val current = _economyState.value
        val matchResult = if (isVictory) ArenaMatchResult.VICTORY else ArenaMatchResult.DEFEAT
        val ratingDelta = ArenaRatingCalculator.calculateRatingDelta(current.arenaRating, opponent.rating, matchResult)
        val newRating = ArenaRatingCalculator.applyRatingChange(current.arenaRating, ratingDelta)
        val newPeakRating = maxOf(current.arenaPeakRating, newRating)
        val oldTier = current.arenaTier
        val newTier = ArenaRankTier.fromRating(newRating)
        val isTierUpgraded = newTier.ordinal > oldTier.ordinal

        val newStreak = if (isVictory) current.arenaCurrentStreak + 1 else 0
        val newHighestStreak = maxOf(current.arenaHighestStreak, newStreak)

        val currentDate = MythosDateUtil.getCurrentLocalDate()
        val isFirstWin = isVictory && (current.arenaFirstWinClaimedDate != currentDate)

        val (baseGold, baseShards, basePoints, xp) = when {
            isVictory && isFirstWin -> listOf(
                ArenaCatalog.Rewards.FIRST_WIN_GOLD,
                ArenaCatalog.Rewards.FIRST_WIN_SHARDS,
                ArenaCatalog.Rewards.FIRST_WIN_ARENA_POINTS,
                ArenaCatalog.Rewards.FIRST_WIN_XP
            )
            isVictory -> listOf(
                ArenaCatalog.Rewards.NORMAL_WIN_GOLD,
                ArenaCatalog.Rewards.NORMAL_WIN_SHARDS,
                ArenaCatalog.Rewards.NORMAL_WIN_ARENA_POINTS,
                ArenaCatalog.Rewards.NORMAL_WIN_XP
            )
            else -> listOf(
                ArenaCatalog.Rewards.DEFEAT_GOLD,
                ArenaCatalog.Rewards.DEFEAT_SHARDS,
                ArenaCatalog.Rewards.DEFEAT_ARENA_POINTS,
                ArenaCatalog.Rewards.DEFEAT_XP
            )
        }

        val streakBonus = if (isVictory) ArenaCatalog.Rewards.getStreakBonus(newStreak) else Pair(0, 0)
        val totalGold = baseGold + streakBonus.first
        val totalPoints = basePoints + streakBonus.second

        val matchId = "pvp_match_${System.currentTimeMillis()}"
        val matchRecord = ArenaMatchRecord(
            matchId = matchId,
            timestamp = System.currentTimeMillis(),
            dateFormatted = currentDate,
            opponentName = opponent.name,
            opponentHeroName = opponent.heroName,
            playerHeroName = HeroCatalog.findHero(current.selectedHeroId)?.name ?: "Hercules",
            opponentArchetype = opponent.archetype.title,
            result = matchResult,
            ratingBefore = current.arenaRating,
            ratingAfter = newRating,
            ratingChange = ratingDelta,
            turns = stats.turnsCount,
            durationSeconds = maxOf(15, stats.turnsCount * 8),
            goldAwarded = totalGold,
            xpAwarded = xp,
            shardsAwarded = baseShards,
            arenaPointsAwarded = totalPoints
        )

        val updatedHistory = (listOf(matchRecord) + current.arenaMatchHistory).take(20)

        _economyState.update { curr ->
            val newShards = curr.cardShards.toMutableMap()
            val targetCardId = "c_olympian_guard"
            newShards[targetCardId] = (newShards[targetCardId] ?: 0) + baseShards

            curr.copy(
                gold = curr.gold + totalGold,
                cardShards = newShards,
                arenaPoints = curr.arenaPoints + totalPoints,
                arenaRating = newRating,
                arenaPeakRating = newPeakRating,
                arenaWins = if (isVictory) curr.arenaWins + 1 else curr.arenaWins,
                arenaLosses = if (!isVictory) curr.arenaLosses + 1 else curr.arenaLosses,
                arenaCurrentStreak = newStreak,
                arenaHighestStreak = newHighestStreak,
                arenaFirstWinClaimedDate = if (isFirstWin) currentDate else curr.arenaFirstWinClaimedDate,
                arenaMatchHistory = updatedHistory,
                activeDeck = curr.activeDeck.createDefensiveCopy()
            )
        }

        // Consistently update Player XP, lifetime battles, damage, daily quests, weekly objectives, alliance contribution (Requirement #18)
        addPlayerXp(xp)
        recordBattleFinished(isVictory = isVictory, stats = stats, isCampaign = false)

        saveToPersistence()

        return ArenaBattleResultSummary(
            matchId = matchId,
            result = matchResult,
            opponentName = opponent.name,
            ratingBefore = current.arenaRating,
            ratingAfter = newRating,
            ratingChange = ratingDelta,
            newTier = newTier,
            isTierUpgraded = isTierUpgraded,
            goldAwarded = totalGold,
            xpAwarded = xp,
            cardShardsAwarded = baseShards,
            arenaPointsAwarded = totalPoints,
            isFirstWinOfDay = isFirstWin,
            currentStreak = newStreak,
            streakBonusGold = streakBonus.first,
            streakBonusPoints = streakBonus.second
        )
    }

    /**
     * Checks if current season has expired and performs competitive season reset (Requirement #9).
     */
    @Synchronized
    fun checkSeasonReset(currentTimestampMs: Long = System.currentTimeMillis()) {
        val current = _economyState.value
        val season = current.arenaCurrentSeason
        if (season.endDateMs != Long.MAX_VALUE && currentTimestampMs >= season.endDateMs) {
            val newRating = ArenaRatingCalculator.calculateSeasonResetRating(current.arenaRating)
            val nextSeasonNumber = season.seasonNumber + 1
            val nextSeason = ArenaSeason(
                seasonId = "season_$nextSeasonNumber",
                seasonNumber = nextSeasonNumber,
                startDateMs = currentTimestampMs,
                endDateMs = currentTimestampMs + ArenaCatalog.SEASON_DURATION_MS,
                rating = newRating,
                peakRating = newRating,
                wins = 0,
                losses = 0,
                matches = 0,
                isRewardsClaimed = false
            )
            _economyState.update {
                it.copy(
                    arenaRating = newRating,
                    arenaCurrentSeason = nextSeason,
                    activeDeck = it.activeDeck.createDefensiveCopy()
                )
            }
            saveToPersistence()
        }
    }

    /**
     * Claims end-of-season rewards (Requirement #10).
     * Enforces strict idempotency — cannot be claimed twice for the same season.
     */
    @Synchronized
    fun claimSeasonReward(seasonId: String): ArenaSeasonReward? {
        val current = _economyState.value
        if (current.arenaSeasonRewardsClaimed.contains(seasonId)) {
            return null // Idempotent: already claimed
        }
        val reward = ArenaCatalog.SEASON_REWARDS[current.arenaTier] ?: return null
        _economyState.update { curr ->
            val newShards = curr.cardShards.toMutableMap()
            newShards["c_olympian_guard"] = (newShards["c_olympian_guard"] ?: 0) + reward.cardShards

            val newHeroShards = curr.heroShards.toMutableMap()
            if (reward.heroShards > 0) {
                newHeroShards[HerculesIdentity.HERO_ID] = (newHeroShards[HerculesIdentity.HERO_ID] ?: 0) + reward.heroShards
            }

            val newFrames = curr.ownedFrames.toMutableSet()
            reward.exclusiveFrameId?.let { newFrames.add(it) }

            val newCosmetics = curr.ownedCosmeticIds.toMutableSet()
            reward.exclusiveCosmeticId?.let { newCosmetics.add(it) }

            curr.copy(
                gold = curr.gold + reward.gold,
                cardShards = newShards,
                heroShards = newHeroShards,
                ownedFrames = newFrames,
                ownedCosmeticIds = newCosmetics,
                arenaSeasonRewardsClaimed = curr.arenaSeasonRewardsClaimed + seasonId,
                activeDeck = curr.activeDeck.createDefensiveCopy()
            )
        }
        saveToPersistence()
        return reward
    }

    // Testing Helpers
    fun setArenaRatingForTesting(rating: Int) {
        _economyState.update {
            it.copy(
                arenaRating = rating,
                arenaPeakRating = maxOf(it.arenaPeakRating, rating)
            )
        }
    }

    fun setArenaAttemptsForTesting(attempts: Int) {
        _economyState.update { it.copy(arenaDailyAttempts = attempts) }
    }

    fun setArenaSeasonForTesting(season: ArenaSeason) {
        _economyState.update { it.copy(arenaCurrentSeason = season) }
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
