package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.backend.AuditOperationType
import com.example.backend.BackendConfig
import com.example.backend.BackendMode
import com.example.backend.DefaultPlayerIdentity
import com.example.backend.GameSyncRepository
import com.example.backend.MythosBackend
import com.example.backend.ServerAuditLogger
import com.example.backend.SyncState
import com.example.data.ActiveDeck
import com.example.data.CardCatalog
import com.example.data.DeckValidator
import com.example.monetization.PlayerEconomyRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies the app-facing Phase 12 boundary: the Arena deck gate must agree with the existing local
 * deck rules (so it never blocks a deck the player could already legitimately use) while still
 * rejecting invalid decks, and the backend must stay local-only until BackendConfig enables sync.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase12IntegrationTest {

    private lateinit var repository: PlayerEconomyRepository

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("mythos_player_data", Context.MODE_PRIVATE).edit().clear().commit()
        repository = PlayerEconomyRepository()
        repository.initPersistence(context)
        ServerAuditLogger.clear()
    }

    @Test
    fun defaultActiveDeckPassesTheArenaDeckGate() = runBlocking {
        val state = repository.economyState.value
        val result = GameSyncRepository().validateDeckForPlay(state.activeDeck, state.ownedCardCounts)

        assertTrue("Default deck must be accepted: ${result.message}", result.isSuccess)
        assertEquals(ActiveDeck.REQUIRED_DECK_SIZE, result.syncedItems)
    }

    @Test
    fun gateAgreesWithExistingLocalDeckValidator() = runBlocking {
        val state = repository.economyState.value
        val validCards = state.activeDeck.cardIds

        val unownedCard = CardCatalog.ALL_CARDS.first { state.ownedCardCounts[it.id] == null }.id
        val decks = listOf(
            state.activeDeck,
            state.activeDeck.copy(cardIds = validCards.dropLast(1)),
            state.activeDeck.copy(cardIds = validCards.dropLast(1) + unownedCard),
            state.activeDeck.copy(cardIds = validCards.dropLast(3) + List(3) { validCards.first() }),
            state.activeDeck.copy(cardIds = validCards.dropLast(1) + "c_does_not_exist")
        )

        for (deck in decks) {
            val local = DeckValidator.validate(deck, state.ownedCardCounts).isValid
            val gate = GameSyncRepository().validateDeckForPlay(deck, state.ownedCardCounts).isSuccess
            assertEquals("Gate and local validator must agree for deck of ${deck.cardIds.size}", local, gate)
        }
    }

    @Test
    fun unownedCardIsRejectedAuditedAndNeverBecomesAuthoritative() = runBlocking {
        val state = repository.economyState.value
        val unownedCard = CardCatalog.ALL_CARDS.first { state.ownedCardCounts[it.id] == null }.id
        val tampered = state.activeDeck.copy(cardIds = state.activeDeck.cardIds.dropLast(1) + unownedCard)

        val sync = GameSyncRepository()
        val result = sync.validateDeckForPlay(tampered, state.ownedCardCounts)

        assertFalse(result.isSuccess)
        assertNull("A rejected deck must not become the authoritative deck", sync.authoritativeDeck)
        assertTrue(ServerAuditLogger.logs.any { it.type == AuditOperationType.DECK_VALIDATION_FAILURE })
    }

    @Test
    fun rejectedDeckDoesNotReplacePreviouslyValidatedDeck() = runBlocking {
        val state = repository.economyState.value
        val sync = GameSyncRepository()
        assertTrue(sync.validateDeckForPlay(state.activeDeck, state.ownedCardCounts).isSuccess)

        val tooShort = state.activeDeck.copy(cardIds = state.activeDeck.cardIds.dropLast(1))
        assertFalse(sync.validateDeckForPlay(tooShort, state.ownedCardCounts).isSuccess)

        assertEquals(ActiveDeck.REQUIRED_DECK_SIZE, sync.authoritativeDeck?.cardIds?.size)
    }

    @Test
    fun backendStaysLocalUntilSyncIsEnabled() {
        assertEquals("ONLINE_AUTHORITATIVE must stay off until the Cloud Functions are deployed", BackendMode.LOCAL_DEVELOPMENT, BackendConfig.MODE)
        assertFalse(MythosBackend.isOnline)
        assertSame(DefaultPlayerIdentity.Instance, MythosBackend.identity)
        assertEquals(SyncState.LOCAL_ONLY, MythosBackend.sync.syncState.value)
    }
}
