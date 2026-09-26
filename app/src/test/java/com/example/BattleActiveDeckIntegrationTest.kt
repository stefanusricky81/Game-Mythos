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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BattleActiveDeckIntegrationTest {

    private lateinit var viewModel: BattleViewModel

    @Before
    fun setUp() {
        viewModel = BattleViewModel()
    }

    /**
     * Test A: Valid ActiveDeck loads into Battle.
     * The 20-card ActiveDeck resolves via CardCatalog and creates the exact 20-card battle pool.
     */
    @Test
    fun testValidActiveDeckLoadsIntoBattle() {
        val defaultDeck = ActiveDeck.createDefaultOlympusDeck()
        val success = viewModel.startNewBattle(defaultDeck)

        assertTrue("Battle should successfully start with valid 20-card deck", success)
        val state = viewModel.uiState.value
        assertFalse("Deck should not be marked invalid", state.isDeckInvalid)
        assertNull("Validation error should be null", state.deckValidationResult)

        // Hand + Draw Pile must equal exactly 20 cards
        val totalBattleCards = state.playerHand.size + state.playerDrawPile.size
        assertEquals(20, totalBattleCards)
        assertEquals(4, state.playerHand.size)
        assertEquals(16, state.playerDrawPile.size)
        assertEquals(0, state.playerDiscardPile.size)

        // All cards in combat pool must originate from the ActiveDeck
        val allCombatCardIds = (state.playerHand + state.playerDrawPile).map { it.id }.sorted()
        val expectedCardIds = defaultDeck.cardIds.sorted()
        assertEquals(expectedCardIds, allCombatCardIds)
    }

    /**
     * Test B: Card replacement updates the battle deck.
     * Replacing a card in ActiveDeck changes the cards present in the subsequent battle.
     */
    @Test
    fun testCardReplacementUpdatesBattleDeck() {
        // Create custom deck swapping c_power_strike for c_divine_aegis (which is owned and has 0 copies in default deck)
        val modifiedCardIds = ActiveDeck.createDefaultOlympusDeck().cardIds.toMutableList()
        val removeIndex = modifiedCardIds.indexOf("c_power_strike")
        assertTrue(removeIndex != -1)
        modifiedCardIds[removeIndex] = "c_divine_aegis"

        val customDeck = ActiveDeck(
            deckId = "custom_test_deck",
            name = "Aegis & Olympus",
            cardIds = modifiedCardIds,
            heroId = "hero_hercules"
        )

        val success = viewModel.startNewBattle(customDeck)
        assertTrue("Custom deck should be valid and start battle", success)

        val state = viewModel.uiState.value
        val allCards = state.playerHand + state.playerDrawPile

        // c_divine_aegis must now be present in the battle card pool
        val aegisCount = allCards.count { it.id == "c_divine_aegis" }
        assertEquals(1, aegisCount)

        // c_power_strike was 2 copies, now should be exactly 1 copy
        val strikeCount = allCards.count { it.id == "c_power_strike" }
        assertEquals(1, strikeCount)
    }

    /**
     * Test C: Card level upgrades carry into battle.
     * Upgraded cards in ActiveDeck resolve using their progression data and effect stats.
     */
    @Test
    fun testCardLevelUpgradesCarryIntoBattle() {
        val repo = PlayerEconomyRepository.instance
        // Simulate Titan's Wrath and Olympian Guard upgraded to Level 3 in player economy
        val stateField = repo.javaClass.getDeclaredField("_economyState")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = stateField.get(repo) as kotlinx.coroutines.flow.MutableStateFlow<com.example.monetization.PlayerEconomyState>
        stateFlow.value = stateFlow.value.copy(
            cardLevels = stateFlow.value.cardLevels + mapOf(
                "c_titans_wrath" to 3,
                "c_olympian_guard" to 3
            )
        )

        val success = viewModel.startNewBattle()
        assertTrue(success)

        val state = viewModel.uiState.value
        val allCombatCards = state.playerHand + state.playerDrawPile

        // Titan's Wrath (1 copy in default deck)
        val titansWrathCards = allCombatCards.filter { it.id == "c_titans_wrath" }
        assertEquals(1, titansWrathCards.size)
        val titanCard = titansWrathCards.first()
        assertEquals("Card level in battle must match Collection level", 3, titanCard.level)
        assertEquals("Level 3 Titan's Wrath deals 4100 damage", 4100, titanCard.effect.damage)

        // Olympian Guard (2 copies in default deck)
        val guardCards = allCombatCards.filter { it.id == "c_olympian_guard" }
        assertEquals(2, guardCards.size)
        guardCards.forEach { card ->
            assertEquals("All copies in battle must inherit Level 3", 3, card.level)
            assertEquals("Level 3 Olympian Guard provides 1500 shield", 1500, card.effect.shield)
        }
    }

    /**
     * Test D: Retry / Battle Again creates a fresh battle state.
     * Starts fresh from ActiveDeck without persisting mid-combat HP or temporary status effects.
     */
    @Test
    fun testRetryCreatesFreshBattleState() {
        viewModel.startNewBattle()

        // Modify state as if combat has transpired
        val stateField = viewModel.javaClass.getDeclaredField("_uiState")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val uiFlow = stateField.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<BattleUiState>
        uiFlow.value = uiFlow.value.copy(
            playerEnergy = 1,
            playerMythPower = 95,
            playerHero = uiFlow.value.playerHero.copy(currentHp = 4200, currentShield = 800),
            enemyHero = uiFlow.value.enemyHero.copy(currentHp = 2100),
            playerDiscardPile = listOf(CardCatalog.getCard("c_power_strike"))
        )

        // Tap Retry / Battle Again
        val restarted = viewModel.startNewBattle()
        assertTrue(restarted)

        val freshState = viewModel.uiState.value
        assertEquals("Player HP should reset to 10000", 10000, freshState.playerHero.currentHp)
        assertEquals("Player Shield should reset to 0", 0, freshState.playerHero.currentShield)
        assertEquals("Enemy HP should reset to 10000", 10000, freshState.enemyHero.currentHp)
        assertEquals("Player Energy should reset to 5", 5, freshState.playerEnergy)
        assertEquals("Player Myth Power should reset to 30", 30, freshState.playerMythPower)
        assertEquals("Hand should have 4 cards", 4, freshState.playerHand.size)
        assertEquals("Draw pile should have 16 cards", 16, freshState.playerDrawPile.size)
        assertEquals("Discard pile should be empty", 0, freshState.playerDiscardPile.size)
    }

    /**
     * Test E: Deck validation blocks invalid deck before battle.
     * When ActiveDeck has fewer than 20 cards, battle is blocked and isDeckInvalid is flagged.
     */
    @Test
    fun testDeckValidationBlocksInvalidDeck() {
        val invalidShortDeck = ActiveDeck(
            deckId = "short_deck",
            name = "Incomplete",
            cardIds = listOf("c_titans_wrath", "c_olympian_guard")
        )

        val success = viewModel.startNewBattle(invalidShortDeck)
        assertFalse("Battle start must return false for incomplete deck", success)

        val state = viewModel.uiState.value
        assertTrue("isDeckInvalid must be true", state.isDeckInvalid)
        assertNotNull("deckValidationResult must be populated", state.deckValidationResult)
        assertFalse(state.deckValidationResult!!.isValid)
        assertEquals(DeckValidationErrorType.TOO_FEW_CARDS, state.deckValidationResult!!.primaryIssueType)
        assertTrue(state.playerHand.isEmpty())
        assertTrue(state.playerDrawPile.isEmpty())
    }

    /**
     * Test F: Duplicate card copies in hand are tracked independently.
     * Playing one copy removes only that single card copy, leaving the other copy in hand.
     */
    @Test
    fun testDuplicateCardCopyRemovalFromHand() {
        val card1 = CardCatalog.getCard("c_olympian_guard", 1).copy(instanceId = "guard_inst_1")
        val card2 = CardCatalog.getCard("c_olympian_guard", 1).copy(instanceId = "guard_inst_2")
        val card3 = CardCatalog.getCard("c_power_strike", 1).copy(instanceId = "strike_inst_1")

        val stateField = viewModel.javaClass.getDeclaredField("_uiState")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val uiFlow = stateField.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<BattleUiState>
        uiFlow.value = uiFlow.value.copy(
            playerEnergy = 5,
            playerHand = listOf(card1, card2, card3),
            currentTurn = BattleTurn.PLAYER_TURN,
            isExecutingTurn = false
        )

        // Play the first instance
        viewModel.playCard(card1)

        val afterPlayState = viewModel.uiState.value
        // Only card1 should be removed; card2 must remain in hand!
        assertFalse("Card 1 should no longer be in hand", afterPlayState.playerHand.any { it.instanceId == "guard_inst_1" })
        assertTrue("Card 2 copy must still remain in hand", afterPlayState.playerHand.any { it.instanceId == "guard_inst_2" })
        assertTrue("Card 3 must still remain in hand", afterPlayState.playerHand.any { it.instanceId == "strike_inst_1" })
        assertEquals(2, afterPlayState.playerHand.size)
    }

    /**
     * Test G: Battle Victory Rewards enter PlayerEconomyRepository.
     * Completes loop: VICTORY -> REWARDS -> COLLECTION.
     */
    @Test
    fun testBattleVictoryRewardsFlowIntoEconomy() {
        val repo = PlayerEconomyRepository.instance
        val initialGold = repo.economyState.value.gold
        val rewards = BattleRewards(gold = 750, xp = 1200, cardRewardName = "Divine Aegis of Olympus")

        repo.claimBattleVictoryRewards(rewards)

        val updatedEconomy = repo.economyState.value
        assertEquals("Gold should increase by 750", initialGold + 750, updatedEconomy.gold)
        assertTrue("Divine Aegis card or shards should be in owned inventory",
            updatedEconomy.ownedCardIds.contains("c_divine_aegis") ||
            (updatedEconomy.cardShards["c_divine_aegis"] ?: 0) > 0
        )
    }

    /**
     * Requirement #8 Regression Test 1:
     * Full battle lifecycle isolation test.
     * 1. Create valid ActiveDeck with exactly 20 cards.
     * 2. Save ActiveDeck.
     * 3. Start Battle.
     * 4. Assert battle runtime contains 20 cards.
     * 5. Play/remove several cards.
     * 6. Assert battle runtime changes.
     * 7. Assert persistent ActiveDeck STILL contains exactly 20 cards.
     * 8. Finish Victory.
     * 9. Assert persistent ActiveDeck STILL contains exactly 20 cards.
     * 10. Reload repository/persistence.
     * 11. Assert ActiveDeck STILL contains exactly 20 cards.
     */
    @Test
    fun testActiveDeckImmutabilityAcrossBattleVictoryAndPersistenceReload() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = PlayerEconomyRepository.instance
        repo.initPersistence(context)

        // 1. Create valid ActiveDeck with exactly 20 cards
        val initialDeck = ActiveDeck.createDefaultOlympusDeck()
        assertEquals(20, initialDeck.cardIds.size)

        // 2. Save ActiveDeck
        val saveResult = repo.saveActiveDeck(initialDeck)
        assertTrue("Saved deck must be valid", saveResult.isValid)
        assertEquals(20, repo.economyState.value.activeDeck.cardIds.size)

        // 3. Start Battle
        val started = viewModel.startNewBattle()
        assertTrue("Battle must start successfully", started)

        // 4. Assert battle runtime contains 20 cards (4 in hand + 16 in draw pile)
        val initialRuntimeTotal = viewModel.uiState.value.playerHand.size + viewModel.uiState.value.playerDrawPile.size
        assertEquals("Battle runtime must contain exactly 20 cards", 20, initialRuntimeTotal)
        assertEquals(4, viewModel.uiState.value.playerHand.size)
        assertEquals(16, viewModel.uiState.value.playerDrawPile.size)
        assertEquals(0, viewModel.uiState.value.playerDiscardPile.size)

        // 5. Play/remove several cards from hand
        val cardToPlay1 = viewModel.uiState.value.playerHand[0]
        viewModel.playCard(cardToPlay1)
        val cardToPlay2 = viewModel.uiState.value.playerHand.firstOrNull { it.cost <= viewModel.uiState.value.playerEnergy }
        if (cardToPlay2 != null) {
            viewModel.playCard(cardToPlay2)
        }

        // 6. Assert battle runtime changes (hand loses card, energy deducted)
        val afterCombatState = viewModel.uiState.value
        assertTrue("Hand size must have decreased after playing card", afterCombatState.playerHand.size < 4)
        assertFalse("Played card should no longer be in hand", afterCombatState.playerHand.any { it.instanceId == cardToPlay1.instanceId })

        // 7. Assert persistent ActiveDeck STILL contains exactly 20 cards
        val persistentDeckDuringBattle = repo.economyState.value.activeDeck
        assertEquals("Persistent ActiveDeck must STILL have exactly 20 cards during combat", 20, persistentDeckDuringBattle.cardIds.size)
        val validationDuringBattle = DeckValidator.validate(persistentDeckDuringBattle, repo.economyState.value.ownedCardCounts)
        assertTrue("Persistent ActiveDeck must remain VALID during combat", validationDuringBattle.isValid)

        // 8. Finish Victory (triggers rewards claim and persistence save)
        viewModel.debugForceVictory()
        assertEquals("Turn must be VICTORY", BattleTurn.VICTORY, viewModel.uiState.value.currentTurn)

        // 9. Assert persistent ActiveDeck STILL contains exactly 20 cards after Victory
        val persistentDeckAfterVictory = repo.economyState.value.activeDeck
        assertEquals("Persistent ActiveDeck must STILL have exactly 20 cards after Victory", 20, persistentDeckAfterVictory.cardIds.size)
        val validationAfterVictory = DeckValidator.validate(persistentDeckAfterVictory, repo.economyState.value.ownedCardCounts)
        assertTrue("Persistent ActiveDeck must remain VALID after Victory", validationAfterVictory.isValid)

        // 10. Reload repository from persistence
        repo.initPersistence(context)

        // 11. Assert reloaded ActiveDeck STILL contains exactly 20 cards and is VALID
        val persistentDeckAfterReload = repo.economyState.value.activeDeck
        assertEquals("ActiveDeck reloaded from persistence must STILL have exactly 20 cards", 20, persistentDeckAfterReload.cardIds.size)
        val validationAfterReload = DeckValidator.validate(persistentDeckAfterReload, repo.economyState.value.ownedCardCounts)
        assertTrue("Reloaded ActiveDeck must be VALID", validationAfterReload.isValid)
    }

    /**
     * Requirement #8 Regression Test 2:
     * Retry flow isolation test.
     * Start battle → modify runtime → Retry → ActiveDeck still 20 cards.
     */
    @Test
    fun testActiveDeckImmutabilityAcrossRetry() {
        val repo = PlayerEconomyRepository.instance
        repo.saveActiveDeck(ActiveDeck.createDefaultOlympusDeck())
        assertEquals(20, repo.economyState.value.activeDeck.cardIds.size)

        // Start battle
        viewModel.startNewBattle()
        assertEquals(20, viewModel.uiState.value.playerHand.size + viewModel.uiState.value.playerDrawPile.size)

        // Modify runtime by playing a card
        val card = viewModel.uiState.value.playerHand.first()
        viewModel.playCard(card)

        // Tap Retry / Start New Battle
        val retried = viewModel.startNewBattle()
        assertTrue("Retry must succeed", retried)

        // Fresh battle has 20 cards
        val freshTotal = viewModel.uiState.value.playerHand.size + viewModel.uiState.value.playerDrawPile.size
        assertEquals(20, freshTotal)
        assertEquals(4, viewModel.uiState.value.playerHand.size)
        assertEquals(16, viewModel.uiState.value.playerDrawPile.size)
        assertEquals(0, viewModel.uiState.value.playerDiscardPile.size)

        // Persistent ActiveDeck is untouched
        val activeDeck = repo.economyState.value.activeDeck
        assertEquals("Persistent ActiveDeck must remain 20 cards after Retry", 20, activeDeck.cardIds.size)
        assertTrue("Persistent ActiveDeck must be valid", DeckValidator.validate(activeDeck, repo.economyState.value.ownedCardCounts).isValid)
    }

    /**
     * Requirement #8 Regression Test 3:
     * Defeat flow isolation test.
     * Start battle → modify runtime → Defeat → ActiveDeck still 20 cards.
     */
    @Test
    fun testActiveDeckImmutabilityAcrossDefeat() {
        val repo = PlayerEconomyRepository.instance
        repo.saveActiveDeck(ActiveDeck.createDefaultOlympusDeck())
        assertEquals(20, repo.economyState.value.activeDeck.cardIds.size)

        viewModel.startNewBattle()
        val card = viewModel.uiState.value.playerHand.first()
        viewModel.playCard(card)

        // Trigger Defeat
        viewModel.debugForceDefeat()
        assertEquals(BattleTurn.DEFEAT, viewModel.uiState.value.currentTurn)

        // Persistent ActiveDeck is untouched
        val activeDeck = repo.economyState.value.activeDeck
        assertEquals("Persistent ActiveDeck must remain 20 cards after Defeat", 20, activeDeck.cardIds.size)
        assertTrue("Persistent ActiveDeck must be valid", DeckValidator.validate(activeDeck, repo.economyState.value.ownedCardCounts).isValid)
    }

    /**
     * Requirement #8 Regression Test 4:
     * Duplicate card instance independence & ActiveDeck integrity.
     * ActiveDeck contains two copies of a card.
     * Play one copy.
     * Assert:
     * - battle runtime loses only one copy
     * - second copy remains in hand
     * - ActiveDeck still contains two copies (and 20 total).
     */
    @Test
    fun testDuplicateCardInstancesAndActiveDeckIntegrity() {
        val repo = PlayerEconomyRepository.instance
        val defaultDeck = ActiveDeck.createDefaultOlympusDeck()
        repo.saveActiveDeck(defaultDeck)

        // Default deck contains 2 copies of c_olympian_guard
        val guardCopiesInActiveDeck = repo.economyState.value.activeDeck.cardIds.count { it == "c_olympian_guard" }
        assertEquals(2, guardCopiesInActiveDeck)

        // Setup hand with both instances
        val guard1 = CardCatalog.getCard("c_olympian_guard", 1).copy(instanceId = "guard_inst_A")
        val guard2 = CardCatalog.getCard("c_olympian_guard", 1).copy(instanceId = "guard_inst_B")

        val stateField = viewModel.javaClass.getDeclaredField("_uiState")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val uiFlow = stateField.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<BattleUiState>
        uiFlow.value = uiFlow.value.copy(
            playerEnergy = 5,
            playerHand = listOf(guard1, guard2),
            currentTurn = BattleTurn.PLAYER_TURN,
            isExecutingTurn = false
        )

        // Play instance A
        viewModel.playCard(guard1)

        val battleStateAfterPlay = viewModel.uiState.value
        // Only instance A removed, instance B still in hand
        assertFalse("Instance A should be removed from hand", battleStateAfterPlay.playerHand.any { it.instanceId == "guard_inst_A" })
        assertTrue("Instance B must still remain in hand", battleStateAfterPlay.playerHand.any { it.instanceId == "guard_inst_B" })
        assertEquals(1, battleStateAfterPlay.playerHand.size)

        // Persistent ActiveDeck must STILL have both copies and exactly 20 cards!
        val activeDeck = repo.economyState.value.activeDeck
        assertEquals(20, activeDeck.cardIds.size)
        assertEquals("ActiveDeck must still contain both copies", 2, activeDeck.cardIds.count { it == "c_olympian_guard" })
        assertTrue(DeckValidator.validate(activeDeck, repo.economyState.value.ownedCardCounts).isValid)
    }
}
