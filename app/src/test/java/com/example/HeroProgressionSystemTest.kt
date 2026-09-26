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
 * Phase 7C — Hero Progression & Hero Unlock System Unit & Integration Tests (Section 20).
 *
 * Required tests:
 * 1. Hercules starts unlocked.
 * 2. Achilles starts locked.
 * 3. Merlin starts locked.
 * 4. Completing Stage 2 unlocks Achilles.
 * 5. Completing Stage 5 unlocks Merlin.
 * 6. Hero unlock persists.
 * 7. Hero XP persists.
 * 8. Hero shards persist.
 * 9. Hero upgrade deducts exact Gold + Hero Shards.
 * 10. Upgrade cannot happen with insufficient resources.
 * 11. Hero stats scale correctly by level.
 * 12. Hero selection does not mutate ActiveDeck cardIds.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HeroProgressionSystemTest {

    private lateinit var context: Context
    private lateinit var repository: PlayerEconomyRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val prefs = context.getSharedPreferences("mythos_player_data", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        repository = PlayerEconomyRepository.instance
        repository.resetForTesting(context)
    }

    /**
     * 1. Hercules starts unlocked.
     */
    @Test
    fun testHerculesStartsUnlocked() {
        val state = repository.economyState.value
        val progress = state.getHeroProgress(HerculesIdentity.HERO_ID)

        assertTrue("Hercules must be unlocked by default", progress.isUnlocked)
        assertTrue("Hercules must be in ownedHeroIds", state.ownedHeroIds.contains(HerculesIdentity.HERO_ID))
        assertEquals("Hercules must be selected hero by default", HerculesIdentity.HERO_ID, state.selectedHeroId)
        assertEquals(1, progress.level)
    }

    /**
     * 2. Achilles starts locked.
     */
    @Test
    fun testAchillesStartsLocked() {
        val state = repository.economyState.value
        val progress = state.getHeroProgress("hero_achilles")

        assertFalse("Achilles must start locked", progress.isUnlocked)
        assertFalse("Achilles must not be in ownedHeroIds", state.ownedHeroIds.contains("hero_achilles"))
        assertFalse("Selecting locked Achilles must fail", repository.selectHero("hero_achilles"))
    }

    /**
     * 3. Merlin starts locked.
     */
    @Test
    fun testMerlinStartsLocked() {
        val state = repository.economyState.value
        val progress = state.getHeroProgress("hero_merlin")

        assertFalse("Merlin must start locked", progress.isUnlocked)
        assertFalse("Merlin must not be in ownedHeroIds", state.ownedHeroIds.contains("hero_merlin"))
        assertFalse("Selecting locked Merlin must fail", repository.selectHero("hero_merlin"))
    }

    /**
     * 4. Completing Stage 2 unlocks Achilles.
     */
    @Test
    fun testCompletingStage2UnlocksAchilles() {
        val stage1 = CampaignCatalog.findStage("stage_1_1")!!
        val stage2 = CampaignCatalog.findStage("stage_1_2")!!

        // Clear Stage 1
        repository.recordCampaignVictory(stage1, 8000, 10000, 4)
        assertFalse("Achilles still locked after Stage 1", repository.economyState.value.ownedHeroIds.contains("hero_achilles"))

        // Clear Stage 2 (Wrath of the Arena)
        val stage2Result = repository.recordCampaignVictory(stage2, 8500, 10000, 5)
        assertEquals("Achilles", stage2Result.heroUnlocked)

        val updatedState = repository.economyState.value
        assertTrue("Achilles must be unlocked after Stage 2 victory", updatedState.ownedHeroIds.contains("hero_achilles"))
        assertTrue("Achilles progress must reflect unlocked state", updatedState.getHeroProgress("hero_achilles").isUnlocked)

        // Achilles can now be selected
        val selectSuccess = repository.selectHero("hero_achilles")
        assertTrue("Player should now be able to select unlocked Achilles", selectSuccess)
        assertEquals("hero_achilles", repository.economyState.value.selectedHeroId)
    }

    /**
     * 5. Completing Stage 5 unlocks Merlin.
     */
    @Test
    fun testCompletingStage5UnlocksMerlin() {
        val stages = CampaignCatalog.WORLD_1_STAGES
        assertEquals(5, stages.size)

        for (stage in stages) {
            val result = repository.recordCampaignVictory(stage, 7000, 10000, 5)
            if (stage.stageId == "stage_1_5") {
                assertEquals("Merlin", result.heroUnlocked)
            }
        }

        val finalState = repository.economyState.value
        assertTrue("Merlin must be unlocked after Stage 5 boss clear", finalState.ownedHeroIds.contains("hero_merlin"))
        assertTrue("Merlin progress must reflect unlocked state", finalState.getHeroProgress("hero_merlin").isUnlocked)

        // Merlin can now be selected
        val selectSuccess = repository.selectHero("hero_merlin")
        assertTrue("Player should now be able to select unlocked Merlin", selectSuccess)
        assertEquals("hero_merlin", repository.economyState.value.selectedHeroId)
    }

    /**
     * 6. Hero unlock persists.
     */
    @Test
    fun testHeroUnlockPersists() {
        val stage1 = CampaignCatalog.findStage("stage_1_1")!!
        val stage2 = CampaignCatalog.findStage("stage_1_2")!!

        repository.recordCampaignVictory(stage1, 8000, 10000, 4)
        repository.recordCampaignVictory(stage2, 8000, 10000, 4)

        assertTrue(repository.economyState.value.ownedHeroIds.contains("hero_achilles"))

        // Verify stored in SharedPreferences
        val prefs = context.getSharedPreferences("mythos_player_data", Context.MODE_PRIVATE)
        val savedHeroes = prefs.getStringSet("saved_owned_heroes", null)
        assertNotNull(savedHeroes)
        assertTrue("Persistent storage must contain hero_achilles", savedHeroes!!.contains("hero_achilles"))

        // Create new repository instance simulating app reload
        val newRepo = PlayerEconomyRepository()
        newRepo.initPersistence(context)
        assertTrue("Achilles unlock must persist across repository reload", newRepo.economyState.value.ownedHeroIds.contains("hero_achilles"))
    }

    /**
     * 7. Hero XP persists.
     */
    @Test
    fun testHeroXpPersists() {
        repository.addHeroXp(HerculesIdentity.HERO_ID, 1850)
        assertEquals(1850, repository.economyState.value.heroXp[HerculesIdentity.HERO_ID])

        val prefs = context.getSharedPreferences("mythos_player_data", Context.MODE_PRIVATE)
        val savedXpJson = prefs.getString("saved_hero_xp", null)
        assertNotNull(savedXpJson)
        assertTrue("Persistent storage must contain saved XP", savedXpJson!!.contains("1850"))

        // Create new repository instance simulating app reload
        val newRepo = PlayerEconomyRepository()
        newRepo.initPersistence(context)
        assertEquals(1850, newRepo.economyState.value.heroXp[HerculesIdentity.HERO_ID])
    }

    /**
     * 8. Hero shards persist.
     */
    @Test
    fun testHeroShardsPersist() {
        val initialShards = repository.economyState.value.heroShards[HerculesIdentity.HERO_ID] ?: 0
        repository.addHeroShards(HerculesIdentity.HERO_ID, 45)

        val updatedShards = repository.economyState.value.heroShards[HerculesIdentity.HERO_ID] ?: 0
        assertEquals(initialShards + 45, updatedShards)

        val prefs = context.getSharedPreferences("mythos_player_data", Context.MODE_PRIVATE)
        val savedShardsJson = prefs.getString("saved_hero_shards", null)
        assertNotNull(savedShardsJson)

        // Reload into clean repository
        val newRepo = PlayerEconomyRepository()
        newRepo.initPersistence(context)
        assertEquals(updatedShards, newRepo.economyState.value.heroShards[HerculesIdentity.HERO_ID])
    }

    /**
     * 9. Hero upgrade deducts exact Gold + Hero Shards.
     */
    @Test
    fun testHeroUpgradeDeductsExactGoldAndHeroShards() {
        val heroId = HerculesIdentity.HERO_ID
        // Set initial resources: 30,000 gold, 50 Hercules shards, Level 1
        repository.resetForTesting(context)
        repository.addHeroShards(heroId, 50) // Now has at least 50 shards

        val stateBefore = repository.economyState.value
        val goldBefore = stateBefore.gold
        val shardsBefore = stateBefore.heroShards[heroId] ?: 0
        val levelBefore = stateBefore.heroProgression[heroId] ?: 1
        assertEquals(1, levelBefore)

        val cost = HeroProgressionConfig.getUpgradeCost(1)!!
        assertEquals(5_000, cost.goldCost)
        assertEquals(20, cost.shardCost)

        val upgradeResult = repository.upgradeHero(heroId)
        assertTrue("Upgrade should succeed with sufficient resources", upgradeResult.isSuccess)

        val stateAfter = repository.economyState.value
        assertEquals("Level must increment to 2", 2, stateAfter.heroProgression[heroId])
        assertEquals("Exact gold must be deducted", goldBefore - cost.goldCost, stateAfter.gold)
        assertEquals("Exact hero shards must be deducted", shardsBefore - cost.shardCost, stateAfter.heroShards[heroId])
    }

    /**
     * 10. Upgrade cannot happen with insufficient resources.
     */
    @Test
    fun testUpgradeCannotHappenWithInsufficientResources() {
        val heroId = HerculesIdentity.HERO_ID
        repository.resetForTesting(context)

        // Set gold to insufficient
        val state = repository.economyState.value
        val cost = HeroProgressionConfig.getUpgradeCost(1)!!

        // Case A: Insufficient Gold
        repository.addHeroShards(heroId, 100)
        // Drain gold
        repository.upgradeCard("c_power_strike") // non-destructive check: direct test with insufficient gold
        // Create an economy with 0 gold
        val prefs = context.getSharedPreferences("mythos_player_data", Context.MODE_PRIVATE)
        prefs.edit().putInt("saved_gold", 100).commit() // only 100 gold, need 5,000
        val lowGoldRepo = PlayerEconomyRepository()
        lowGoldRepo.initPersistence(context)

        val failedResult = lowGoldRepo.upgradeHero(heroId)
        assertTrue("Upgrade must fail when gold is insufficient", failedResult.isFailure)
        assertTrue(failedResult.exceptionOrNull()?.message?.contains("Insufficient Gold") == true)
        assertEquals("Level must not change on failed upgrade", 1, lowGoldRepo.economyState.value.heroProgression[heroId])

        // Case B: Insufficient Hero Shards
        prefs.edit()
            .putInt("saved_gold", 50_000)
            .putString("saved_hero_shards", """{"hero_hercules": 5}""") // only 5 shards, need 20
            .commit()
        val lowShardRepo = PlayerEconomyRepository()
        lowShardRepo.initPersistence(context)

        val failedShardResult = lowShardRepo.upgradeHero(heroId)
        assertTrue("Upgrade must fail when shards are insufficient", failedShardResult.isFailure)
        assertTrue(failedShardResult.exceptionOrNull()?.message?.contains("Insufficient Hero Shards") == true)
        assertEquals("Gold must not be deducted on failure", 50_000, lowShardRepo.economyState.value.gold)
    }

    /**
     * 11. Hero stats scale correctly by level.
     */
    @Test
    fun testHeroStatsScaleCorrectlyByLevel() {
        val herculesDef = HeroCatalog.HERCULES

        // Level 1: 100% (10,000 HP, 2,800 ATK, 2,600 DEF)
        val l1 = HeroProgressionConfig.getScaledStats(herculesDef, 1)
        assertEquals(10_000, l1.hp)
        assertEquals(2_800, l1.attack)
        assertEquals(2_600, l1.defense)
        assertEquals(1.00f, l1.multiplier, 0.001f)

        // Level 2: 105% (10,500 HP, 2,940 ATK, 2,730 DEF)
        val l2 = HeroProgressionConfig.getScaledStats(herculesDef, 2)
        assertEquals(10_500, l2.hp)
        assertEquals(2_940, l2.attack)
        assertEquals(2_730, l2.defense)
        assertEquals(1.05f, l2.multiplier, 0.001f)

        // Level 3: 110% (11,000 HP, 3,080 ATK, 2,860 DEF)
        val l3 = HeroProgressionConfig.getScaledStats(herculesDef, 3)
        assertEquals(11_000, l3.hp)
        assertEquals(3_080, l3.attack)
        assertEquals(2_860, l3.defense)
        assertEquals(1.10f, l3.multiplier, 0.001f)

        // Level 4: 116% (11,600 HP, 3,248 ATK, 3,003 DEF - canonical match)
        val l4 = HeroProgressionConfig.getScaledStats(herculesDef, 4)
        assertEquals(11_600, l4.hp)
        assertEquals(3_248, l4.attack)
        assertEquals(3_003, l4.defense)
        assertEquals(1.16f, l4.multiplier, 0.001f)

        // Level 5: 123% (12,300 HP, 3,444 ATK, 3,198 DEF)
        val l5 = HeroProgressionConfig.getScaledStats(herculesDef, 5)
        assertEquals(12_300, l5.hp)
        assertEquals(3_444, l5.attack)
        assertEquals(3_198, l5.defense)
        assertEquals(1.23f, l5.multiplier, 0.001f)
    }

    /**
     * 12. Hero selection does not mutate ActiveDeck cardIds.
     */
    @Test
    fun testHeroSelectionDoesNotMutateActiveDeckCardIds() {
        val initialDeck = repository.economyState.value.activeDeck
        assertEquals(20, initialDeck.cardIds.size)
        val initialCardIds = initialDeck.cardIds.toList()

        // Unlock Achilles
        repository.setHeroUnlockForTesting("hero_achilles", true)

        // Select Achilles
        val selected = repository.selectHero("hero_achilles")
        assertTrue(selected)

        val updatedDeck = repository.economyState.value.activeDeck
        assertEquals("hero_achilles", updatedDeck.heroId)
        assertEquals("ActiveDeck card count MUST remain exactly 20", 20, updatedDeck.cardIds.size)
        assertEquals("ActiveDeck cards must NOT be modified, reordered, truncated, or reset", initialCardIds, updatedDeck.cardIds)

        // Perform hero upgrade
        repository.addHeroShards(HerculesIdentity.HERO_ID, 50)
        repository.upgradeHero(HerculesIdentity.HERO_ID)

        val deckAfterUpgrade = repository.economyState.value.activeDeck
        assertEquals(20, deckAfterUpgrade.cardIds.size)
        assertEquals(initialCardIds, deckAfterUpgrade.cardIds)

        // Switch back to Hercules
        repository.selectHero(HerculesIdentity.HERO_ID)
        val deckAfterSwitch = repository.economyState.value.activeDeck
        assertEquals(HerculesIdentity.HERO_ID, deckAfterSwitch.heroId)
        assertEquals(20, deckAfterSwitch.cardIds.size)
        assertEquals(initialCardIds, deckAfterSwitch.cardIds)
    }
}
