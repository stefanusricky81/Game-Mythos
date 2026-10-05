package com.example.backend.online

import com.example.data.ActiveDeck
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * Typed client for the Phase 12 Cloud Functions. This is the ONLY place that knows function names and
 * payload shapes, so a rename or schema change on the server has exactly one client counterpart.
 *
 * Mutating calls generate ONE requestId per logical action and reuse it across transient retries, so a
 * lost response or a flaky connection can never produce a second grant (the server replays the stored
 * result for a repeated requestId).
 */
class GameBackendApi(
    private val transport: BackendTransport,
    private val newRequestId: () -> String = { UUID.randomUUID().toString() },
    private val maxAttempts: Int = 3,
    private val retryDelayMs: Long = 600
) {
    private suspend fun <T> withRetry(block: suspend () -> T): T {
        var attempt = 0
        while (true) {
            try {
                return block()
            } catch (e: BackendException) {
                attempt++
                if (!e.isTransient || attempt >= maxAttempts) throw e
                delay(retryDelayMs * attempt)
            }
        }
    }

    private suspend fun read(function: String, payload: Json = emptyMap()): Json = withRetry { transport.call(function, payload) }

    private suspend fun mutate(function: String, payload: Json = emptyMap()): Json {
        val requestId = newRequestId()
        return withRetry { transport.call(function, payload + ("requestId" to requestId)) }
    }

    private fun deck(d: ActiveDeck): Json = mapOf("heroId" to d.heroId, "cardIds" to d.cardIds)

    // ---- player / deck -----------------------------------------------------------------------------
    suspend fun ensureProfile(): Json = read("ensurePlayerProfile")

    /** Returns the server's verdict as (isValid, human readable issues). */
    suspend fun validateDeck(d: ActiveDeck): Pair<Boolean, List<String>> {
        val r = read("validateDeck", deck(d))
        return r.bool("valid") to r.objList("issues").map { it.str("message") }
    }

    suspend fun syncCollection(ownedCardCounts: Map<String, Int>): Json = mutate("syncCollection", mapOf("ownedCardCounts" to ownedCardCounts))

    // ---- arena -------------------------------------------------------------------------------------
    suspend fun startArenaMatch(d: ActiveDeck): OnlineArenaMatch = OnlineArenaMatch.from(mutate("startArenaMatch", deck(d)))

    suspend fun submitArenaResult(matchId: String, outcome: String, turns: Int, damageDealt: Int, damageTaken: Int): OnlineArenaResult =
        OnlineArenaResult.from(
            // The match id is the idempotency key for submissions, so no requestId is needed.
            withRetry {
                transport.call(
                    "submitArenaResult",
                    mapOf("matchId" to matchId, "outcome" to outcome, "turns" to turns, "damageDealt" to damageDealt, "damageTaken" to damageTaken)
                )
            }
        )

    suspend fun claimArenaSeasonReward(seasonId: String): OnlineSeasonClaim =
        OnlineSeasonClaim.from(mutate("claimArenaReward", mapOf("seasonId" to seasonId)))

    suspend fun getArenaState(): OnlineArenaSnapshot = OnlineArenaSnapshot.from(read("getArenaState"))

    suspend fun getLeaderboard(limit: Int = 50): OnlineLeaderboard = OnlineLeaderboard.from(read("getLeaderboard", mapOf("limit" to limit)))

    // ---- endgame -----------------------------------------------------------------------------------
    suspend fun startWorldBoss(bossId: String): OnlineSession =
        OnlineSession.from(mutate("startEndgameAttempt", mapOf("kind" to "WORLD_BOSS", "targetId" to bossId)))

    suspend fun startRaid(raidId: String, stage: Int, difficulty: String): OnlineSession =
        OnlineSession.from(mutate("startEndgameAttempt", mapOf("kind" to "RAID", "targetId" to raidId, "stage" to stage, "difficulty" to difficulty)))

    suspend fun submitWorldBoss(sessionId: String, damage: Int, victory: Boolean): OnlineBossResult =
        OnlineBossResult.from(withRetry { transport.call("submitWorldBossContribution", mapOf("sessionId" to sessionId, "damage" to damage, "victory" to victory)) })

    suspend fun submitRaid(sessionId: String, damage: Int, cleared: Boolean): OnlineRaidResult =
        OnlineRaidResult.from(withRetry { transport.call("submitRaidContribution", mapOf("sessionId" to sessionId, "damage" to damage, "cleared" to cleared)) })

    suspend fun claimWorldBossReward(bossId: String): OnlineClaim = OnlineClaim.from(mutate("claimWorldBossReward", mapOf("bossId" to bossId)))

    suspend fun claimEventReward(eventId: String, itemId: String): OnlineClaim =
        OnlineClaim.from(mutate("claimEventReward", mapOf("eventId" to eventId, "itemId" to itemId)))

    suspend fun getEndgameState(): OnlineEndgameState = OnlineEndgameState.from(read("getEndgameState"))

    // ---- alliance ----------------------------------------------------------------------------------
    suspend fun syncAlliance(includeOpen: Boolean = true): OnlineAllianceView =
        OnlineAllianceView.from(read("syncAlliance", mapOf("includeOpen" to includeOpen)))

    suspend fun allianceAction(action: String, extra: Json = emptyMap()): Json = mutate("allianceAction", extra + ("action" to action))

    // ---- reward outbox -----------------------------------------------------------------------------
    suspend fun getPendingRewards(): List<OnlinePendingReward> = read("getPendingRewards").objList("rewards").map { OnlinePendingReward.from(it) }

    suspend fun ackRewards(claimIds: List<String>): Int =
        if (claimIds.isEmpty()) 0 else withRetry { transport.call("ackRewards", mapOf("claimIds" to claimIds)) }.int("acknowledged")
}
