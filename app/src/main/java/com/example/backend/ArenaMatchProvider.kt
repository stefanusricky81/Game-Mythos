package com.example.backend

import com.example.data.ArenaCatalog
import com.example.data.ArenaMatchResult
import com.example.data.ArenaOpponent
import com.example.data.ArenaOpponentArchetype

/**
 * Matched Arena Opponent Data Payload (Requirement #25).
 */
data class ArenaMatchedOpponent(
    val matchId: String,
    val opponentId: String,
    val name: String,
    val rating: Int,
    val archetype: ArenaOpponentArchetype,
    val heroId: String,
    val heroName: String,
    val deckCardIds: List<String>,
    val isSimulated: Boolean
)

/**
 * Result submission request payload.
 */
data class ArenaMatchSubmission(
    val matchId: String,
    val result: ArenaMatchResult,
    val turns: Int,
    val damageDealt: Int,
    val damageTaken: Int,
    val clientRequestId: String
)

/**
 * Server Arena Result Response Payload.
 */
data class ServerArenaResultResponse(
    val matchId: String,
    val isSuccess: Boolean,
    val ratingBefore: Int,
    val ratingAfter: Int,
    val ratingDelta: Int,
    val rewards: ServerArenaRewards,
    val isIdempotentReplay: Boolean
)

/**
 * Arena Match Provider Interface (Requirement #25).
 * Decouples match generation and submission so real-time multiplayer / server matchmaking
 * can cleanly replace the simulated opponent without duplicating combat or UI logic.
 */
interface ArenaMatchProvider {
    suspend fun findMatch(
        playerRating: Int,
        playerHeroId: String,
        playerDeckCardIds: List<String>
    ): ArenaMatchedOpponent

    suspend fun submitResult(
        playerUid: String,
        submission: ArenaMatchSubmission
    ): ServerArenaResultResponse
}

/**
 * Simulated Arena Match Provider (Phase 11/12 baseline).
 * Uses rating-bracket search and deterministic archetype deck strategies.
 */
class SimulatedArenaMatchProvider(
    private val serverArenaService: ServerArenaService,
    private val getArenaState: () -> ServerArenaState
) : ArenaMatchProvider {

    override suspend fun findMatch(
        playerRating: Int,
        playerHeroId: String,
        playerDeckCardIds: List<String>
    ): ArenaMatchedOpponent {
        val opponent = ArenaCatalog.findOpponent(playerRating)
        val currentState = getArenaState()
        val (_, session) = serverArenaService.createArenaMatch(
            currentState = currentState,
            opponentId = opponent.id,
            opponentName = opponent.name,
            opponentArchetype = opponent.archetype.name,
            opponentRating = opponent.rating,
            deckCardIds = playerDeckCardIds
        )

        return ArenaMatchedOpponent(
            matchId = session.matchId,
            opponentId = opponent.id,
            name = opponent.name,
            rating = opponent.rating,
            archetype = opponent.archetype,
            heroId = opponent.archetype.heroId,
            heroName = opponent.archetype.heroName,
            deckCardIds = opponent.deckCardIds,
            isSimulated = true
        )
    }

    override suspend fun submitResult(
        playerUid: String,
        submission: ArenaMatchSubmission
    ): ServerArenaResultResponse {
        val currentState = getArenaState()
        val sessionBefore = serverArenaService.activeSessions[submission.matchId]
            ?: throw IllegalArgumentException("Active match session not found")

        val (updatedState, completedSession) = serverArenaService.submitArenaResult(
            currentState = currentState,
            matchId = submission.matchId,
            result = submission.result,
            turns = submission.turns,
            damageDealt = submission.damageDealt,
            damageTaken = submission.damageTaken
        )

        return ServerArenaResultResponse(
            matchId = completedSession.matchId,
            isSuccess = true,
            ratingBefore = completedSession.playerRatingBefore,
            ratingAfter = updatedState.rating,
            ratingDelta = completedSession.ratingDelta,
            rewards = completedSession.grantedRewards ?: ServerArenaRewards(0, 0, 0, 0, false),
            isIdempotentReplay = (completedSession.state == ServerMatchLifecycleState.COMPLETED)
        )
    }
}

/**
 * Production Firebase Arena Match Provider (Placeholder for Live Cloud matchmaking).
 * Adheres strictly to the identical interface.
 */
class FirebaseArenaMatchProvider(
    private val serverArenaService: ServerArenaService,
    private val getArenaState: () -> ServerArenaState
) : ArenaMatchProvider {
    // When backend cloud queue is live, this executes Cloud Functions 'startArenaMatch'
    private val fallback = SimulatedArenaMatchProvider(serverArenaService, getArenaState)

    override suspend fun findMatch(
        playerRating: Int,
        playerHeroId: String,
        playerDeckCardIds: List<String>
    ): ArenaMatchedOpponent {
        return fallback.findMatch(playerRating, playerHeroId, playerDeckCardIds)
    }

    override suspend fun submitResult(
        playerUid: String,
        submission: ArenaMatchSubmission
    ): ServerArenaResultResponse {
        return fallback.submitResult(playerUid, submission)
    }
}
