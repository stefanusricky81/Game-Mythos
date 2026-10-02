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
 * Phase 10 — Endgame PvE, World Boss, Raid & Limited-Time Events Unit Tests.
 *
 * Comprehensive coverage:
 * 1. Endgame difficulty configuration and scaling multipliers (Normal, Hard, Nightmare, Mythic).
 * 2. 5 Endgame Challenge Stages / Trials catalog and star condition resolution.
 * 3. World Boss definition (Kronos & Jormungandr) with multi-phase mechanics.
 * 4. World Boss combat contribution tracking (damage, highest hit, battles, score).
 * 5. World Boss reward tiers and idempotent reward claiming (no duplicate claims).
 * 6. Alliance Raid integration and leaderboard calculations.
 * 7. Multi-stage Raid definitions (Olympus Raid & Valhalla Raid - 3 stages each).
 * 8. Daily Raid attempt consumption, depletion, and midnight reset.
 * 9. Raid stage clear rewards and progression persistence.
 * 10. Limited-time event activation/deactivation across mock dates (Wrath of Olympus, Ragnarok, Judgment of Anubis).
 * 11. Event Currency (EVENT_TOKENS) accrual and separation from Gold/Gems.
 * 12. In-game Event Reward Shop purchase limits, currency deduction, and idempotence.
 * 13. 5 Weekly Objectives tracking and claim verification.
 * 14. Calendar-based weekly reset triggers independently of daily quest reset.
 * 15. PERSISTENCE: World Boss contributions, Raid progress, Event tokens, and Weekly Objectives survive repository reload.
 * 16. REGRESSION & SAFETY: ActiveDeck card count remains strictly 20 throughout all Endgame operations.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EndgameAndWorldBossSystemTest {

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
    fun test01_endgameDifficultyMultipliers() {
        assertEquals("Normal multiplier must be 1.0x", 1.0f, EndgameDifficulty.NORMAL.multiplier, 0.001f)
        assertEquals("Hard multiplier must be 1.35x", 1.35f, EndgameDifficulty.HARD.multiplier, 0.001f)
        assertEquals("Nightmare multiplier must be 1.75x", 1.75f, EndgameDifficulty.NIGHTMARE.multiplier, 0.001f)
        assertEquals("Mythic multiplier must be 2.25x", 2.25f, EndgameDifficulty.MYTHIC.multiplier, 0.001f)

        // Scaling checks
        val baseHp = 10_000
        val normalHp = (baseHp * EndgameDifficulty.getHpMultiplier(EndgameDifficulty.NORMAL)).toInt()
        val mythicHp = (baseHp * EndgameDifficulty.getHpMultiplier(EndgameDifficulty.MYTHIC)).toInt()
        assertEquals(10_000, normalHp)
        assertEquals(22_500, mythicHp)

        val power = EndgameDifficulty.getRecommendedPower(10_000, EndgameDifficulty.HARD)
        assertEquals(13_500, power)
    }

    @Test
    fun test02_endgameChallengeStagesCatalog() {
        val trials = EndgameCatalog.ALL_TRIALS
        assertEquals("Must support at least 5 challenge stages", 5, trials.size)

        val olympusTrial = EndgameCatalog.findTrial("trial_olympus")
        assertNotNull(olympusTrial)
        assertEquals("Olympus", olympusTrial!!.world)

        val valhallaTrial = EndgameCatalog.findTrial("trial_valhalla")
        assertNotNull(valhallaTrial)
        assertEquals("Valhalla", valhallaTrial!!.world)

        val egyptTrial = EndgameCatalog.findTrial("trial_egypt")
        assertNotNull(egyptTrial)
        assertEquals("Egypt", egyptTrial!!.world)

        val underworldTrial = EndgameCatalog.findTrial("trial_underworld")
        assertNotNull(underworldTrial)
        assertEquals("Underworld", underworldTrial!!.world)

        val celestialTrial = EndgameCatalog.findTrial("trial_celestial")
        assertNotNull(celestialTrial)
        assertEquals("Celestial", celestialTrial!!.world)

        // Verify conversion to battle encounter config with difficulty scaling
        val config = olympusTrial.toBattleEncounterConfig(EndgameDifficulty.MYTHIC)
        assertTrue(config.enemyHero.currentHp > olympusTrial.baseEnemyHp)
        assertTrue(config.rewards.gold > olympusTrial.baseGoldReward)
    }

    @Test
    fun test03_worldBossDefinitionsAndPhases() {
        val kronos = EndgameCatalog.KRONOS
        assertEquals("world_boss_kronos", kronos.bossId)
        assertEquals(1_000_000L, kronos.maxHp)
        assertEquals(3, kronos.phases.size)

        // Phase 1 check
        val p1 = kronos.getCurrentPhase(1_000_000L)
        assertEquals(1, p1.phaseNumber)
        assertEquals("Temporal Tear", p1.abilityName)

        // Phase 2 check (under 66% HP)
        val p2 = kronos.getCurrentPhase(600_000L)
        assertEquals(2, p2.phaseNumber)
        assertEquals("Chronos Fury", p2.abilityName)
        assertEquals(1.35f, p2.attackMultiplier, 0.001f)
        assertEquals(0.80f, p2.defenseMultiplier, 0.001f)

        // Phase 3 check (under 33% HP)
        val p3 = kronos.getCurrentPhase(250_000L)
        assertEquals(3, p3.phaseNumber)
        assertEquals("Time Collapse", p3.abilityName)

        val jormungandr = EndgameCatalog.JORMUNGANDR
        assertEquals("world_boss_jormungandr", jormungandr.bossId)
        assertEquals(1_200_000L, jormungandr.maxHp)
        assertEquals(3, jormungandr.phases.size)
        assertEquals("Ragnarok Coil", jormungandr.phases[2].abilityName)
    }

    @Test
    fun test04_worldBossBattleAndContributionTracking() {
        val initialHp = repository.economyState.value.worldBossCurrentHp
        val initialTokens = repository.economyState.value.eventTokens

        // Deal 45,000 damage in battle 1
        repository.recordWorldBossBattle("world_boss_kronos", 45_000L, isVictory = false)

        val state1 = repository.economyState.value
        assertEquals("Boss HP should decrease by damage dealt", initialHp - 45_000L, state1.worldBossCurrentHp)
        assertEquals("Event tokens should increase from participation", initialTokens + 25, state1.eventTokens)

        val contrib1 = state1.worldBossContributions["world_boss_kronos"]
        assertNotNull(contrib1)
        assertEquals(45_000L, contrib1!!.totalDamage)
        assertEquals(45_000L, contrib1.highestSingleHit)
        assertEquals(1, contrib1.battlesCompleted)
        assertEquals(0, contrib1.victories)
        assertEquals(1, contrib1.participationCount)

        // Deal 60,000 damage in battle 2 (Victory)
        repository.recordWorldBossBattle("world_boss_kronos", 60_000L, isVictory = true)
        val state2 = repository.economyState.value
        val contrib2 = state2.worldBossContributions["world_boss_kronos"]!!
        assertEquals(105_000L, contrib2.totalDamage)
        assertEquals(60_000L, contrib2.highestSingleHit)
        assertEquals(2, contrib2.battlesCompleted)
        assertEquals(1, contrib2.victories)
    }

    @Test
    fun test05_worldBossRewardsIdempotency() {
        // Accumulate 550,000 damage (Top 5% tier)
        repository.recordWorldBossBattle("world_boss_kronos", 550_000L, isVictory = true)

        val initialGold = repository.economyState.value.gold
        val initialTokens = repository.economyState.value.eventTokens

        val result1 = repository.claimWorldBossReward("world_boss_kronos")
        assertTrue("Claim must succeed", result1.isSuccess)
        val tier = result1.getOrNull()!!
        assertEquals(WorldBossRewardTier.TOP_5_PERCENT, tier)

        val afterClaimState = repository.economyState.value
        assertEquals(initialGold + tier.goldReward, afterClaimState.gold)
        assertEquals(initialTokens + tier.eventTokensReward, afterClaimState.eventTokens)

        // Attempt second claim — must fail (idempotent)
        val result2 = repository.claimWorldBossReward("world_boss_kronos")
        assertTrue("Duplicate claim must fail", result2.isFailure)
        assertEquals(initialGold + tier.goldReward, repository.economyState.value.gold)
    }

    @Test
    fun test06_allianceRaidBoardData() {
        val board = EndgameCatalog.getMockAllianceRaidBoard()
        assertEquals(10, board.size)
        val rank1 = board.first()
        assertEquals(1, rank1.rank)
        assertTrue(rank1.totalDamage > 0L)

        val myAlliance = board.find { it.allianceName == "Olympus Guardians" }
        assertNotNull(myAlliance)
        assertEquals(7, myAlliance!!.rank)
        assertEquals(18, myAlliance.membersCount)
    }

    @Test
    fun test07_raidDefinitionsAndStages() {
        val raids = EndgameCatalog.ALL_RAIDS
        assertEquals(2, raids.size)

        val olympusRaid = EndgameCatalog.findRaid("raid_olympus")
        assertNotNull(olympusRaid)
        assertEquals(3, olympusRaid!!.stages.size)
        assertEquals("Temple Gate", olympusRaid.stages[0].name)
        assertEquals("Titan's Army", olympusRaid.stages[1].name)
        assertEquals("Titan Boss", olympusRaid.stages[2].name)
        assertTrue(olympusRaid.stages[2].isBossStage)

        val valhallaRaid = EndgameCatalog.findRaid("raid_valhalla")
        assertNotNull(valhallaRaid)
        assertEquals(3, valhallaRaid!!.stages.size)
        assertEquals("Frozen Gate", valhallaRaid.stages[0].name)
        assertEquals("Einherjar Legion", valhallaRaid.stages[1].name)
        assertEquals("Jormungandr Avatar", valhallaRaid.stages[2].name)
    }

    @Test
    fun test08_raidDailyAttemptsAndConsumption() {
        assertEquals(3, repository.economyState.value.raidDailyAttempts)

        assertTrue(repository.consumeRaidAttempt())
        assertEquals(2, repository.economyState.value.raidDailyAttempts)

        assertTrue(repository.consumeRaidAttempt())
        assertEquals(1, repository.economyState.value.raidDailyAttempts)

        assertTrue(repository.consumeRaidAttempt())
        assertEquals(0, repository.economyState.value.raidDailyAttempts)

        // Attempts exhausted: must fail
        assertFalse(repository.consumeRaidAttempt())
        assertEquals(0, repository.economyState.value.raidDailyAttempts)

        // Next day daily reset must replenish 3 attempts
        repository.checkDailyReset("2099-01-01")
        assertEquals("New day must reset attempts to 3", 3, repository.economyState.value.raidDailyAttempts)
    }

    @Test
    fun test09_raidStageProgressionAndRewards() {
        val initialGold = repository.economyState.value.gold
        val initialTokens = repository.economyState.value.eventTokens

        val success = repository.recordRaidStageClear(
            raidId = "raid_olympus",
            stageNumber = 1,
            goldReward = 3_000,
            tokenReward = 20
        )
        assertTrue(success)

        val state = repository.economyState.value
        assertEquals(1, state.raidStageProgress["raid_olympus"])
        assertEquals(initialGold + 3_000, state.gold)
        assertEquals(initialTokens + 20, state.eventTokens)
    }

    @Test
    fun test10_limitedTimeEventsActivation() {
        val events = repository.economyState.value.activeEvents

        val wrathOfOlympus = events.find { it.eventId == "event_wrath_of_olympus" }
        assertNotNull("Wrath of Olympus event must exist", wrathOfOlympus)
        assertTrue("Wrath of Olympus is active in late Sept 2026", wrathOfOlympus!!.isWithinDateRange("2026-09-25"))

        val ragnarok = events.find { it.eventId == "event_ragnarok" }
        assertNotNull("Ragnarok event must exist", ragnarok)
        assertTrue("Ragnarok is active in late Sept 2026", ragnarok!!.isWithinDateRange("2026-09-28"))

        val judgmentOfAnubis = events.find { it.eventId == "event_judgment_of_anubis" }
        assertNotNull("Judgment of Anubis event must exist", judgmentOfAnubis)
        assertFalse("Judgment of Anubis inactive in Sept 2026", judgmentOfAnubis!!.isWithinDateRange("2026-09-28"))
        assertTrue("Judgment of Anubis active in Oct 2026", judgmentOfAnubis.isWithinDateRange("2026-10-05"))
    }

    @Test
    fun test11_eventCurrencyAccumulation() {
        val initialTokens = repository.economyState.value.eventTokens
        repository.addEventTokens(75)
        assertEquals(initialTokens + 75, repository.economyState.value.eventTokens)

        // Confirm Event Tokens are distinct from Gold and Gems
        val initialGold = repository.economyState.value.gold
        val initialGems = repository.economyState.value.mythGems
        repository.addEventTokens(50)
        assertEquals(initialGold, repository.economyState.value.gold)
        assertEquals(initialGems, repository.economyState.value.mythGems)
    }

    @Test
    fun test12_eventRewardShopPurchasesAndLimits() {
        repository.setEventTokensForTesting(200)

        val initialHerculesShards = repository.economyState.value.heroShards[HerculesIdentity.HERO_ID] ?: 0

        // Purchase Hercules Shards x10 (cost: 50 tokens)
        val result1 = repository.purchaseEventShopItem("shop_hercules_shards")
        assertTrue("Purchase should succeed", result1.isSuccess)
        assertEquals(150, repository.economyState.value.eventTokens)
        assertEquals(initialHerculesShards + 10, repository.economyState.value.heroShards[HerculesIdentity.HERO_ID])
        assertEquals(1, repository.economyState.value.eventShopPurchases["shop_hercules_shards"])

        // Purchase remaining 4 to reach limit (5 max)
        for (i in 2..5) {
            repository.setEventTokensForTesting(100)
            assertTrue(repository.purchaseEventShopItem("shop_hercules_shards").isSuccess)
        }

        // 6th purchase must fail due to limit
        val result6 = repository.purchaseEventShopItem("shop_hercules_shards")
        assertTrue("Purchase beyond limit must fail", result6.isFailure)
    }

    @Test
    fun test13_weeklyObjectivesProgressAndClaim() {
        val objectives = repository.economyState.value.weeklyObjectives
        assertEquals("Must support 5 weekly objectives", 5, objectives.size)

        val winBattlesObj = objectives.find { it.type == WeeklyObjectiveType.WIN_BATTLES }
        assertNotNull(winBattlesObj)
        assertEquals(10L, winBattlesObj!!.target)

        // Progress win battles to 10
        repository.updateWeeklyObjectiveProgress(WeeklyObjectiveType.WIN_BATTLES, 10L)
        val updatedObj = repository.economyState.value.weeklyObjectives.find { it.id == winBattlesObj.id }!!
        assertTrue(updatedObj.isCompleted)
        assertTrue(updatedObj.canClaim)

        val initialGold = repository.economyState.value.gold
        val initialTokens = repository.economyState.value.eventTokens
        val claimResult = repository.claimWeeklyObjective(winBattlesObj.id)
        assertTrue(claimResult.isSuccess)

        val stateAfterClaim = repository.economyState.value
        assertEquals(initialGold + winBattlesObj.goldReward, stateAfterClaim.gold)
        assertEquals(initialTokens + winBattlesObj.eventTokensReward, stateAfterClaim.eventTokens)

        // Duplicate claim must fail
        val duplicateClaim = repository.claimWeeklyObjective(winBattlesObj.id)
        assertTrue(duplicateClaim.isFailure)
    }

    @Test
    fun test14_weeklyResetDoesNotAffectDailyQuests() {
        repository.setWeeklyObjectiveProgressForTesting("weekly_win_battles", 8L)
        repository.setDailyQuestProgressForTesting("quest_1", 12)

        // Trigger weekly reset with new week
        repository.checkWeeklyReset("2026-W42")

        val state = repository.economyState.value
        assertEquals("2026-W42", state.weeklyObjectiveWeek)
        val resetWeekly = state.weeklyObjectives.find { it.id == "weekly_win_battles" }!!
        assertEquals("Weekly objective should reset to 0 progress", 0L, resetWeekly.progress)

        // Daily quest must NOT be reset by weekly check
        val dailyQuest = state.dailyQuests.find { it.questId == "quest_1" }!!
        assertEquals("Daily quest progress must be preserved", 12, dailyQuest.progress)
    }

    @Test
    fun test15_persistenceOfEndgameSystems() {
        repository.setEventTokensForTesting(320)
        repository.recordWorldBossBattle("world_boss_kronos", 80_000L, isVictory = false)
        repository.recordRaidStageClear("raid_olympus", 2, 4_000, 30)
        repository.recordTrialClear("trial_olympus", EndgameDifficulty.HARD, 3, 5_000, 3_000, 35)

        // Recreate repository from shared preferences
        val reloaded = PlayerEconomyRepository()
        reloaded.initPersistence(context)

        val reloadedState = reloaded.economyState.value
        assertTrue("Event tokens must survive reload", reloadedState.eventTokens >= 320)
        assertEquals("Raid progress must survive reload", 2, reloadedState.raidStageProgress["raid_olympus"])
        assertEquals("Trial stars must survive reload", 3, reloadedState.trialStars["trial_olympus"])
        val contrib = reloadedState.worldBossContributions["world_boss_kronos"]
        assertNotNull(contrib)
        assertEquals("World boss damage must survive reload", 80_000L, contrib!!.totalDamage)
    }

    @Test
    fun test16_activeDeckRemainsStrictly20CardsAcrossEndgameOperations() {
        val initialDeck = repository.economyState.value.activeDeck
        assertEquals(20, initialDeck.cardIds.size)

        // 1. World Boss operations
        repository.recordWorldBossBattle("world_boss_kronos", 25_000L, isVictory = false)
        assertEquals(20, repository.economyState.value.activeDeck.cardIds.size)

        // 2. Raid battle & consumption
        repository.consumeRaidAttempt()
        repository.recordRaidStageClear("raid_olympus", 1, 3_000, 20)
        assertEquals(20, repository.economyState.value.activeDeck.cardIds.size)

        // 3. Trial battle
        repository.recordTrialClear("trial_valhalla", EndgameDifficulty.NORMAL, 3, 5_000, 3_000, 40)
        assertEquals(20, repository.economyState.value.activeDeck.cardIds.size)

        // 4. Shop purchases
        repository.setEventTokensForTesting(500)
        repository.purchaseEventShopItem("shop_hercules_shards")
        repository.purchaseEventShopItem("shop_card_shards_pack")
        repository.purchaseEventShopItem("shop_exclusive_card")
        assertEquals(20, repository.economyState.value.activeDeck.cardIds.size)

        // 5. Weekly objectives
        repository.updateWeeklyObjectiveProgress(WeeklyObjectiveType.WIN_BATTLES, 10L)
        repository.claimWeeklyObjective("weekly_win_battles")
        assertEquals(20, repository.economyState.value.activeDeck.cardIds.size)

        // 6. App reload
        val newRepo = PlayerEconomyRepository()
        newRepo.initPersistence(context)
        assertEquals("ActiveDeck must be strictly 20 cards after all endgame actions and reload", 20, newRepo.economyState.value.activeDeck.cardIds.size)
    }
}
