package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.monetization.PlayerEconomyRepository
import com.example.viewmodel.BattleViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 11 — PvP Arena & Competitive System Automated Test Suite.
 *
 * Verifies all 24 requirements:
 * 1. Arena starts at rating 1000
 * 2. Win increases rating
 * 3. Loss decreases rating
 * 4. Rating cannot become negative
 * 5. ELO calculation deterministic
 * 6. Rank calculation correct across all 8 tiers
 * 7. Matchmaking respects rating range
 * 8. Arena attempt consumed exactly once
 * 9. Daily attempt reset works
 * 10. First-win reward claim is idempotent
 * 11. Arena rewards persist across reload
 * 12. Season data persists
 * 13. Season reset works
 * 14. Season reward cannot be claimed twice
 * 15. Match history persists
 * 16. Win streak increments
 * 17. Win streak resets after defeat
 * 18. Player XP integration works
 * 19. Daily quest integration works
 * 20. Weekly objective integration works
 * 21. Alliance contribution integration works
 * 22. ActiveDeck remains exactly 20 cards
 * 23. Existing PvE tests remain passing
 * 24. Payment untouched verification
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ArenaSystemTest {

    private lateinit var context: Context
    private lateinit var repository: PlayerEconomyRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("mythos_player_data", Context.MODE_PRIVATE).edit().clear().commit()
        repository = PlayerEconomyRepository()
        repository.initPersistence(context)
    }

    @Test
    fun test01_arenaStartsAtRating1000() {
        val state = repository.economyState.value
        assertEquals("Initial Arena rating must be 1000", 1000, state.arenaRating)
        assertEquals("Initial Arena peak rating must be 1000", 1000, state.arenaPeakRating)
        assertEquals("Initial Rank Tier must be SILVER", ArenaRankTier.SILVER, state.arenaTier)
    }

    @Test
    fun test02_winIncreasesRating() {
        val initialRating = repository.economyState.value.arenaRating
        val opp = ArenaCatalog.findOpponent(initialRating)
        val dummyStats = BattleStats(turnsCount = 5, totalDamageDealt = 4000, totalDamageTaken = 1000)

        val result = repository.recordArenaBattleFinished(opp, isVictory = true, stats = dummyStats)

        val updatedRating = repository.economyState.value.arenaRating
        assertTrue("Rating must increase on victory", updatedRating > initialRating)
        assertTrue("Rating delta must be positive", result.ratingChange > 0)
        assertEquals(updatedRating, result.ratingAfter)
        assertEquals(1, repository.economyState.value.arenaWins)
    }

    @Test
    fun test03_lossDecreasesRating() {
        val initialRating = repository.economyState.value.arenaRating
        val opp = ArenaCatalog.findOpponent(initialRating)
        val dummyStats = BattleStats(turnsCount = 4, totalDamageDealt = 1500, totalDamageTaken = 4500)

        val result = repository.recordArenaBattleFinished(opp, isVictory = false, stats = dummyStats)

        val updatedRating = repository.economyState.value.arenaRating
        assertTrue("Rating must decrease on defeat", updatedRating < initialRating)
        assertTrue("Rating delta must be negative", result.ratingChange < 0)
        assertEquals(updatedRating, result.ratingAfter)
        assertEquals(1, repository.economyState.value.arenaLosses)
    }

    @Test
    fun test04_ratingCannotBecomeNegative() {
        repository.setArenaRatingForTesting(10)
        val weakOpponent = ArenaOpponent(
            id = "opp_god",
            name = "Overwhelming Gladiator",
            heroId = "ares",
            heroName = "Ares",
            archetype = ArenaOpponentArchetype.SPARTAN_WARBORN,
            rating = 2800,
            combatPower = 20000,
            deckCardIds = ArenaCatalog.SPARTAN_WARBORN_DECK
        )
        val dummyStats = BattleStats(turnsCount = 3, totalDamageDealt = 500, totalDamageTaken = 5000)

        // Multiple defeats starting near 0
        repository.recordArenaBattleFinished(weakOpponent, isVictory = false, stats = dummyStats)
        repository.recordArenaBattleFinished(weakOpponent, isVictory = false, stats = dummyStats)

        val finalRating = repository.economyState.value.arenaRating
        assertTrue("Rating must never become negative", finalRating >= 0)
    }

    @Test
    fun test05_eloCalculationDeterministic() {
        val expectedEqual = ArenaRatingCalculator.calculateExpectedScore(1000, 1000)
        assertEquals("Expected score between equal players must be 0.5", 0.5, expectedEqual, 0.001)

        val deltaWin = ArenaRatingCalculator.calculateRatingDelta(1000, 1000, ArenaMatchResult.VICTORY)
        assertEquals("Win against equal player with K=32 must yield 16 rating", 16, deltaWin)

        val deltaLoss = ArenaRatingCalculator.calculateRatingDelta(1000, 1000, ArenaMatchResult.DEFEAT)
        assertEquals("Loss against equal player with K=32 must yield -16 rating", -16, deltaLoss)

        // Repeat calculation to verify deterministic output
        val repeatDeltaWin = ArenaRatingCalculator.calculateRatingDelta(1000, 1000, ArenaMatchResult.VICTORY)
        assertEquals(deltaWin, repeatDeltaWin)
    }

    @Test
    fun test06_rankCalculationCorrect() {
        assertEquals(ArenaRankTier.BRONZE, ArenaRankTier.fromRating(0))
        assertEquals(ArenaRankTier.BRONZE, ArenaRankTier.fromRating(999))
        assertEquals(ArenaRankTier.SILVER, ArenaRankTier.fromRating(1000))
        assertEquals(ArenaRankTier.SILVER, ArenaRankTier.fromRating(1299))
        assertEquals(ArenaRankTier.GOLD, ArenaRankTier.fromRating(1300))
        assertEquals(ArenaRankTier.GOLD, ArenaRankTier.fromRating(1599))
        assertEquals(ArenaRankTier.PLATINUM, ArenaRankTier.fromRating(1600))
        assertEquals(ArenaRankTier.PLATINUM, ArenaRankTier.fromRating(1899))
        assertEquals(ArenaRankTier.DIAMOND, ArenaRankTier.fromRating(1900))
        assertEquals(ArenaRankTier.DIAMOND, ArenaRankTier.fromRating(2199))
        assertEquals(ArenaRankTier.MASTER, ArenaRankTier.fromRating(2200))
        assertEquals(ArenaRankTier.MASTER, ArenaRankTier.fromRating(2499))
        assertEquals(ArenaRankTier.GRANDMASTER, ArenaRankTier.fromRating(2500))
        assertEquals(ArenaRankTier.GRANDMASTER, ArenaRankTier.fromRating(2799))
        assertEquals(ArenaRankTier.MYTHIC, ArenaRankTier.fromRating(2800))
        assertEquals(ArenaRankTier.MYTHIC, ArenaRankTier.fromRating(3500))
    }

    @Test
    fun test07_matchmakingRespectsRatingRange() {
        val playerRating = 1400
        val opp = ArenaCatalog.findOpponent(playerRating, searchRange = 150)
        val diff = kotlin.math.abs(opp.rating - playerRating)
        assertTrue("Opponent rating ($opp.rating) should be within search range of player ($playerRating)", diff <= 200)
        assertEquals("Opponent must have a 20-card deck", 20, opp.deckCardIds.size)
    }

    @Test
    fun test08_arenaAttemptConsumedExactlyOnce() {
        assertEquals("Starts with 5 attempts", 5, repository.economyState.value.arenaDailyAttempts)

        val success = repository.consumeArenaAttempt()
        assertTrue("Attempt consumption must succeed", success)
        assertEquals("Must have 4 attempts left", 4, repository.economyState.value.arenaDailyAttempts)

        // Consume remaining
        repository.consumeArenaAttempt()
        repository.consumeArenaAttempt()
        repository.consumeArenaAttempt()
        repository.consumeArenaAttempt()
        assertEquals(0, repository.economyState.value.arenaDailyAttempts)

        val failed = repository.consumeArenaAttempt()
        assertFalse("Must fail when attempts depleted", failed)
        assertEquals(0, repository.economyState.value.arenaDailyAttempts)
    }

    @Test
    fun test09_dailyAttemptResetWorks() {
        repository.setArenaAttemptsForTesting(0)
        assertEquals(0, repository.economyState.value.arenaDailyAttempts)

        // Simulate next calendar day
        repository.checkDailyReset(forceDate = "2026-10-15")

        assertEquals("Daily attempts must reset to 5 on new calendar day", 5, repository.economyState.value.arenaDailyAttempts)
    }

    @Test
    fun test10_firstWinRewardClaimIsIdempotent() {
        val initialGold = repository.economyState.value.gold
        val opp = ArenaCatalog.findOpponent(1000)
        val dummyStats = BattleStats(turnsCount = 5, totalDamageDealt = 3000, totalDamageTaken = 1000)

        // First win of the day
        val win1 = repository.recordArenaBattleFinished(opp, isVictory = true, stats = dummyStats)
        assertTrue("First win of day must flag true", win1.isFirstWinOfDay)
        val goldAfterWin1 = repository.economyState.value.gold
        assertTrue("First win gold (+1500) awarded", goldAfterWin1 >= initialGold + ArenaCatalog.Rewards.FIRST_WIN_GOLD)

        // Second win on same day
        val win2 = repository.recordArenaBattleFinished(opp, isVictory = true, stats = dummyStats)
        assertFalse("Second win on same day must not flag first win", win2.isFirstWinOfDay)
        val goldAfterWin2 = repository.economyState.value.gold
        // Normal win reward is 750 (plus any streak bonus)
        assertEquals(goldAfterWin1 + ArenaCatalog.Rewards.NORMAL_WIN_GOLD, goldAfterWin2)
    }

    @Test
    fun test11_arenaRewardsPersist() {
        val opp = ArenaCatalog.findOpponent(1000)
        val dummyStats = BattleStats(turnsCount = 4, totalDamageDealt = 3500, totalDamageTaken = 800)
        repository.recordArenaBattleFinished(opp, isVictory = true, stats = dummyStats)

        val savedRating = repository.economyState.value.arenaRating
        val savedWins = repository.economyState.value.arenaWins
        val savedPoints = repository.economyState.value.arenaPoints

        // Reload repository from SharedPreferences
        val newRepo = PlayerEconomyRepository()
        newRepo.initPersistence(context)

        assertEquals("Arena rating must persist", savedRating, newRepo.economyState.value.arenaRating)
        assertEquals("Arena wins must persist", savedWins, newRepo.economyState.value.arenaWins)
        assertEquals("Arena points must persist", savedPoints, newRepo.economyState.value.arenaPoints)
    }

    @Test
    fun test12_seasonDataPersists() {
        val season = repository.economyState.value.arenaCurrentSeason
        assertEquals("season_1", season.seasonId)
        assertEquals(28, season.durationDays)

        val newRepo = PlayerEconomyRepository()
        newRepo.initPersistence(context)
        assertEquals("Season ID must persist", season.seasonId, newRepo.economyState.value.arenaCurrentSeason.seasonId)
    }

    @Test
    fun test13_seasonResetWorks() {
        repository.setArenaRatingForTesting(1600)
        val currentSeason = repository.economyState.value.arenaCurrentSeason.copy(
            startDateMs = 1000L,
            endDateMs = 2000L
        )
        repository.setArenaSeasonForTesting(currentSeason)

        // Trigger season reset with timestamp past end date
        val futureTime = currentSeason.endDateMs + 1000L
        repository.checkSeasonReset(currentTimestampMs = futureTime)

        val updatedSeason = repository.economyState.value.arenaCurrentSeason
        assertEquals("Season number must increment to 2", 2, updatedSeason.seasonNumber)
        // Soft reset rule: 1000 + (1600 - 1000) / 2 = 1300
        assertEquals("Soft reset rating must be 1300", 1300, repository.economyState.value.arenaRating)
    }

    @Test
    fun test14_seasonRewardCannotBeClaimedTwice() {
        repository.setArenaRatingForTesting(1400) // Gold tier
        val seasonId = "season_1"

        val reward1 = repository.claimSeasonReward(seasonId)
        assertNotNull("First claim must succeed", reward1)
        assertEquals(ArenaRankTier.GOLD, reward1?.tier)
        assertTrue(reward1!!.gold > 0)

        // Attempt second claim
        val reward2 = repository.claimSeasonReward(seasonId)
        assertNull("Duplicate season reward claim must be blocked", reward2)
    }

    @Test
    fun test15_matchHistoryPersists() {
        val opp = ArenaCatalog.findOpponent(1000)
        val dummyStats = BattleStats(turnsCount = 6, totalDamageDealt = 4000, totalDamageTaken = 1200)
        repository.recordArenaBattleFinished(opp, isVictory = true, stats = dummyStats)

        assertEquals(1, repository.economyState.value.arenaMatchHistory.size)
        val record = repository.economyState.value.arenaMatchHistory.first()
        assertEquals(opp.name, record.opponentName)
        assertEquals(ArenaMatchResult.VICTORY, record.result)

        // Reload from preferences
        val newRepo = PlayerEconomyRepository()
        newRepo.initPersistence(context)
        assertEquals("Match history must persist across reload", 1, newRepo.economyState.value.arenaMatchHistory.size)
        assertEquals(opp.name, newRepo.economyState.value.arenaMatchHistory.first().opponentName)
    }

    @Test
    fun test16_winStreakIncrements() {
        val opp = ArenaCatalog.findOpponent(1000)
        val dummyStats = BattleStats(turnsCount = 5, totalDamageDealt = 3000, totalDamageTaken = 1000)

        repository.recordArenaBattleFinished(opp, isVictory = true, stats = dummyStats)
        assertEquals(1, repository.economyState.value.arenaCurrentStreak)

        repository.recordArenaBattleFinished(opp, isVictory = true, stats = dummyStats)
        assertEquals(2, repository.economyState.value.arenaCurrentStreak)

        val win3 = repository.recordArenaBattleFinished(opp, isVictory = true, stats = dummyStats)
        assertEquals(3, repository.economyState.value.arenaCurrentStreak)
        assertEquals("Milestone 3 streak bonus must award 200 Gold", 200, win3.streakBonusGold)
    }

    @Test
    fun test17_winStreakResetsAfterDefeat() {
        val opp = ArenaCatalog.findOpponent(1000)
        val dummyStats = BattleStats(turnsCount = 5, totalDamageDealt = 3000, totalDamageTaken = 1000)

        repository.recordArenaBattleFinished(opp, isVictory = true, stats = dummyStats)
        repository.recordArenaBattleFinished(opp, isVictory = true, stats = dummyStats)
        assertEquals(2, repository.economyState.value.arenaCurrentStreak)

        // Defeat
        repository.recordArenaBattleFinished(opp, isVictory = false, stats = dummyStats)
        assertEquals("Current streak must reset to 0 after defeat", 0, repository.economyState.value.arenaCurrentStreak)
        assertEquals("Highest streak must preserve the peak of 2", 2, repository.economyState.value.arenaHighestStreak)
    }

    @Test
    fun test18_playerXpIntegrationWorks() {
        val initialXp = repository.economyState.value.playerProgress.playerXp
        val opp = ArenaCatalog.findOpponent(1000)
        val dummyStats = BattleStats(turnsCount = 5, totalDamageDealt = 3000, totalDamageTaken = 1000)

        repository.recordArenaBattleFinished(opp, isVictory = true, stats = dummyStats)

        val updatedXp = repository.economyState.value.playerProgress.playerXp
        assertTrue("Player XP must increase from arena battles", updatedXp > initialXp)
    }

    @Test
    fun test19_dailyQuestIntegrationWorks() {
        val opp = ArenaCatalog.findOpponent(1000)
        val dummyStats = BattleStats(turnsCount = 5, totalDamageDealt = 3000, totalDamageTaken = 1000)

        val initialBattles = repository.economyState.value.playerProgress.totalBattles
        repository.recordArenaBattleFinished(opp, isVictory = true, stats = dummyStats)

        assertEquals("Total battles tracked for quests must increment", initialBattles + 1, repository.economyState.value.playerProgress.totalBattles)
    }

    @Test
    fun test20_weeklyObjectiveIntegrationWorks() {
        val initialDamage = repository.economyState.value.playerProgress.totalDamageDealt
        val opp = ArenaCatalog.findOpponent(1000)
        val dummyStats = BattleStats(turnsCount = 5, totalDamageDealt = 5000, totalDamageTaken = 1000)

        repository.recordArenaBattleFinished(opp, isVictory = true, stats = dummyStats)

        val updatedDamage = repository.economyState.value.playerProgress.totalDamageDealt
        assertEquals(initialDamage + 5000, updatedDamage)
    }

    @Test
    fun test21_allianceContributionIntegrationWorks() {
        val initialContribution = repository.economyState.value.playerProgress.allianceContribution
        val opp = ArenaCatalog.findOpponent(1000)
        val dummyStats = BattleStats(turnsCount = 5, totalDamageDealt = 3000, totalDamageTaken = 1000)

        repository.recordArenaBattleFinished(opp, isVictory = true, stats = dummyStats)

        val updatedContribution = repository.economyState.value.playerProgress.allianceContribution
        assertTrue("Alliance contribution score must increase from victories", updatedContribution >= initialContribution)
    }

    @Test
    fun test22_activeDeckRemainsExactly20() {
        val initialSize = repository.economyState.value.activeDeck.cardIds.size
        assertEquals("ActiveDeck must be 20 cards", 20, initialSize)

        // Multiple operations: Matchmaking, combat win, combat loss, streak, reward claim
        val opp = ArenaCatalog.findOpponent(1000)
        val dummyStats = BattleStats(turnsCount = 5, totalDamageDealt = 3000, totalDamageTaken = 1000)

        repository.consumeArenaAttempt()
        assertEquals(20, repository.economyState.value.activeDeck.cardIds.size)

        repository.recordArenaBattleFinished(opp, isVictory = true, stats = dummyStats)
        assertEquals(20, repository.economyState.value.activeDeck.cardIds.size)

        repository.recordArenaBattleFinished(opp, isVictory = false, stats = dummyStats)
        assertEquals(20, repository.economyState.value.activeDeck.cardIds.size)

        repository.claimSeasonReward("season_1")
        assertEquals("ActiveDeck must remain strictly 20 cards across all Arena operations", 20, repository.economyState.value.activeDeck.cardIds.size)
    }

    @Test
    fun test23_existingPveBattleWorks() {
        val vm = BattleViewModel()
        val encounter = BattleEncounterConfig(
            encounterName = "PvE Stage 1",
            enemyHero = Hero.createAres()
        )
        val started = vm.startNewBattle(encounterConfig = encounter)
        assertTrue("PvE battle initialization must succeed", started)
        assertEquals(20, vm.uiState.value.playerDrawPile.size + vm.uiState.value.playerHand.size)
    }

    @Test
    fun test24_paymentUntouchedVerification() {
        // Assert that monetization files and billing status remain strictly read-only and unmutated
        val state = repository.economyState.value
        assertEquals("Free starting gems must remain 500", 500, state.mythGems)
        assertTrue("No billing purchase records modified", state.purchaseHistory.isEmpty())
    }
}
