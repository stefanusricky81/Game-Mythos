package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.monetization.PlayerEconomyRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 7D — Player Progression, Daily Quests & Daily Rewards Unit Tests (Section 26).
 *
 * Required test coverage:
 * 1. Player starts at Level 1.
 * 2. Player XP persists.
 * 3. Player level-up works.
 * 4. Level reward cannot be claimed twice.
 * 5. Daily quests generate correctly.
 * 6. Quest progress updates from battle events.
 * 7. Quest reward claiming works atomically and idempotently.
 * 8. Daily quest reset triggers on day change but not within same day.
 * 9. Daily login reward advances day and locks for the rest of today.
 * 10. ActiveDeck card count remains exactly 20 after all progression operations.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlayerProgressionAndDailySystemTest {

    private lateinit var context: Context
    private lateinit var repository: PlayerEconomyRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("mythos_player_economy", Context.MODE_PRIVATE).edit().clear().commit()
        repository = PlayerEconomyRepository()
        repository.initPersistence(context)
    }

    @Test
    fun test1_playerStartsAtLevelOne() {
        val state = repository.economyState.value
        assertEquals("Player must start at Level 1", 1, state.playerProgress.playerLevel)
        assertEquals("Player starting XP must be 0", 0, state.playerProgress.playerXp)
        assertEquals(
            "Level 1 -> 2 requirement must be 1,000 XP",
            1000,
            state.playerProgress.xpRequiredForNextLevel
        )
    }

    @Test
    fun test2_playerXpPersists() {
        repository.addPlayerXp(450)
        assertEquals(450, repository.economyState.value.playerProgress.playerXp)

        // Simulate app restart / recreation
        val newRepo = PlayerEconomyRepository()
        newRepo.initPersistence(context)

        assertEquals("Player XP must survive app restart", 450, newRepo.economyState.value.playerProgress.playerXp)
        assertEquals("Player Level must survive app restart", 1, newRepo.economyState.value.playerProgress.playerLevel)
    }

    @Test
    fun test3_playerLevelUpWorks() {
        // Level 1 -> 2 requires 1,000 XP. Adding 1,250 XP should reach Level 2 with 250 XP leftover.
        val rewards = repository.addPlayerXp(1250)

        val state = repository.economyState.value
        assertEquals("Player should advance to Level 2", 2, state.playerProgress.playerLevel)
        assertEquals("Remainder XP should be 250", 250, state.playerProgress.playerXp)
        assertTrue("Level-up reward should be awarded for Level 2", rewards.any { it.level == 2 })
    }

    @Test
    fun test4_levelRewardCannotBeClaimedTwice() {
        val initialGold = repository.economyState.value.gold
        repository.addPlayerXp(1000) // Reaches Level 2

        val claimResult1 = repository.claimPlayerLevelReward(2)
        assertTrue("Level 2 reward should be claimed successfully", claimResult1.isSuccess)
        assertEquals("Level 2 grants +1,000 Gold", initialGold + 1000, repository.economyState.value.gold)

        // Attempt second claim
        val claimResult2 = repository.claimPlayerLevelReward(2)
        assertTrue("Second claim must fail", claimResult2.isFailure)
        assertEquals("Gold must not increase on duplicate claim", initialGold + 1000, repository.economyState.value.gold)

        // Recreate repo and confirm claimed status is persisted
        val newRepo = PlayerEconomyRepository()
        newRepo.initPersistence(context)
        assertTrue(
            "Claimed level rewards must persist",
            newRepo.economyState.value.playerProgress.claimedLevelRewards.contains(2)
        )
    }

    @Test
    fun test5_dailyQuestsGenerateCorrectly() {
        val quests = repository.economyState.value.dailyQuests
        assertEquals("There must be exactly 5 daily quests", 5, quests.size)

        val expectedIds = setOf("quest_1", "quest_2", "quest_3", "quest_4", "quest_5")
        val actualIds = quests.map { it.questId }.toSet()
        assertEquals("Quest IDs must match configuration", expectedIds, actualIds)

        // Verify initial state
        quests.forEach { quest ->
            assertEquals("Initial quest progress must be 0", 0, quest.progress)
            assertFalse("Initial quest must not be completed", quest.isCompleted)
            assertFalse("Initial quest must not be claimed", quest.isClaimed)
        }
    }

    @Test
    fun test6_questProgressUpdatesFromBattleEvents() {
        // Battle victory should update WIN_BATTLE quest
        repository.updateQuestProgress(QuestType.WIN_BATTLE, 1)
        val q1 = repository.economyState.value.dailyQuests.find { it.questId == "quest_1" }!!
        assertEquals("Quest 1 should be 1/1", 1, q1.progress)
        assertTrue("Quest 1 should be completed", q1.isCompleted)

        // Playing cards should update PLAY_CARDS quest
        repository.updateQuestProgress(QuestType.PLAY_CARDS, 4)
        val q2 = repository.economyState.value.dailyQuests.find { it.questId == "quest_2" }!!
        assertEquals("Quest 2 should have 4 cards played", 4, q2.progress)
        assertFalse("Quest 2 should not be completed yet", q2.isCompleted)

        // Dealing damage should update DEAL_DAMAGE quest
        repository.updateQuestProgress(QuestType.DEAL_DAMAGE, 2500)
        val q3 = repository.economyState.value.dailyQuests.find { it.questId == "quest_3" }!!
        assertEquals("Quest 3 damage should be 2500", 2500, q3.progress)
    }

    @Test
    fun test7_questRewardClaimingWorksAtomicallyAndIdempotently() {
        // Complete Quest 1 (Target: 1, Reward: 500 Gold)
        repository.updateQuestProgress(QuestType.WIN_BATTLE, 1)
        val initialGold = repository.economyState.value.gold

        // First claim must succeed
        val claimResult = repository.claimQuestReward("quest_1")
        assertTrue("First claim must succeed", claimResult.isSuccess)
        assertEquals("Gold must increase by 500", initialGold + 500, repository.economyState.value.gold)

        val updatedQ1 = repository.economyState.value.dailyQuests.find { it.questId == "quest_1" }!!
        assertTrue("Quest 1 must be marked as claimed", updatedQ1.isClaimed)

        // Second claim attempt must fail idempotently
        val duplicateClaim = repository.claimQuestReward("quest_1")
        assertTrue("Duplicate claim must fail", duplicateClaim.isFailure)
        assertEquals("Gold must not increase on duplicate claim", initialGold + 500, repository.economyState.value.gold)
    }

    @Test
    fun test8_dailyQuestResetTriggersOnDayChange() {
        val today = "2026-09-26"
        val tomorrow = "2026-09-27"

        repository.checkDailyReset(forceDate = today)
        repository.updateQuestProgress(QuestType.WIN_BATTLE, 1)
        val q1Before = repository.economyState.value.dailyQuests.find { it.questId == "quest_1" }!!
        assertEquals(1, q1Before.progress)

        // Same day check must NOT reset
        repository.checkDailyReset(forceDate = today)
        val q1SameDay = repository.economyState.value.dailyQuests.find { it.questId == "quest_1" }!!
        assertEquals("Quests must NOT reset within the same day", 1, q1SameDay.progress)

        // Next day check MUST reset quests
        repository.checkDailyReset(forceDate = tomorrow)
        val q1NextDay = repository.economyState.value.dailyQuests.find { it.questId == "quest_1" }!!
        assertEquals("Quests must reset on date change", 0, q1NextDay.progress)
        assertFalse("Quests must be unclaimed after reset", q1NextDay.isClaimed)
    }

    @Test
    fun test9_dailyLoginRewardAdvancesDayAndLocksForToday() {
        val dateToday = "2026-09-26"
        assertTrue("Login reward should be claimable initially", repository.canClaimDailyLoginReward(dateToday))

        val initialGold = repository.economyState.value.gold
        val result = repository.claimDailyLoginReward(dateToday)
        assertTrue("Claim must succeed", result.isSuccess)

        // Day 1 gives 500 Gold
        assertEquals("Day 1 awards 500 Gold", initialGold + 500, repository.economyState.value.gold)
        assertEquals("Reward day must advance to Day 2", 2, repository.economyState.value.loginRewardDay)

        // Claiming again today must fail
        assertFalse("Cannot claim again today", repository.canClaimDailyLoginReward(dateToday))
        val duplicate = repository.claimDailyLoginReward(dateToday)
        assertTrue("Duplicate claim must fail", duplicate.isFailure)
    }

    @Test
    fun test10_activeDeckPreservedAcrossAllProgressionOperations() {
        val originalDeck = repository.economyState.value.activeDeck
        assertEquals("Initial deck must contain 20 cards", 20, originalDeck.cardIds.size)
        val originalCardIds = originalDeck.cardIds.toList()

        // 1. Add Player XP and level up
        repository.addPlayerXp(3000)
        assertEquals("Deck size after level up", 20, repository.economyState.value.activeDeck.cardIds.size)
        assertEquals("Deck cards after level up", originalCardIds, repository.economyState.value.activeDeck.cardIds)

        // 2. Complete and claim daily quest
        repository.updateQuestProgress(QuestType.WIN_BATTLE, 1)
        repository.claimQuestReward("quest_1")
        assertEquals("Deck size after quest claim", 20, repository.economyState.value.activeDeck.cardIds.size)
        assertEquals("Deck cards after quest claim", originalCardIds, repository.economyState.value.activeDeck.cardIds)

        // 3. Claim daily login reward
        repository.claimDailyLoginReward("2026-09-26")
        assertEquals("Deck size after daily login claim", 20, repository.economyState.value.activeDeck.cardIds.size)
        assertEquals("Deck cards after daily login claim", originalCardIds, repository.economyState.value.activeDeck.cardIds)

        // 4. Daily quest reset
        repository.checkDailyReset("2026-09-27")
        assertEquals("Deck size after daily reset", 20, repository.economyState.value.activeDeck.cardIds.size)
        assertEquals("Deck cards after daily reset", originalCardIds, repository.economyState.value.activeDeck.cardIds)
    }
}
