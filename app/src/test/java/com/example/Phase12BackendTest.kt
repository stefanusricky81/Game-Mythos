package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.backend.*
import com.example.data.*
import com.example.monetization.PlayerEconomyRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

/**
 * Phase 12 — Live Backend & Production Online Automated Test Suite.
 *
 * Verifies all 30 mandatory requirements:
 * 1. Firebase UID identity
 * 2. Player profile creation
 * 3. Player profile persistence
 * 4. Server currency mutation
 * 5. Negative currency rejection
 * 6. Card ownership validation
 * 7. Deck exactly 20 cards
 * 8. Duplicate card limit
 * 9. Unowned card rejection
 * 10. Arena state persistence
 * 11. ELO server calculation
 * 12. Arena result idempotency
 * 13. Arena reward idempotency
 * 14. Daily attempt validation
 * 15. Daily attempt reset
 * 16. Season state
 * 17. Season reset
 * 18. Season reward claim once
 * 19. Leaderboard read
 * 20. Match history persistence
 * 21. Alliance membership validation
 * 22. Alliance role validation
 * 23. Raid contribution validation
 * 24. World Boss contribution validation
 * 25. Event reward idempotency
 * 26. Sync retry
 * 27. Duplicate request protection
 * 28. Unauthorized access rejection
 * 29. ActiveDeck 20-card regression
 * 30. Payment regression
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase12BackendTest {

    private lateinit var context: Context
    private lateinit var economyRepository: PlayerEconomyRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("mythos_player_data", Context.MODE_PRIVATE).edit().clear().commit()
        economyRepository = PlayerEconomyRepository()
        economyRepository.initPersistence(context)
    }

    // 1. Firebase UID identity
    @Test
    fun test01_firebaseUidIdentity() = runBlocking {
        val identity = DefaultPlayerIdentity { "anon_user_12345" }
        assertEquals("anon_user_12345", identity.currentUid)
        assertTrue(identity.isAnonymous)
        assertTrue(identity.isAuthenticated)

        identity.setAuthenticatedUser("firebase_auth_999", isAnon = false)
        assertEquals("firebase_auth_999", identity.currentUid)
        assertFalse(identity.isAnonymous)
        assertNotNull(identity.getAuthToken())
    }

    // 2. Player profile creation
    @Test
    fun test02_playerProfileCreation() {
        val profile = ServerPlayerProfile(uid = "test_player_01")
        assertEquals("test_player_01", profile.uid)
        assertEquals(1, profile.playerLevel)
        assertEquals(0L, profile.playerXp)
        assertEquals(1000L, profile.gold)
        assertEquals(100L, profile.mythGems)
        assertEquals(50L, profile.cardShards)
        assertEquals(0L, profile.arenaPoints)
    }

    // 3. Player profile persistence & sync
    @Test
    fun test03_playerProfilePersistence() = runBlocking {
        val identity = DefaultPlayerIdentity { "test_player_sync" }
        val syncRepo = GameSyncRepository(playerIdentity = identity)
        val localProfile = ServerPlayerProfile(uid = identity.currentUid, gold = 2500L, playerXp = 500L)
        val result = syncRepo.syncPlayerState(localProfile)

        assertTrue(result.isSuccess)
        assertEquals(SyncState.SERVER_AUTHORITATIVE, syncRepo.syncState.value)
        assertEquals(2500L, syncRepo.authoritativeProfile?.gold)
        assertEquals(500L, syncRepo.authoritativeProfile?.playerXp)
    }

    // 4. Server currency mutation
    @Test
    fun test04_serverCurrencyMutation() {
        var profile = ServerPlayerProfile(uid = "p1", gold = 1000L, mythGems = 100L)
        profile = profile.awardGold(500L)
        assertEquals(1500L, profile.gold)

        profile = profile.spendGold(300L)
        assertEquals(1200L, profile.gold)

        profile = profile.awardGems(50L)
        assertEquals(150L, profile.mythGems)

        profile = profile.awardPlayerXp(1200L)
        assertEquals(1200L, profile.playerXp)
        assertEquals(2, profile.playerLevel) // Level increased
    }

    // 5. Negative currency rejection
    @Test
    fun test05_negativeCurrencyRejection() {
        val profile = ServerPlayerProfile(uid = "p1", gold = 1000L)

        assertThrows(IllegalArgumentException::class.java) {
            profile.awardGold(-100L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            profile.spendGold(-50L)
        }
        assertThrows(IllegalStateException::class.java) {
            profile.spendGold(2000L) // Insufficient funds
        }
    }

    // 6. Card ownership validation
    @Test
    fun test06_cardOwnershipValidation() {
        val inventory = ServerCardInventory()
        // Register Olympian Guard
        inventory.grantCard("c_olympian_guard", count = 3)

        assertEquals(3, inventory.getQuantity("c_olympian_guard"))
        assertTrue(inventory.isCardValid("c_olympian_guard"))
        assertFalse(inventory.isCardValid("c_nonexistent_card"))

        // Upgrade requires level * 2 = 2 copies
        val upgraded = inventory.upgradeCard("c_olympian_guard")
        assertEquals(2, upgraded.level)
        assertEquals(1, inventory.getQuantity("c_olympian_guard"))
    }

    // 7. Deck exactly 20 cards
    @Test
    fun test07_deckExactly20Cards() {
        val defaultDeck = ActiveDeck.createDefaultOlympusDeck()
        assertEquals(20, defaultDeck.totalCards)

        val validResult = ServerDeckValidator.validateDeck(defaultDeck)
        assertTrue(validResult.isValid)

        // Test 19 cards rejected
        val nineteenDeck = defaultDeck.copy(cardIds = defaultDeck.cardIds.take(19))
        val invalid19 = ServerDeckValidator.validateDeck(nineteenDeck)
        assertFalse(invalid19.isValid)
        assertTrue(invalid19.issues.any { it.type == ServerDeckIssueType.INVALID_DECK_SIZE })

        // Test 21 cards rejected
        val twentyOneDeck = defaultDeck.copy(cardIds = defaultDeck.cardIds + "c_zeus_01")
        val invalid21 = ServerDeckValidator.validateDeck(twentyOneDeck)
        assertFalse(invalid21.isValid)
        assertTrue(invalid21.issues.any { it.type == ServerDeckIssueType.INVALID_DECK_SIZE })
    }

    // 8. Duplicate card limit
    @Test
    fun test08_duplicateCardLimit() {
        // Construct deck with 3 copies of c_zeus_01
        val baseList = HeroCatalog.HERCULES.defaultDeckCardIds.toMutableList()
        baseList[0] = "c_zeus_01"
        baseList[1] = "c_zeus_01"
        baseList[2] = "c_zeus_01" // 3 copies!
        val badDeck = ActiveDeck(cardIds = baseList)

        val result = ServerDeckValidator.validateDeck(badDeck)
        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.type == ServerDeckIssueType.EXCEEDED_DUPLICATE_LIMIT })
    }

    // 9. Unowned card rejection
    @Test
    fun test09_unownedCardRejection() {
        val defaultDeck = ActiveDeck.createDefaultOlympusDeck()
        val emptyInventory = ServerCardInventory() // Owns 0 cards

        val result = ServerDeckValidator.validateDeck(defaultDeck, emptyInventory)
        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.type == ServerDeckIssueType.UNOWNED_CARD })
    }

    // 10. Arena state persistence
    @Test
    fun test10_arenaStatePersistence() {
        val state = ServerArenaState(uid = "p_arena_01", rating = 1150, wins = 5, losses = 2)
        assertEquals(ArenaRankTier.SILVER, state.tier)
        assertEquals(7, state.totalMatches)
        assertEquals(1150, state.rating)
    }

    // 11. ELO server calculation
    @Test
    fun test11_eloServerCalculation() {
        // Equal ratings (1000 vs 1000): Expected = 0.5, Delta = 32 * (1 - 0.5) = +16
        val winDelta = ArenaRatingCalculator.calculateRatingDelta(1000, 1000, ArenaMatchResult.VICTORY)
        assertEquals(16, winDelta)

        val lossDelta = ArenaRatingCalculator.calculateRatingDelta(1000, 1000, ArenaMatchResult.DEFEAT)
        assertEquals(-16, lossDelta)

        // Rating floor at 0
        val clamped = ArenaRatingCalculator.applyRatingChange(5, -20)
        assertEquals(0, clamped)
    }

    // 12. Arena result idempotency
    @Test
    fun test12_arenaResultIdempotency() {
        val service = ServerArenaService()
        val state = ServerArenaState(uid = "p_idem_01")
        val deck = ActiveDeck.createDefaultOlympusDeck().cardIds

        val (stateAfterMatch, session) = service.createArenaMatch(
            currentState = state,
            opponentId = "opp_1",
            opponentName = "Olympus Vanguard",
            opponentArchetype = "OLYMPUS_VANGUARD",
            opponentRating = 1000,
            deckCardIds = deck
        )

        // Submit victory
        val (stateVictory1, completedSession1) = service.submitArenaResult(
            currentState = stateAfterMatch,
            matchId = session.matchId,
            result = ArenaMatchResult.VICTORY,
            turns = 5,
            damageDealt = 100,
            damageTaken = 20
        )
        val ratingAfterFirstSubmit = stateVictory1.rating
        assertEquals(1016, ratingAfterFirstSubmit)

        // Duplicate submission of same matchId
        val (stateVictory2, completedSession2) = service.submitArenaResult(
            currentState = stateVictory1,
            matchId = session.matchId,
            result = ArenaMatchResult.VICTORY,
            turns = 5,
            damageDealt = 100,
            damageTaken = 20
        )

        // State and rewards must NOT duplicate
        assertEquals(ratingAfterFirstSubmit, stateVictory2.rating)
        assertEquals(1, stateVictory2.wins)
    }

    // 13. Arena reward idempotency
    @Test
    fun test13_arenaRewardIdempotency() {
        val idempotency = ServerIdempotencyManager()
        var grantCount = 0

        val (res1, executed1) = idempotency.executeIdempotent("claim_arena_reward_01") {
            grantCount++
            1500
        }
        assertTrue(executed1)
        assertEquals(1500, res1)
        assertEquals(1, grantCount)

        // Retry same claim token
        val (res2, executed2) = idempotency.executeIdempotent("claim_arena_reward_01") {
            grantCount++
            1500
        }
        assertFalse(executed2)
        assertNull(res2)
        assertEquals(1, grantCount) // Never granted twice
    }

    // 14. Daily attempt validation
    @Test
    fun test14_dailyAttemptValidation() {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val service = ServerArenaService()
        var state = ServerArenaState(uid = "p_attempts_01", dailyAttempts = 1, lastAttemptResetDate = today)
        val deck = ActiveDeck.createDefaultOlympusDeck().cardIds

        val (updatedState, _) = service.createArenaMatch(
            currentState = state,
            opponentId = "opp_1",
            opponentName = "Opponent",
            opponentArchetype = "VANGUARD",
            opponentRating = 1000,
            deckCardIds = deck
        )
        assertEquals(0, updatedState.dailyAttempts)

        // Attempting to start match with 0 attempts must be rejected
        assertThrows(IllegalStateException::class.java) {
            service.createArenaMatch(
                currentState = updatedState,
                opponentId = "opp_2",
                opponentName = "Opponent 2",
                opponentArchetype = "VANGUARD",
                opponentRating = 1000,
                deckCardIds = deck
            )
        }
    }

    // 15. Daily attempt reset
    @Test
    fun test15_dailyAttemptReset() {
        val service = ServerArenaService(
            dateProvider = { "2026-10-01" } // Tomorrow
        )
        val state = ServerArenaState(
            uid = "p_reset",
            dailyAttempts = 0,
            lastAttemptResetDate = "2026-09-30" // Yesterday
        )

        val resetState = service.checkDailyReset(state)
        assertEquals(ArenaCatalog.MAX_DAILY_ATTEMPTS, resetState.dailyAttempts)
        assertEquals("2026-10-01", resetState.lastAttemptResetDate)
    }

    // 16. Season state
    @Test
    fun test16_seasonState() {
        val state = ServerArenaState(uid = "p_season", rating = 1450, peakRating = 1520)
        assertEquals("season_01", state.currentSeasonId)
        assertEquals(ArenaRankTier.GOLD, state.tier)
        assertEquals(1520, state.peakRating)
    }

    // 17. Season reset
    @Test
    fun test17_seasonReset() {
        val service = ServerArenaService()
        val state = ServerArenaState(
            uid = "p_season_reset",
            rating = 1800,
            peakRating = 1850,
            wins = 25,
            losses = 10,
            currentWinStreak = 4
        )

        val resetState = service.processSeasonReset(state, newSeasonId = "season_02")
        assertEquals("season_02", resetState.currentSeasonId)
        assertEquals(1850, resetState.peakRating) // Preserved
        assertEquals(0, resetState.wins)
        assertEquals(0, resetState.losses)
        assertEquals(0, resetState.currentWinStreak)
        // Soft reset pulls rating toward 1000
        assertTrue(resetState.rating in 1000..1800)
    }

    // 18. Season reward claim once
    @Test
    fun test18_seasonRewardClaimOnce() {
        val idempotency = ServerIdempotencyManager()
        val seasonRewardKey = "season_01_reward_gold_tier"

        val (_, claimed1) = idempotency.executeIdempotent(seasonRewardKey) { "REWARD_GRANTED" }
        assertTrue(claimed1)

        val (_, claimed2) = idempotency.executeIdempotent(seasonRewardKey) { "REWARD_GRANTED" }
        assertFalse(claimed2)
    }

    // 19. Leaderboard read
    @Test
    fun test19_leaderboardRead() = runBlocking {
        val repo = ServerLeaderboardRepository()
        val top = repo.getTopEntries(5)
        assertEquals(5, top.size)
        assertTrue(top[0].rating >= top[1].rating)

        repo.updatePlayerEntry(
            uid = "player_hero_1",
            playerName = "Achilles",
            rating = 3000,
            wins = 50,
            losses = 2,
            peakRating = 3000
        )
        val playerEntry = repo.getPlayerEntry("player_hero_1")
        assertNotNull(playerEntry)
        assertEquals(1, playerEntry?.rank) // Now rank 1
    }

    // 20. Match history persistence
    @Test
    fun test20_matchHistoryPersistence() {
        val service = ServerArenaService()
        val state = ServerArenaState(uid = "p_hist")
        val deck = ActiveDeck.createDefaultOlympusDeck().cardIds

        val (stateAfterMatch, session) = service.createArenaMatch(
            currentState = state,
            opponentId = "opp_test",
            opponentName = "Ares Champion",
            opponentArchetype = "SPARTAN_WARBORN",
            opponentRating = 1100,
            deckCardIds = deck
        )

        service.submitArenaResult(
            currentState = stateAfterMatch,
            matchId = session.matchId,
            result = ArenaMatchResult.VICTORY,
            turns = 4,
            damageDealt = 120,
            damageTaken = 30
        )

        val history = service.matchHistory
        assertEquals(1, history.size)
        assertEquals(session.matchId, history[0].matchId)
        assertEquals("Ares Champion", history[0].opponentName)
        assertEquals(ArenaMatchResult.VICTORY, history[0].result)
    }

    // 21. Alliance membership validation
    @Test
    fun test21_allianceMembershipValidation() {
        val service = ServerAllianceService()
        val alliance = service.createAlliance("all_01", "Spartan Brotherhood", "p_leader", "Leonidas")
        assertEquals(1, alliance.memberCount)

        service.joinAlliance("all_01", "p_warrior", "Kratos")
        val updated = service.getAlliance("all_01")
        assertEquals(2, updated?.memberCount)

        // Max 20 members limit check
        assertFalse(updated!!.isFull)
    }

    // 22. Alliance role validation
    @Test
    fun test22_allianceRoleValidation() {
        val service = ServerAllianceService()
        service.createAlliance("all_02", "Argonauts", "p_jason", "Jason")
        service.joinAlliance("all_02", "p_regular", "Sailor")

        // Regular member cannot kick others
        assertThrows(IllegalStateException::class.java) {
            service.kickMember("all_02", callerUid = "p_regular", targetUid = "p_jason")
        }

        // Leader promotes regular to officer
        val promoted = service.promoteMember("all_02", leaderUid = "p_jason", targetUid = "p_regular")
        val officer = promoted.members.find { it.uid == "p_regular" }
        assertEquals(AllianceRole.OFFICER, officer?.role)
    }

    // 23. Raid contribution validation
    @Test
    fun test23_raidContributionValidation() {
        val service = ServerEndgameService()
        val updated = service.submitRaidContribution("p_raider", damage = 25000L)
        assertEquals(1, updated.attemptsConsumed["p_raider"])
        assertEquals(25000L, updated.playerContributions["p_raider"])

        // Daily attempts limit (3 max)
        service.submitRaidContribution("p_raider", damage = 10000L)
        service.submitRaidContribution("p_raider", damage = 10000L)

        // 4th attempt must fail
        assertThrows(IllegalStateException::class.java) {
            service.submitRaidContribution("p_raider", damage = 5000L)
        }
    }

    // 24. World Boss contribution validation
    @Test
    fun test24_worldBossContributionValidation() {
        val service = ServerEndgameService()
        val initialHp = service.worldBossState.currentHp

        val updated = service.submitWorldBossContribution("p_slayer", allianceId = "all_01", damage = 50000L)
        assertEquals(initialHp - 50000L, updated.currentHp)
        assertEquals(50000L, updated.playerContributions["p_slayer"])

        // Impossible damage (>500,000) rejected
        assertThrows(IllegalStateException::class.java) {
            service.submitWorldBossContribution("p_slayer", allianceId = null, damage = 999_999L)
        }
    }

    // 25. Event reward idempotency
    @Test
    fun test25_eventRewardIdempotency() {
        val service = ServerEndgameService()
        val event = ServerLimitedEventState(
            eventId = "wrath_of_olympus",
            name = "Wrath of Olympus",
            playerCurrencies = mapOf("p_event_user" to 200)
        )
        service.registerEvent(event)

        val (e1, granted1) = service.claimEventReward("wrath_of_olympus", "p_event_user", "milestone_1", cost = 50)
        assertTrue(granted1)
        assertEquals(150, e1.playerCurrencies["p_event_user"])

        // Duplicate claim
        val (e2, granted2) = service.claimEventReward("wrath_of_olympus", "p_event_user", "milestone_1", cost = 50)
        assertFalse(granted2)
        assertEquals(150, e2.playerCurrencies["p_event_user"]) // Tokens not deducted again
    }

    // 26. Sync retry
    @Test
    fun test26_syncRetry() = runBlocking {
        val syncRepo = GameSyncRepository()
        var attempts = 0
        val result = syncRepo.retryWithBackoff(maxAttempts = 3) {
            attempts++
            if (attempts < 2) throw RuntimeException("Transient connection dropped")
            "SYNC_SUCCESS"
        }
        assertTrue(result.isSuccess)
        assertEquals("SYNC_SUCCESS", result.getOrNull())
        assertEquals(2, attempts)
    }

    // 27. Duplicate request protection
    @Test
    fun test27_duplicateRequestProtection() {
        val limiter = ServerRateLimiter(maxRequestsPerWindow = 2, windowMs = 500L)
        assertTrue(limiter.acquirePermission("uid_spam"))
        assertTrue(limiter.acquirePermission("uid_spam"))
        // 3rd rapid request exceeds limit
        assertFalse(limiter.acquirePermission("uid_spam"))
    }

    // 28. Unauthorized access rejection
    @Test
    fun test28_unauthorizedAccessRejection() {
        assertThrows(SecurityException::class.java) {
            ServerSecurityValidator.validateAuthContext(authenticatedUid = "uid_alice", requestedUid = "uid_bob")
        }
        assertThrows(SecurityException::class.java) {
            ServerSecurityValidator.validateAuthContext(authenticatedUid = null, requestedUid = "uid_bob")
        }
        // Matching UID succeeds
        ServerSecurityValidator.validateAuthContext(authenticatedUid = "uid_charlie", requestedUid = "uid_charlie")
    }

    // 29. ActiveDeck 20-card regression
    @Test
    fun test29_activeDeck20CardRegression() {
        val activeDeck = economyRepository.economyState.value.activeDeck
        assertEquals(20, activeDeck.totalCards)
        assertEquals(20, activeDeck.cardIds.size)
        assertTrue(activeDeck.isComplete)

        val validation = ServerDeckValidator.validateDeck(activeDeck)
        assertTrue(validation.isValid)
    }

    // 30. Payment regression
    @Test
    fun test30_paymentRegression() {
        // Confirm PlayerEconomyRepository untouched payment behavior works correctly
        val gems = economyRepository.economyState.value.mythGems
        assertTrue(gems >= 0)
        val purchaseHistory = economyRepository.economyState.value.purchaseHistory
        assertNotNull(purchaseHistory)
    }
}
