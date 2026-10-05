package com.example.backend.online

import com.example.backend.MythosBackend
import com.example.data.BattleEncounterConfig
import com.example.data.BattleStats

/** What the server decided about a finished online battle (or why we could not ask it). */
sealed interface OnlineBattleOutcome {
    data class Arena(val result: OnlineArenaResult) : OnlineBattleOutcome
    data class WorldBoss(val result: OnlineBossResult) : OnlineBattleOutcome
    data class Raid(val result: OnlineRaidResult) : OnlineBattleOutcome
    /** [willRetry]: the result is queued on the device and will be re-sent; nothing was granted locally. */
    data class Failed(val message: String, val willRetry: Boolean) : OnlineBattleOutcome
}

/**
 * Bridge between a finished battle (BattleViewModel) and the server (OnlineBackend). The battle only
 * reports what happened; rating, rewards and progress are decided by the server and come back in the
 * outcome (and, for gold/XP, through the reward outbox).
 */
class OnlineBattleReporter(private val backend: () -> OnlineBackend = { MythosBackend.online }) {

    suspend fun report(config: BattleEncounterConfig, victory: Boolean, stats: BattleStats): OnlineBattleOutcome {
        val sessionId = config.onlineSessionId
            ?: return OnlineBattleOutcome.Failed("This battle has no server session.", willRetry = false)
        val online = backend()

        return when {
            config.isArenaMatch -> online.submitArenaResult(sessionId, victory, stats).fold(
                { OnlineBattleOutcome.Arena(it) }, ::failed
            )
            config.isWorldBoss -> online.submitWorldBoss(sessionId, stats.totalDamageDealt, victory).fold(
                { OnlineBattleOutcome.WorldBoss(it) }, ::failed
            )
            config.isRaid -> online.submitRaid(sessionId, stats.totalDamageDealt, victory).fold(
                { OnlineBattleOutcome.Raid(it) }, ::failed
            )
            else -> OnlineBattleOutcome.Failed("Unsupported online encounter.", willRetry = false)
        }
    }

    private fun failed(e: Throwable): OnlineBattleOutcome {
        val backendError = e as? BackendException
        return OnlineBattleOutcome.Failed(
            message = if (backendError != null) OnlineBackend.userMessage(backendError) else (e.message ?: "Unexpected error."),
            willRetry = backendError?.isTransient == true
        )
    }
}
