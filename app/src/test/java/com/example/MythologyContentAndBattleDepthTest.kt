package com.example

import com.example.ai.AiAction
import com.example.ai.EnemyAi
import com.example.combat.*
import com.example.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MythologyContentAndBattleDepthTest {

    // =========================================================================
    // 1. HERO EXPANSION & BALANCE TESTS (Requirement #1, #2)
    // =========================================================================

    @Test
    fun test01_expandedHeroRosterCompleteness() {
        val requiredIds = listOf(
            HerculesIdentity.HERO_ID,
            "hero_zeus",
            "hero_athena",
            "hero_ares",
            "hero_medusa",
            "hero_hades",
            "hero_thor",
            "hero_loki",
            "hero_anubis",
            "hero_achilles",
            "hero_merlin"
        )

        for (id in requiredIds) {
            val hero = HeroCatalog.findHero(id)
            assertNotNull("Hero with id '$id' must exist in HeroCatalog", hero)
            assertTrue("Hero name must not be blank", hero!!.name.isNotBlank())
            assertTrue("Hero title must not be blank", hero.title.isNotBlank())
            assertTrue("Hero faction must not be blank", hero.faction.isNotBlank())
            assertTrue("Hero baseHp must be > 0", hero.baseHp > 0)
            assertTrue("Hero baseAttack must be > 0", hero.baseAttack > 0)
            assertTrue("Hero baseDefense must be > 0", hero.baseDefense > 0)
            assertTrue("Hero passiveName must not be blank", hero.passiveName.isNotBlank())
            assertTrue("Hero ultimateName must not be blank", hero.ultimateName.isNotBlank())
            assertTrue("Hero combatIdentity must not be blank", hero.combatIdentity.isNotBlank())
            assertTrue("Hero unlockCondition must not be blank", hero.unlockCondition.isNotBlank())
        }

        assertEquals("Hero selection must contain at least 11 heroes", 11, HeroCatalog.SELECTION_HEROES.size)
    }

    @Test
    fun test02_heroRoleAndGameplayIdentities() {
        val zeus = HeroCatalog.findHero("hero_zeus")!!
        assertEquals("Burst / Control", zeus.role)
        assertEquals("THUNDER SOVEREIGNTY", zeus.passiveName)
        assertEquals("WRATH OF OLYMPUS", zeus.ultimateName)

        val athena = HeroCatalog.findHero("hero_athena")!!
        assertEquals("Defense / Control", athena.role)
        assertEquals("DIVINE STRATEGY", athena.passiveName)
        assertEquals("AEGIS OF ATHENA", athena.ultimateName)

        val ares = HeroCatalog.findHero("hero_ares")!!
        assertEquals("Aggressive Damage", ares.role)
        assertEquals("BLOODLUST", ares.passiveName)
        assertEquals("GOD OF WAR", ares.ultimateName)

        val medusa = HeroCatalog.findHero("hero_medusa")!!
        assertEquals("Control / Debuff", medusa.role)
        assertEquals("PETRIFYING GAZE", medusa.passiveName)
        assertEquals("STONE CURSE", medusa.ultimateName)

        val hades = HeroCatalog.findHero("hero_hades")!!
        assertEquals("Drain / Sustain", hades.role)
        assertEquals("LORD OF THE UNDERWORLD", hades.passiveName)
        assertEquals("UNDERWORLD JUDGMENT", hades.ultimateName)

        val thor = HeroCatalog.findHero("hero_thor")!!
        assertEquals("Heavy Burst", thor.role)
        assertEquals("THUNDERBORN", thor.passiveName)
        assertEquals("MJOLNIR'S WRATH", thor.ultimateName)

        val loki = HeroCatalog.findHero("hero_loki")!!
        assertEquals("Trick / Control", loki.role)
        assertEquals("DECEPTION", loki.passiveName)
        assertEquals("CHAOS UNLEASHED", loki.ultimateName)

        val anubis = HeroCatalog.findHero("hero_anubis")!!
        assertEquals("Execution / Death Control", anubis.role)
        assertEquals("WEIGHER OF SOULS", anubis.passiveName)
        assertEquals("JUDGMENT OF THE DEAD", anubis.ultimateName)
    }

    @Test
    fun test03_heroStatProgressionCompatibility() {
        val zeusDef = HeroCatalog.findHero("hero_zeus")!!
        val battleZeusL1 = zeusDef.toBattleHero(level = 1)
        val battleZeusL5 = zeusDef.toBattleHero(level = 5)

        assertEquals(10500, battleZeusL1.maxHp)
        assertEquals(3500, battleZeusL1.baseAttack)
        assertEquals(2200, battleZeusL1.baseDefense)

        // At level 5, multiplier is 1.60x
        assertTrue("Level 5 HP must scale higher than Level 1", battleZeusL5.maxHp > battleZeusL1.maxHp)
        assertTrue("Level 5 ATK must scale higher than Level 1", battleZeusL5.baseAttack > battleZeusL1.baseAttack)
    }

    // =========================================================================
    // 2. CARD EXPANSION TESTS (Requirement #3)
    // =========================================================================

    @Test
    fun test04_cardExpansionCountAndTypes() {
        val newCards = CardCatalogExpansion.NEW_CARDS
        assertTrue("Card expansion must have at least 30 new cards", newCards.size >= 30)
        assertEquals("Expansion contains exactly 32 configured cards", 32, newCards.size)

        val allCards = CardCatalog.ALL_CARDS
        assertTrue("Master catalog must contain 50+ total cards", allCards.size >= 50)

        // Verify required card categories exist in expansion
        val godCards = newCards.filter { it.type == CardType.GOD }
        val relicCards = newCards.filter { it.type == CardType.RELIC }
        val spellCards = newCards.filter { it.type == CardType.SPELL }
        val trapCards = newCards.filter { it.type == CardType.TRAP }
        val summonCards = newCards.filter { it.type == CardType.SUMMON }
        val mythicRarity = newCards.filter { it.rarity == CardRarity.MYTHIC }

        assertTrue("GOD cards must exist", godCards.isNotEmpty())
        assertTrue("RELIC cards must exist", relicCards.isNotEmpty())
        assertTrue("SPELL cards must exist", spellCards.isNotEmpty())
        assertTrue("TRAP cards must exist", trapCards.isNotEmpty())
        assertTrue("SUMMON cards must exist", summonCards.isNotEmpty())
        assertTrue("MYTHIC cards must exist", mythicRarity.isNotEmpty())
    }

    @Test
    fun test05_newCardsProgressionIntegrity() {
        for (cardDef in CardCatalogExpansion.NEW_CARDS) {
            assertEquals("Card ${cardDef.name} must have 5 progression levels", 5, cardDef.progression.size)
            val level1 = cardDef.getCard(1)
            val level5 = cardDef.getCard(5)
            assertNotNull(level1)
            assertNotNull(level5)
            assertTrue("Card lore quote must not be blank", cardDef.loreQuote.isNotBlank())
        }
    }

    // =========================================================================
    // 3. STATUS EFFECT SYSTEM & COMBAT MECHANICS (Requirement #4, #5)
    // =========================================================================

    @Test
    fun test06_statusEffectModelAndTiming() {
        val poison = StatusEffectCatalog.createPoison(turns = 3, damagePerTurn = 400)
        assertEquals(StatusEffectType.POISON, poison.type)
        assertEquals(3, poison.durationTurns)
        assertEquals(400, poison.magnitude)
        assertEquals(StatusTiming.TURN_END, poison.timing)

        val burn = StatusEffectCatalog.createBurn(turns = 2, damagePerTurn = 300)
        assertEquals(StatusEffectType.BURN, burn.type)
        assertEquals(StatusTiming.TURN_START, burn.timing)

        val stun = StatusEffectCatalog.createStun(1)
        assertEquals(StatusEffectType.STUN, stun.type)
        assertTrue(stun.type.isDebuff)

        val vuln = StatusEffectCatalog.createVulnerable(2)
        assertEquals(StatusEffectType.VULNERABLE, vuln.type)

        val weaken = StatusEffectCatalog.createWeaken(2)
        assertEquals(StatusEffectType.WEAKEN, weaken.type)

        val regen = StatusEffectCatalog.createRegeneration(3, 500)
        assertEquals(StatusEffectType.REGENERATION, regen.type)
        assertFalse(regen.type.isDebuff)
    }

    @Test
    fun test07_vulnerableAmplifiesIncomingDamage() {
        val baseHero = Hero.createAres()
        val vulnHero = DamageEngine.applyStatusEffect(
            baseHero,
            StatusEffectCatalog.createVulnerable(2, 30)
        )
        assertTrue(vulnHero.isVulnerable)

        val normalResult = DamageEngine.calculateAndApplyDamage(null, baseHero, 2000)
        val vulnResult = DamageEngine.calculateAndApplyDamage(null, vulnHero, 2000)

        // +30% amplified damage: 2000 * 1.30 = 2600
        assertEquals(2000, normalResult.modifiedDamage)
        assertEquals(2600, vulnResult.modifiedDamage)
        assertTrue(vulnResult.wasVulnerableHit)
    }

    @Test
    fun test08_weakenReducesAttackerDamage() {
        val baseAttacker = Hero.createHercules()
        val weakenedAttacker = DamageEngine.applyStatusEffect(
            baseAttacker,
            StatusEffectCatalog.createWeaken(2, 25)
        )
        assertTrue(weakenedAttacker.isWeakened)

        val defender = Hero.createAres()
        val normalResult = DamageEngine.calculateAndApplyDamage(baseAttacker, defender, 1000)
        val weakResult = DamageEngine.calculateAndApplyDamage(weakenedAttacker, defender, 1000)

        assertTrue("Weakened attacker deals less damage", weakResult.modifiedDamage < normalResult.modifiedDamage)
    }

    @Test
    fun test09_turnStartAndTurnEndStatusResolution() {
        var hero = Hero(
            id = "test_hero",
            name = "Test",
            title = "Test",
            currentHp = 5000,
            maxHp = 10000,
            baseAttack = 2000,
            baseDefense = 2000
        )

        // Attach Burn and Regeneration
        hero = DamageEngine.applyStatusEffect(hero, StatusEffectCatalog.createBurn(2, 500))
        hero = DamageEngine.applyStatusEffect(hero, StatusEffectCatalog.createRegeneration(2, 800))

        // Resolve Turn Start: -500 burn, +800 regen -> net +300 HP
        val (startHero, floatingStart) = DamageEngine.resolveTurnStartStatuses(hero, isEnemy = false)
        assertEquals(5300, startHero.currentHp)
        assertEquals(2, floatingStart.size)

        // Attach Poison and resolve Turn End: -400 poison
        val poisonedHero = DamageEngine.applyStatusEffect(startHero, StatusEffectCatalog.createPoison(2, 400))
        val (endHero, floatingEnd) = DamageEngine.resolveTurnEndStatuses(poisonedHero, isEnemy = false)
        assertEquals(4900, endHero.currentHp)
        assertEquals(1, floatingEnd.size)
    }

    @Test
    fun test10_cleanseDebuffsRemovesOnlyNegativeEffects() {
        var hero = Hero.createHercules()
        hero = DamageEngine.applyStatusEffect(hero, StatusEffectCatalog.createPoison(2, 300))
        hero = DamageEngine.applyStatusEffect(hero, StatusEffectCatalog.createBurn(2, 200))
        hero = DamageEngine.applyStatusEffect(hero, StatusEffectCatalog.createRegeneration(3, 500))

        assertEquals(3, hero.statusEffects.size)
        val cleansed = DamageEngine.cleanseDebuffs(hero)

        assertEquals("Only positive buff (Regeneration) should remain", 1, cleansed.statusEffects.size)
        assertEquals(StatusEffectType.REGENERATION, cleansed.statusEffects.first().type)
        assertEquals(0, cleansed.poisonTurnsRemaining)
    }

    // =========================================================================
    // 4. ENEMY VARIETY & BOSS FRAMEWORK TESTS (Requirement #6, #7)
    // =========================================================================

    @Test
    fun test11_enemyVarietyCatalogCompleteness() {
        val requiredEnemies = listOf(
            "enemy_minotaur",
            "enemy_hydra",
            "enemy_medusa",
            "enemy_cerberus",
            "enemy_cyclops",
            "enemy_spartan_champion",
            "enemy_valkyrie",
            "enemy_frost_giant",
            "enemy_egyptian_guardian"
        )

        for (enemyId in requiredEnemies) {
            val enemy = EnemyCatalog.findEnemy(enemyId)
            assertNotNull("Enemy $enemyId must exist in EnemyCatalog", enemy)
            assertTrue(enemy!!.hp > 0)
            assertTrue(enemy.attack > 0)
            assertTrue(enemy.defense > 0)
            assertTrue(enemy.specialAbilityName.isNotBlank())
        }
    }

    @Test
    fun test12_enemyAiBehaviorDifferentiation() {
        val minotaur = EnemyCatalog.MINOTAUR.toBattleHero()
        val player = Hero.createHercules()

        // Minotaur aggressive AI
        val actions = EnemyAi.decideTurnActions(minotaur, player, availableEnergy = 4)
        assertTrue("Minotaur AI must generate actions", actions.isNotEmpty())

        // Medusa control AI
        val medusa = EnemyCatalog.MEDUSA.toBattleHero()
        val medusaActions = EnemyAi.decideTurnActions(medusa, player, availableEnergy = 4)
        assertTrue("Medusa AI must generate actions", medusaActions.isNotEmpty())

        // Boss Phase 2 Special Ability Trigger
        val bossActions = EnemyAi.decideTurnActions(minotaur, player, availableEnergy = 4, isBossPhase2 = true)
        assertTrue("Boss in Phase 2 should unleash BossSpecialAbility", bossActions.any { it is AiAction.BossSpecialAbility })
    }

    @Test
    fun test13_bossCatalogAndPhaseDefinitions() {
        val minotaurKing = BossCatalog.MINOTAUR_KING
        assertEquals(2, minotaurKing.phases.size)
        assertEquals(1.0f, minotaurKing.phases[0].healthThresholdPercent, 0.01f)
        assertEquals(0.50f, minotaurKing.phases[1].healthThresholdPercent, 0.01f)
        assertEquals(40, minotaurKing.phases[1].attackBonusPercent)
        assertTrue(minotaurKing.phases[1].bannerText.contains("ENRAGED"))

        val hydra = BossCatalog.LERNAEAN_HYDRA
        assertEquals(2, hydra.phases.size)
        assertTrue(hydra.phases[1].bannerText.contains("REGROWTH"))

        val medusaQueen = BossCatalog.MEDUSA_QUEEN
        assertEquals(2, medusaQueen.phases.size)
        assertTrue(medusaQueen.phases[1].bannerText.contains("PETRIFYING"))
    }

    // =========================================================================
    // 5. CAMPAIGN WORLDS & STAGES EXPANSION (Requirement #8)
    // =========================================================================

    @Test
    fun test14_campaignWorldProgressionCompleteness() {
        val allWorlds = CampaignCatalog.ALL_WORLDS
        assertEquals("Campaign must contain 5 structured worlds", 5, allWorlds.size)

        val worldIds = allWorlds.map { it.worldId }
        assertTrue(worldIds.contains("world_olympus"))
        assertTrue(worldIds.contains("world_valhalla"))
        assertTrue(worldIds.contains("world_egypt"))
        assertTrue(worldIds.contains("world_underworld"))
        assertTrue(worldIds.contains("world_celestial"))

        for (w in allWorlds) {
            assertEquals("Each world must have 5 stages", 5, w.stages.size)
            assertTrue("World must have a boss in stage 5", w.stages.last().isBoss)
        }
    }

    @Test
    fun test15_world2AndWorld3StageDefinitions() {
        // World 2: Valhalla stages
        for (i in 1..5) {
            val stage = CampaignCatalog.findStage("stage_2_$i")
            assertNotNull("Stage 2-$i must exist", stage)
            assertEquals("world_valhalla", stage!!.worldId)
            assertTrue(stage.recommendedPower > 0)
            assertTrue(stage.goldReward > 0)
        }

        // World 3: Egypt stages
        for (i in 1..5) {
            val stage = CampaignCatalog.findStage("stage_3_$i")
            assertNotNull("Stage 3-$i must exist", stage)
            assertEquals("world_egypt", stage!!.worldId)
            assertTrue(stage.recommendedPower > 0)
            assertTrue(stage.goldReward > 0)
        }
    }

    // =========================================================================
    // 6. DECK ANALYSIS & SYNERGIES (Requirement #9, #10)
    // =========================================================================

    @Test
    fun test16_deckAnalysisAndSynergyCalculations() {
        val hercules = HeroCatalog.HERCULES
        val sampleCards = listOf(
            CardCatalog.getCard("c_olympian_guard", 1),
            CardCatalog.getCard("c_olympian_guard", 1),
            CardCatalog.getCard("c_power_strike", 1),
            CardCatalog.getCard("c_heroic_rage", 1),
            CardCatalog.getCard("c_spartan_phalanx", 1),
            CardCatalog.getCard("c_titans_wrath", 1),
            CardCatalog.getCard("c_zeus_thunderstone", 1)
        )

        val analysis = SynergyCatalog.analyzeDeck(sampleCards, hercules)
        assertTrue("Deck power must be calculated > 0", analysis.deckPower > 0)
        assertTrue("Average energy cost must be > 0", analysis.averageEnergyCost > 0)
        assertEquals(7, analysis.totalCards)
        assertTrue(analysis.typeDistribution.isNotEmpty())
        assertTrue(analysis.availableSynergies.isNotEmpty())
    }

    @Test
    fun test17_stormAndWarSynergiesActivation() {
        val zeus = HeroCatalog.ZEUS
        val stormCards = listOf(
            CardCatalog.getCard("c_zeus_thunderbolt", 1),
            CardCatalog.getCard("c_zeus_storm_olympus", 1),
            CardCatalog.getCard("c_thor_thunder_strike", 1)
        )

        val analysis = SynergyCatalog.analyzeDeck(stormCards, zeus)
        val stormSynergy = analysis.activeSynergies.find { it.id == SynergyCatalog.SYNERGY_STORM }
        assertNotNull("Tempest Wrath synergy should activate with 3+ storm cards", stormSynergy)
        assertTrue(stormSynergy!!.isActive)
    }

    // =========================================================================
    // 7. DECK INTEGRITY PRESERVATION
    // =========================================================================

    @Test
    fun test18_activeDeckRemainsExactly20Cards() {
        val prototypeDeck = DeckFactory.createPrototypeDeck()
        assertEquals("Active prototype deck must contain exactly 20 cards", 20, prototypeDeck.size)
        assertEquals("Hercules default deck must contain exactly 20 cards", 20, HeroCatalog.HERCULES.defaultDeckCardIds.size)
    }
}
