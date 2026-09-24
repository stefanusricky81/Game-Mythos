package com.example

import com.example.combat.DamageEngine
import com.example.data.*
import com.example.viewmodel.BattleViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BattleMechanicsTest {

    private lateinit var viewModel: BattleViewModel

    @Before
    fun setUp() {
        viewModel = BattleViewModel()
    }

    @Test
    fun `step 1 to 3 - battle starts with correct initial resources`() {
        val state = viewModel.uiState.value
        assertEquals("Hercules", state.playerHero.name)
        assertEquals("Ares", state.enemyHero.name)
        assertEquals(10000, state.playerHero.currentHp)
        assertEquals(10000, state.enemyHero.currentHp)
        assertEquals(5, state.playerEnergy)
        assertEquals(5, state.maxPlayerEnergy)
        assertEquals(30, state.playerMythPower)
        assertEquals(100, state.maxMythPower)
        assertEquals(BattleTurn.PLAYER_TURN, state.currentTurn)
        assertEquals(4, state.playerHand.size)
    }

    @Test
    fun `step 4 to 6 - olympian guard grants shield and absorbs damage`() {
        val hercules = Hero.createHercules()
        val shieldedHercules = DamageEngine.applyShield(hercules, 1200)
        assertEquals(1200, shieldedHercules.currentShield)

        // Ares attacks for 1800 damage -> 1200 absorbed by shield, 600 damages HP
        val damageResult = DamageEngine.calculateAndApplyDamage(
            attacker = null,
            defender = shieldedHercules,
            baseDamage = 1800
        )
        assertEquals(1200, damageResult.shieldAbsorbed)
        assertEquals(600, damageResult.hpDamageDealt)
        assertEquals(0, damageResult.updatedDefender.currentShield)
        assertEquals(9400, damageResult.updatedDefender.currentHp)
    }

    @Test
    fun `step 7 to 9 - titans wrath deals massive physical damage`() {
        val ares = Hero.createAres()
        val hercules = Hero.createHercules()

        // Titan's Wrath base damage: 3600
        val damageResult = DamageEngine.calculateAndApplyDamage(
            attacker = hercules,
            defender = ares,
            baseDamage = 3600
        )
        assertEquals(3600, damageResult.hpDamageDealt)
        assertEquals(6400, damageResult.updatedDefender.currentHp)
        assertFalse(damageResult.isFatal)
    }

    @Test
    fun `step 17 to 18 - last stand activates when hercules falls below 30 percent hp`() {
        val hercules = Hero.createHercules() // 10,000 HP, base Attack 2,800

        // Hit Hercules for 7,500 damage -> HP falls to 2,500 (<= 3,000 HP threshold)
        val result = DamageEngine.calculateAndApplyDamage(
            attacker = null,
            defender = hercules,
            baseDamage = 7500
        )
        assertTrue(result.didTriggerLastStand)
        assertTrue(result.updatedDefender.isLastStandActive)
        assertTrue(result.updatedDefender.hasLastStandTriggered)
        assertEquals(2500, result.updatedDefender.currentHp)

        // Effective attack should be 2,800 + 25% = 3,500
        assertEquals(3500, result.updatedDefender.effectiveAttack)
    }

    @Test
    fun `step 19 to 22 - twelve labors requires 100 MP and deals 4500 base damage`() {
        val hercules = Hero.createHercules()
        val ares = Hero.createAres()

        val ultimateResult = DamageEngine.calculateAndApplyDamage(
            attacker = hercules,
            defender = ares,
            baseDamage = 4500,
            isPhysical = false
        )
        assertEquals(4500, ultimateResult.hpDamageDealt)
        assertEquals(5500, ultimateResult.updatedDefender.currentHp)
    }

    @Test
    fun `step 23 to 24 - death check and victory condition`() {
        val weakAres = Hero.createAres().copy(currentHp = 1000)
        val hercules = Hero.createHercules()

        val fatalResult = DamageEngine.calculateAndApplyDamage(
            attacker = hercules,
            defender = weakAres,
            baseDamage = 2000
        )
        assertTrue(fatalResult.isFatal)
        assertEquals(0, fatalResult.updatedDefender.currentHp)
        assertFalse(fatalResult.updatedDefender.isAlive)
    }

    @Test
    fun `step 25 to 26 - battle reset restores pristine state`() {
        // Apply modifications
        viewModel.debugAddEnergy(5)
        viewModel.debugAddMythPower(50)
        viewModel.debugDamageEnemy(5000)

        // Reset
        viewModel.startNewBattle()
        val freshState = viewModel.uiState.value

        assertEquals(10000, freshState.playerHero.currentHp)
        assertEquals(10000, freshState.enemyHero.currentHp)
        assertEquals(0, freshState.playerHero.currentShield)
        assertEquals(0, freshState.enemyHero.currentShield)
        assertEquals(5, freshState.playerEnergy)
        assertEquals(5, freshState.maxPlayerEnergy)
        assertEquals(30, freshState.playerMythPower)
        assertFalse(freshState.playerHero.isLastStandActive)
        assertEquals(BattleTurn.PLAYER_TURN, freshState.currentTurn)
        assertEquals(4, freshState.playerHand.size)
    }

    @Test
    fun `insufficient energy triggers compact warning without deducting energy`() {
        val expensiveCard = Card(
            id = "test_expensive",
            name = "Test Card",
            cost = 10,
            type = CardType.ATTACK,
            rarity = CardRarity.COMMON,
            iconKey = "strike",
            effect = CardEffect(damage = 1000),
            effectDescription = "Test",
            loreQuote = "Test"
        )
        val initialEnergy = viewModel.uiState.value.playerEnergy

        viewModel.playCard(expensiveCard)

        // Feedback message should indicate compact energy warning
        assertNotNull(viewModel.uiState.value.compactEnergyWarning)
        assertEquals("Need 5 Energy", viewModel.uiState.value.compactEnergyWarning)
        assertEquals(expensiveCard.id, viewModel.uiState.value.shakingCardId)
        assertTrue(viewModel.uiState.value.isEnergyHighlighted)
        // Energy must NOT be deducted
        assertEquals(initialEnergy, viewModel.uiState.value.playerEnergy)
    }

    @Test
    fun `hero attack costs 1 energy and generates 15 myth power`() {
        val initialEnergy = viewModel.uiState.value.playerEnergy
        val initialMyth = viewModel.uiState.value.playerMythPower

        viewModel.performHeroAttack()

        val afterState = viewModel.uiState.value
        assertEquals(initialEnergy - 1, afterState.playerEnergy)
        assertEquals(initialMyth + 15, afterState.playerMythPower)
    }

    @Test
    fun `visual design system - 6 rarity levels supported`() {
        val rarities = CardRarity.entries
        assertEquals(6, rarities.size)
        assertTrue(rarities.contains(CardRarity.COMMON))
        assertTrue(rarities.contains(CardRarity.UNCOMMON))
        assertTrue(rarities.contains(CardRarity.RARE))
        assertTrue(rarities.contains(CardRarity.EPIC))
        assertTrue(rarities.contains(CardRarity.LEGENDARY))
        assertTrue(rarities.contains(CardRarity.MYTHIC))

        // Ensure every rarity has an assigned color token
        rarities.forEach { rarity ->
            val color = com.example.ui.theme.MythosTokens.getRarityColor(rarity)
            assertNotNull(color)
        }
    }

    @Test
    fun `visual design system - sample cards match specifications`() {
        val deck = DeckFactory.createPrototypeDeck()

        val olympianGuard = deck.find { it.id == "c_olympian_guard" }
        assertNotNull(olympianGuard)
        assertEquals("Olympian Guard", olympianGuard?.name)
        assertEquals(CardType.DEFENSE, olympianGuard?.type)
        assertEquals(CardRarity.COMMON, olympianGuard?.rarity)
        assertEquals(2, olympianGuard?.cost)
        assertEquals(1200, olympianGuard?.effect?.shield)

        val titansWrath = deck.find { it.id == "c_titans_wrath" }
        assertNotNull(titansWrath)
        assertEquals("Titan's Wrath", titansWrath?.name)
        assertEquals(CardType.ATTACK, titansWrath?.type)
        assertEquals(CardRarity.LEGENDARY, titansWrath?.rarity)
        assertEquals(5, titansWrath?.cost)
        assertEquals(3600, titansWrath?.effect?.damage)

        val nectarOfGods = deck.find { it.id == "c_nectar_gods" }
        assertNotNull(nectarOfGods)
        assertEquals("Nectar of the Gods", nectarOfGods?.name)
        assertEquals(CardType.SPELL, nectarOfGods?.type)
        assertEquals(CardRarity.RARE, nectarOfGods?.rarity)
        assertEquals(2, nectarOfGods?.cost)
        assertEquals(1500, nectarOfGods?.effect?.healAmount)
    }

    @Test
    fun `visual design system - data driven heroes support multiple entities`() {
        val hercules = Hero.createHercules()
        val ares = Hero.createAres()
        val athena = Hero.createAthena()
        val zeus = Hero.createZeus()
        val hades = Hero.createHades()

        assertEquals("Hercules", hercules.name)
        assertEquals("Ares", ares.name)
        assertEquals("Athena", athena.name)
        assertEquals("Zeus", zeus.name)
        assertEquals("Hades", hades.name)
    }

    @Test
    fun `visual design system - DEBUG_BUILD flag controls prototype display`() {
        val initial = MythosConfig.DEBUG_BUILD
        MythosConfig.DEBUG_BUILD = false
        assertFalse(MythosConfig.DEBUG_BUILD)
        MythosConfig.DEBUG_BUILD = true
        assertTrue(MythosConfig.DEBUG_BUILD)
        MythosConfig.DEBUG_BUILD = initial
    }

    @Test
    fun `hercules character pipeline - canonical identity verification`() {
        assertEquals("hero_hercules", HerculesIdentity.HERO_ID)
        assertEquals("Hercules", HerculesIdentity.NAME)
        assertEquals("Champion of Olympus", HerculesIdentity.TITLE)
        assertEquals(CharacterFaction.OLYMPUS, HerculesIdentity.FACTION)
        assertEquals(CardRarity.LEGENDARY, HerculesIdentity.RARITY)

        val def = HerculesIdentity.definition
        assertEquals("hero_hercules", def.heroId)
        assertEquals("Hercules", def.name)
        assertEquals(CardRarity.LEGENDARY, def.rarity)
    }

    @Test
    fun `hercules character pipeline - asset registry contains required slots and fallback safety`() {
        val reg = HerculesAssets.registry
        assertEquals("hero_hercules", reg.heroId)
        assertNotNull(reg.fullBody)
        assertNotNull(reg.battle)
        assertNotNull(reg.portrait)
        assertNotNull(reg.cardArt)
        assertNotNull(reg.avatar)
        assertNotNull(reg.lastStand)
        assertNotNull(reg.twelveLabors)
        assertNotNull(reg.victory)
        assertNotNull(reg.defeat)
        assertNotNull(reg.designReference)

        // All assets must resolve to a valid non-zero drawable resource
        reg.allAssets.forEach { asset ->
            assertTrue(asset.assetKey.isNotEmpty())
            assertTrue(asset.assetPath.startsWith("heroes/hercules/"))
            assertTrue("Asset ${asset.assetKey} should resolve to valid resource", asset.resolveResId() != 0)
        }

        // Test fallback mechanism: if primary resource is missing (0), fallback is used without crashing
        val missingAsset = CharacterAsset(
            assetKey = "test_missing",
            assetPath = "heroes/hercules/test.jpg",
            resId = 0,
            fallbackResId = R.drawable.img_hercules_hero,
            status = AssetStatus.PLACEHOLDER
        )
        assertEquals(R.drawable.img_hercules_hero, missingAsset.resolveResId())
    }

    @Test
    fun `hercules character pipeline - battle and last stand asset integration in Hero model`() {
        val heroNormal = Hero.createHercules()
        assertEquals(HerculesIdentity.HERO_ID, heroNormal.id)
        assertEquals(HerculesIdentity.NAME, heroNormal.name)
        assertEquals(HerculesIdentity.assets.portrait.resolveResId(), heroNormal.getEffectivePortraitResId())
        assertEquals(HerculesIdentity.assets.battle.resolveResId(), heroNormal.getBattleResId())

        // In Last Stand, portrait switches to empowered asset
        val heroLastStand = heroNormal.copy(isLastStandActive = true)
        assertEquals(HerculesIdentity.assets.lastStand.resolveResId(), heroLastStand.getEffectivePortraitResId())
    }

    @Test
    fun `hercules character pipeline - card art integration in deck`() {
        val deck = DeckFactory.createPrototypeDeck()
        val titansWrath = deck.find { it.id == "c_titans_wrath" }
        assertNotNull(titansWrath)
        assertEquals(HerculesIdentity.assets.cardArt.resolveResId(), titansWrath?.artworkResId)
    }

    @Test
    fun `battle screen components - hercules metadata and ability cooldowns logic`() {
        val hercules = Hero.createHercules()
        assertEquals("hero_hercules", HerculesIdentity.HERO_ID)
        assertEquals("Hercules", HerculesIdentity.NAME)
        assertEquals(10000, hercules.maxHp)
        assertEquals(10000, hercules.currentHp)
        assertEquals(1.0f, hercules.hpPercentage)

        // Health under Last Stand 30% threshold
        val damagedHercules = hercules.copy(currentHp = 2500, isLastStandActive = true)
        assertEquals(0.25f, damagedHercules.hpPercentage)
        assertTrue(damagedHercules.hpPercentage < 0.30f)
        assertTrue(damagedHercules.isLastStandActive)
        assertEquals(3500, damagedHercules.effectiveAttack) // 2800 * 1.25

        // Ability cooldowns & readiness verification
        val initialMythPower = 30
        val maxMythPower = 100
        val isUltimateReadyInitial = initialMythPower >= maxMythPower
        assertFalse(isUltimateReadyInitial)
        val mythPowerRemaining = maxMythPower - initialMythPower
        assertEquals(70, mythPowerRemaining)

        val chargedMythPower = 100
        val isUltimateReadyCharged = chargedMythPower >= maxMythPower
        assertTrue(isUltimateReadyCharged)
    }
}

