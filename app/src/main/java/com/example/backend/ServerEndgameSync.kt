package com.example.backend

import com.example.data.EndgameCatalog

/**
 * Server-authoritative World Boss State (Requirement #19).
 * Represents worldBosses/{bossId} in Cloud Firestore.
 */
data class ServerWorldBossState(
    val bossId: String = EndgameCatalog.KRONOS.bossId,
    val seasonId: String = "boss_season_01",
    val maxHp: Long = EndgameCatalog.KRONOS.maxHp,
    val currentHp: Long = EndgameCatalog.KRONOS.maxHp,
    val phase: Int = 1,
    val playerContributions: Map<String, Long> = emptyMap(),
    val allianceContributions: Map<String, Long> = emptyMap(),
    val claimedRewards: Set<String> = emptySet(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isDefeated: Boolean get() = currentHp <= 0L
    val hpPercentage: Float get() = (currentHp.toFloat() / maxHp.toFloat()).coerceIn(0f, 1f)

    companion object {
        const val MAX_DAMAGE_PER_ATTEMPT = 500_000L
    }
}

/**
 * Server-authoritative Alliance Raid State (Requirement #20).
 * Represents raids/{raidId} in Cloud Firestore.
 */
data class ServerAllianceRaidState(
    val raidId: String = "raid_tartarus",
    val currentStage: Int = 1,
    val maxStage: Int = 5,
    val stageHpRemaining: Long = 100_000L,
    val attemptsConsumed: Map<String, Int> = emptyMap(),
    val lastAttemptResetDate: String = "",
    val playerContributions: Map<String, Long> = emptyMap(),
    val claimedStageRewardIds: Set<String> = emptySet(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val MAX_DAILY_ATTEMPTS = 3
    }
}

/**
 * Server-authoritative Limited Event State (Requirement #21).
 * Represents events/{eventId} in Cloud Firestore.
 */
data class ServerLimitedEventState(
    val eventId: String,
    val name: String,
    val isActive: Boolean = true,
    val startTimestamp: Long = 0L,
    val endTimestamp: Long = Long.MAX_VALUE,
    val playerCurrencies: Map<String, Int> = emptyMap(),
    val claimedRewardIds: Set<String> = emptySet(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Server Endgame Synchronization and Validation Authority (Requirement #19, #20, #21).
 */
class ServerEndgameService(
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val dateProvider: (Long) -> String = { ts ->
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(ts))
    }
) {
    @Volatile
    var worldBossState: ServerWorldBossState = ServerWorldBossState()
        private set

    @Volatile
    var raidState: ServerAllianceRaidState = ServerAllianceRaidState()
        private set

    private val _events = mutableMapOf<String, ServerLimitedEventState>()
    val events: Map<String, ServerLimitedEventState> get() = _events.toMap()

    /**
     * Authoritatively records World Boss damage with anti-cheat bounds check (Requirement #19).
     */
    fun submitWorldBossContribution(
        uid: String,
        allianceId: String?,
        damage: Long
    ): ServerWorldBossState {
        require(damage > 0) { "Submitted damage must be positive" }
        check(damage <= ServerWorldBossState.MAX_DAMAGE_PER_ATTEMPT) {
            "Damage $damage exceeds plausible single-attempt limit (${ServerWorldBossState.MAX_DAMAGE_PER_ATTEMPT})"
        }
        check(!worldBossState.isDefeated) { "World Boss is already defeated" }

        val newHp = maxOf(0L, worldBossState.currentHp - damage)
        val prevPlayerContrib = worldBossState.playerContributions[uid] ?: 0L
        val updatedPlayerContrib = worldBossState.playerContributions + (uid to (prevPlayerContrib + damage))

        val updatedAllianceContrib = if (allianceId != null) {
            val prevAllianceContrib = worldBossState.allianceContributions[allianceId] ?: 0L
            worldBossState.allianceContributions + (allianceId to (prevAllianceContrib + damage))
        } else {
            worldBossState.allianceContributions
        }

        val updated = worldBossState.copy(
            currentHp = newHp,
            playerContributions = updatedPlayerContrib,
            allianceContributions = updatedAllianceContrib,
            phase = if (newHp <= (worldBossState.maxHp / 2)) 2 else 1,
            updatedAt = clock()
        )
        worldBossState = updated
        return updated
    }

    /**
     * Authoritatively records Raid attempt and stage damage with 3 daily attempts limit (Requirement #20).
     */
    fun submitRaidContribution(
        uid: String,
        damage: Long
    ): ServerAllianceRaidState {
        val today = dateProvider(clock())
        val resetState = if (raidState.lastAttemptResetDate != today) {
            raidState.copy(attemptsConsumed = emptyMap(), lastAttemptResetDate = today)
        } else {
            raidState
        }

        val usedAttempts = resetState.attemptsConsumed[uid] ?: 0
        check(usedAttempts < ServerAllianceRaidState.MAX_DAILY_ATTEMPTS) {
            "No raid attempts remaining for today ($usedAttempts/${ServerAllianceRaidState.MAX_DAILY_ATTEMPTS})"
        }
        require(damage > 0) { "Raid damage must be positive" }

        val newAttempts = resetState.attemptsConsumed + (uid to (usedAttempts + 1))
        val newPlayerContrib = (resetState.playerContributions[uid] ?: 0L) + damage
        val remainingHp = maxOf(0L, resetState.stageHpRemaining - damage)
        val stageCleared = remainingHp <= 0L
        val newStage = if (stageCleared && resetState.currentStage < resetState.maxStage) {
            resetState.currentStage + 1
        } else {
            resetState.currentStage
        }
        val nextStageHp = if (stageCleared) 150_000L else remainingHp

        val updated = resetState.copy(
            currentStage = newStage,
            stageHpRemaining = nextStageHp,
            attemptsConsumed = newAttempts,
            playerContributions = resetState.playerContributions + (uid to newPlayerContrib),
            updatedAt = clock()
        )
        raidState = updated
        return updated
    }

    /**
     * Registers a limited event into the server state.
     */
    fun registerEvent(event: ServerLimitedEventState) {
        _events[event.eventId] = event
    }

    /**
     * Authoritatively claims an event reward with strict idempotency (Requirement #21).
     */
    fun claimEventReward(
        eventId: String,
        uid: String,
        rewardMilestoneId: String,
        cost: Int
    ): Pair<ServerLimitedEventState, Boolean> {
        val event = _events[eventId] ?: throw IllegalArgumentException("Event '$eventId' not found")
        val now = clock()
        check(event.isActive && now in event.startTimestamp..event.endTimestamp) {
            "Event is not currently active"
        }

        val claimKey = "${uid}_${rewardMilestoneId}"
        if (event.claimedRewardIds.contains(claimKey)) {
            // Already claimed (idempotent, no second grant)
            return Pair(event, false)
        }

        val currentTokens = event.playerCurrencies[uid] ?: 0
        check(currentTokens >= cost) {
            "Insufficient event tokens: has $currentTokens, requires $cost"
        }

        val updatedTokens = event.playerCurrencies + (uid to (currentTokens - cost))
        val updatedClaims = event.claimedRewardIds + claimKey
        val updatedEvent = event.copy(
            playerCurrencies = updatedTokens,
            claimedRewardIds = updatedClaims,
            updatedAt = now
        )
        _events[eventId] = updatedEvent
        return Pair(updatedEvent, true)
    }
}
