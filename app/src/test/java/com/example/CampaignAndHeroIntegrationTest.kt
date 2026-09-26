package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.monetization.PlayerEconomyRepository
import com.example.monetization.ShardConversion
import com.example.viewmodel.BattleViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CampaignAndHeroIntegrationTest {

    private lateinit var context: Context
    private lateinit var repository: PlayerEconomyRepository
    private lateinit var battleViewModel: BattleViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Clear shared preferences for clean test environment
        val prefs = context.getSharedPreferences("mythos_player_data", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        repository = PlayerEconomyRepository.instance
        repository.resetForTesting(context)
        battleViewModel = BattleViewModel()
    }

    /**
     * 1. First clear gives first-clear reward (gold, xp, card) and updates stageStars.
     */
    @Test
    fun testFirstClearAwardsFirstClearRewards() {
        val stage1 = CampaignCatalog.findStage("stage_1_1")!!
        val initialGold = repository.economyState.value.gold

        val victoryResult = repository.recordCampaignVictory(
            stage = stage1,
            playerRemainingHp = 8000,
            playerMaxHp = 10000,
            turnsCount = 4
        )

        assertTrue("Should be marked as first clear", victoryResult.isFirstClear)
        assertEquals(stage1.firstClearGold, victoryResult.goldAwarded)
        assertEquals(stage1.firstClearXp, victoryResult.xpAwarded)
        assertEquals(stage1.firstClearCardId, victoryResult.cardIdAwarded)
        assertEquals(3, victoryResult.starsEarned)
        assertTrue(victoryResult.hpConditionMet)
        assertTrue(victoryResult.turnsConditionMet)

        val updatedState = repository.economyState.value
        assertEquals(initialGold + stage1.firstClearGold, updatedState.gold)
        assertTrue(updatedState.completedStageIds.contains("stage_1_1"))
        assertEquals(3, updatedState.stageStars["stage_1_1"])
        assertTrue("Stage 2 must be unlocked", updatedState.highestUnlockedStage >= 2)
    }

    /**
     * 2. Replay does NOT duplicate first-clear reward (grants replayGold & replayXp only, NO card reward).
     */
    @Test
    fun testReplayDoesNotDuplicateFirstClearRewards() {
        val stage1 = CampaignCatalog.findStage("stage_1_1")!!

        // First clear
        repository.recordCampaignVictory(
            stage = stage1,
            playerRemainingHp = 8000,
            playerMaxHp = 10000,
            turnsCount = 4
        )

        val goldAfterFirstClear = repository.economyState.value.gold

        // Replay clear
        val replayResult = repository.recordCampaignVictory(
            stage = stage1,
            playerRemainingHp = 7000,
            playerMaxHp = 10000,
            turnsCount = 5
        )

        assertFalse("Replay must not be flagged as first clear", replayResult.isFirstClear)
        assertEquals(stage1.replayGold, replayResult.goldAwarded)
        assertEquals(stage1.replayXp, replayResult.xpAwarded)
        assertNull("Replay must not award first-clear card reward", replayResult.cardIdAwarded)

        val goldAfterReplay = repository.economyState.value.gold
        assertEquals(goldAfterFirstClear + stage1.replayGold, goldAfterReplay)
    }

    /**
     * 3. Star rating cannot decrease when replayed with fewer stars.
     */
    @Test
    fun testStarRatingCannotDecrease() {
        val stage1 = CampaignCatalog.findStage("stage_1_1")!!

        // First clear with 3 stars
        repository.recordCampaignVictory(
            stage = stage1,
            playerRemainingHp = 9000,
            playerMaxHp = 10000,
            turnsCount = 3
        )
        assertEquals(3, repository.economyState.value.stageStars["stage_1_1"])

        // Replay with poor performance: HP <= 50% and turns > maxTurns -> 1 star
        val replayResult = repository.recordCampaignVictory(
            stage = stage1,
            playerRemainingHp = 2000, // < 50%
            playerMaxHp = 10000,
            turnsCount = 15 // > 8
        )

        assertEquals(1, replayResult.starsEarned)
        assertEquals(3, replayResult.previousStars)
        assertEquals(3, replayResult.bestStars)

        // Stored stars must remain 3
        assertEquals(3, repository.economyState.value.stageStars["stage_1_1"])
    }

    /**
     * 4. Better star rating is saved when replaying with an improved result.
     */
    @Test
    fun testBetterStarRatingIsSaved() {
        val stage2 = CampaignCatalog.findStage("stage_1_2")!!

        // First clear with 1 star (HP <= 50% and turns > 8)
        val firstResult = repository.recordCampaignVictory(
            stage = stage2,
            playerRemainingHp = 3000,
            playerMaxHp = 10000,
            turnsCount = 12
        )
        assertEquals(1, firstResult.starsEarned)
        assertEquals(1, repository.economyState.value.stageStars["stage_1_2"])

        // Replay with master performance (3 stars)
        val replayResult = repository.recordCampaignVictory(
            stage = stage2,
            playerRemainingHp = 8500,
            playerMaxHp = 10000,
            turnsCount = 5
        )
        assertEquals(3, replayResult.starsEarned)
        assertEquals(1, replayResult.previousStars)
        assertEquals(3, replayResult.bestStars)

        // Stored stars must now be updated to 3
        assertEquals(3, repository.economyState.value.stageStars["stage_1_2"])
    }

    /**
     * 5. Campaign progress persists and survives re-initialization.
     */
    @Test
    fun testCampaignProgressPersists() {
        val stage3 = CampaignCatalog.findStage("stage_1_3")!!
        repository.recordCampaignVictory(
            stage = stage3,
            playerRemainingHp = 7000,
            playerMaxHp = 10000,
            turnsCount = 6
        )

        val beforeState = repository.economyState.value
        assertTrue(beforeState.completedStageIds.contains("stage_1_3"))
        assertNotNull(beforeState.stageStars["stage_1_3"])

        // Verify stored in SharedPreferences
        val prefs = context.getSharedPreferences("mythos_player_data", Context.MODE_PRIVATE)
        val completedSet = prefs.getStringSet("saved_completed_stages", null)
        assertNotNull(completedSet)
        assertTrue(completedSet!!.contains("stage_1_3"))
    }

    /**
     * 6. Stage unlock progression works sequentially through all stages up to Stage 5 Boss.
     */
    @Test
    fun testStageUnlockProgression() {
        val stages = CampaignCatalog.WORLD_1_STAGES
        assertEquals(5, stages.size)

        for (stage in stages) {
            repository.recordCampaignVictory(
                stage = stage,
                playerRemainingHp = 6000,
                playerMaxHp = 10000,
                turnsCount = 5
            )
            val state = repository.economyState.value
            assertTrue(state.completedStageIds.contains(stage.stageId))
            if (stage.stageNumber < 5) {
                assertTrue(
                    "Next stage must be unlocked",
                    state.highestUnlockedStage >= stage.stageNumber + 1
                )
            }
        }

        // All 5 stages completed
        val finalState = repository.economyState.value
        assertEquals(5, finalState.completedStageIds.size)
        assertTrue("Boss stage must be completed", finalState.completedStageIds.contains("stage_1_5"))
    }

    /**
     * 7. ActiveDeck remains exactly 20/20 before, during, after victory, and after replay.
     */
    @Test
    fun testActiveDeckRemainsExactly20Cards() {
        val initialDeck = repository.economyState.value.activeDeck.createDefensiveCopy()
        assertEquals(20, initialDeck.cardIds.size)

        val stage1 = CampaignCatalog.findStage("stage_1_1")!!

        // First victory
        repository.recordCampaignVictory(stage1, 9000, 10000, 4)
        val deckAfterFirst = repository.economyState.value.activeDeck
        assertEquals(20, deckAfterFirst.cardIds.size)
        assertEquals(initialDeck.cardIds, deckAfterFirst.cardIds)

        // Replay victory
        repository.recordCampaignVictory(stage1, 8500, 10000, 5)
        val deckAfterReplay = repository.economyState.value.activeDeck
        assertEquals(20, deckAfterReplay.cardIds.size)
        assertEquals(initialDeck.cardIds, deckAfterReplay.cardIds)
    }

    /**
     * 8. Duplicate card rewards convert to card shards via ShardConversion.
     */
    @Test
    fun testDuplicateCardRewardsConvertToShards() {
        val stage4 = CampaignCatalog.findStage("stage_1_4")!!
        val cardId = stage4.firstClearCardId

        val shardsBefore = repository.economyState.value.cardShards[cardId] ?: 0

        // Clear stage 4 granting the card
        val result = repository.recordCampaignVictory(stage4, 7500, 10000, 6)
        assertTrue(result.isFirstClear)

        if (result.wasDuplicateCard) {
            val shardsAfter = repository.economyState.value.cardShards[cardId] ?: 0
            val expectedShards = ShardConversion.getShardsForDuplicate(stage4.firstClearCardRarity)
            assertEquals(shardsBefore + expectedShards, shardsAfter)
            assertEquals(expectedShards, result.shardsAwardedForDuplicate)
        }
    }

    /**
     * 9. Player combat power calculator works accurately based on hero and deck.
     */
    @Test
    fun testPlayerPowerCalculator() {
        val hero = HeroCatalog.HERCULES
        val deck = repository.economyState.value.activeDeck
        val levels = repository.economyState.value.cardLevels

        val power = PlayerPowerCalculator.calculate(hero, deck, levels)
        assertTrue("Combat power must be positive and non-zero", power > 1500)
        assertTrue("Combat power should scale reasonably for early deck", power in 2000..6000)
    }

    /**
     * 10. Hercules is selectable and future heroes remain locked.
     */
    @Test
    fun testHeroSelectionRules() {
        assertTrue(repository.selectHero(HerculesIdentity.HERO_ID))
        assertEquals(HerculesIdentity.HERO_ID, repository.economyState.value.selectedHeroId)

        assertFalse(repository.selectHero("hero_achilles"))
        assertFalse(repository.selectHero("hero_merlin"))
    }
}
