package com.example.backend

import com.example.data.ArenaCatalog
import com.example.data.ArenaMatchResult
import com.example.data.ArenaRankTier
import com.example.data.ArenaRatingCalculator
import java.util.UUID

/**
 * Match Lifecycle States for reconnect / interruption handling (Requirement #24).
 */
enum class ServerMatchLifecycleState {
    CREATED,
    ACTIVE,
    COMPLETED,
    ABANDONED,
    EXPIRED
}

/**
 * Server-authoritative Arena Competitive State (Requirement #10).
 * Represents players/{uid}/arena/state in Cloud Firestore.
 */
data class ServerArenaState(
    val uid: String,
    val rating: Int = ArenaRatingCalculator.STARTING_RATING,
    val peakRating: Int = ArenaRatingCalculator.STARTING_RATING,
    val currentSeasonId: String = "season_01",
    val wins: Int = 0,
    val losses: Int = 0,
    val currentWinStreak: Int = 0,
    val highestWinStreak: Int = 0,
    val arenaPoints: Int = 0,
    val dailyAttempts: Int = ArenaCatalog.MAX_DAILY_ATTEMPTS,
    val lastAttemptResetDate: String = "",
    val firstWinClaimedDate: String = "",
    val claimedSeasonRewardIds: Set<String> = emptySet(),
    val activeMatchId: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val tier: ArenaRankTier get() = ArenaRankTier.fromRating(rating)
    val totalMatches: Int get() = wins + losses
    val winRate: Float get() = if (totalMatches > 0) (wins.toFloat() / totalMatches) * 100f else 0f
}

/**
 * Server-side Arena Match Session Record (Requirement #12, #24).
 */
data class ServerArenaMatchSession(
    val matchId: String,
    val playerUid: String,
    val opponentId: String,
    val opponentName: String,
    val opponentArchetype: String,
    val opponentRating: Int,
    val playerRatingBefore: Int,
    val state: ServerMatchLifecycleState = ServerMatchLifecycleState.CREATED,
    val deckCardIds: List<String> = emptyList(),
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val result: ArenaMatchResult? = null,
    val ratingDelta: Int = 0,
    val grantedRewards: ServerArenaRewards? = null
)

/**
 * Granted Arena Rewards Payload (Requirement #15).
 */
data class ServerArenaRewards(
    val gold: Int,
    val cardShards: Int,
    val arenaPoints: Int,
    val xp: Int,
    val isFirstWin: Boolean,
    val streakBonusGold: Int = 0,
    val streakBonusPoints: Int = 0
)

/**
 * Server-authoritative Match History Record (Requirement #13).
 * Persisted in players/{uid}/arenaMatches/{matchId}.
 */
data class ServerArenaHistoryRecord(
    val matchId: String,
    val playerUid: String,
    val opponentId: String,
    val opponentName: String,
    val opponentArchetype: String,
    val ratingBefore: Int,
    val ratingAfter: Int,
    val result: ArenaMatchResult,
    val turns: Int,
    val damageDealt: Int,
    val damageTaken: Int,
    val rewards: ServerArenaRewards,
    val timestamp: Long = System.currentTimeMillis(),
    val seasonId: String = "season_01"
)

/**
 * Server Arena Management Engine (Requirement #10, #11, #12, #14, #15, #16).
 *
 * Implements authoritative match orchestration, ELO calculations, daily attempt tracking,
 * and idempotent result processing.
 */
class ServerArenaService(
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val dateProvider: (Long) -> String = { ts ->
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(ts))
    }
) {
    private val _sessions = mutableMapOf<String, ServerArenaMatchSession>()
    private val _history = mutableListOf<ServerArenaHistoryRecord>()
    private val _processedResults = mutableSetOf<String>()

    val activeSessions: Map<String, ServerArenaMatchSession> get() = _sessions.toMap()
    val matchHistory: List<ServerArenaHistoryRecord> get() = _history.toList()

    /**
     * Resets daily attempts if the calendar date has advanced (Requirement #14).
     */
    fun checkDailyReset(state: ServerArenaState): ServerArenaState {
        val today = dateProvider(clock())
        return if (state.lastAttemptResetDate != today) {
            state.copy(
                dailyAttempts = ArenaCatalog.MAX_DAILY_ATTEMPTS,
                lastAttemptResetDate = today,
                updatedAt = clock()
            )
        } else {
            state
        }
    }

    /**
     * Step 1: Create and validate new Arena match session (Requirement #12, #14).
     * Consumes exactly 1 daily attempt upon match start.
     */
    fun createArenaMatch(
        currentState: ServerArenaState,
        opponentId: String,
        opponentName: String,
        opponentArchetype: String,
        opponentRating: Int,
        deckCardIds: List<String>,
        customMatchId: String? = null
    ): Pair<ServerArenaState, ServerArenaMatchSession> {
        val stateWithReset = checkDailyReset(currentState)

        check(stateWithReset.dailyAttempts > 0) {
            "No daily Arena attempts remaining (${stateWithReset.dailyAttempts}/${ArenaCatalog.MAX_DAILY_ATTEMPTS})."
        }
        check(deckCardIds.size == ServerDeckValidator.REQUIRED_DECK_SIZE) {
            "Active deck must contain exactly ${ServerDeckValidator.REQUIRED_DECK_SIZE} cards to enter Arena."
        }

        val matchId = customMatchId ?: "arena_match_${UUID.randomUUID()}"
        val session = ServerArenaMatchSession(
            matchId = matchId,
            playerUid = stateWithReset.uid,
            opponentId = opponentId,
            opponentName = opponentName,
            opponentArchetype = opponentArchetype,
            opponentRating = opponentRating,
            playerRatingBefore = stateWithReset.rating,
            state = ServerMatchLifecycleState.ACTIVE,
            deckCardIds = deckCardIds.toList(),
            startedAt = clock()
        )

        val updatedState = stateWithReset.copy(
            dailyAttempts = stateWithReset.dailyAttempts - 1,
            activeMatchId = matchId,
            updatedAt = clock()
        )

        _sessions[matchId] = session
        return Pair(updatedState, session)
    }

    /**
     * Step 2: Authoritatively process and finalize Arena match result (Requirement #11, #12, #15).
     * Strictly idempotent: submitting the same match result multiple times will NOT duplicate rewards or rating changes.
     */
    fun submitArenaResult(
        currentState: ServerArenaState,
        matchId: String,
        result: ArenaMatchResult,
        turns: Int,
        damageDealt: Int,
        damageTaken: Int
    ): Pair<ServerArenaState, ServerArenaMatchSession> {
        val session = _sessions[matchId] ?: throw IllegalArgumentException("Match session '$matchId' not found.")

        // Idempotency check: If match is already completed or processed, return without duplicate modification
        if (session.state == ServerMatchLifecycleState.COMPLETED || _processedResults.contains(matchId)) {
            return Pair(currentState, session)
        }

        // Authoritative ELO Rating Calculation (Requirement #11)
        val ratingDelta = ArenaRatingCalculator.calculateRatingDelta(
            playerRating = session.playerRatingBefore,
            opponentRating = session.opponentRating,
            result = result
        )
        val newRating = ArenaRatingCalculator.applyRatingChange(session.playerRatingBefore, ratingDelta)
        val newPeak = maxOf(currentState.peakRating, newRating)

        // Streak calculation
        val newStreak = if (result == ArenaMatchResult.VICTORY) currentState.currentWinStreak + 1 else 0
        val newHighestStreak = maxOf(currentState.highestWinStreak, newStreak)

        // Rewards calculation (Requirement #15)
        val today = dateProvider(clock())
        val isFirstWin = (result == ArenaMatchResult.VICTORY) && (currentState.firstWinClaimedDate != today)
        val (streakBonusGold, streakBonusPts) = if (result == ArenaMatchResult.VICTORY) {
            ArenaCatalog.Rewards.getStreakBonus(newStreak)
        } else {
            Pair(0, 0)
        }

        val baseRewards = when (result) {
            ArenaMatchResult.VICTORY -> {
                if (isFirstWin) {
                    ServerArenaRewards(
                        gold = ArenaCatalog.Rewards.FIRST_WIN_GOLD + streakBonusGold,
                        cardShards = ArenaCatalog.Rewards.FIRST_WIN_SHARDS,
                        arenaPoints = ArenaCatalog.Rewards.FIRST_WIN_ARENA_POINTS + streakBonusPts,
                        xp = ArenaCatalog.Rewards.FIRST_WIN_XP,
                        isFirstWin = true,
                        streakBonusGold = streakBonusGold,
                        streakBonusPoints = streakBonusPts
                    )
                } else {
                    ServerArenaRewards(
                        gold = ArenaCatalog.Rewards.NORMAL_WIN_GOLD + streakBonusGold,
                        cardShards = ArenaCatalog.Rewards.NORMAL_WIN_SHARDS,
                        arenaPoints = ArenaCatalog.Rewards.NORMAL_WIN_ARENA_POINTS + streakBonusPts,
                        xp = ArenaCatalog.Rewards.NORMAL_WIN_XP,
                        isFirstWin = false,
                        streakBonusGold = streakBonusGold,
                        streakBonusPoints = streakBonusPts
                    )
                }
            }
            ArenaMatchResult.DEFEAT -> {
                ServerArenaRewards(
                    gold = ArenaCatalog.Rewards.DEFEAT_GOLD,
                    cardShards = ArenaCatalog.Rewards.DEFEAT_SHARDS,
                    arenaPoints = ArenaCatalog.Rewards.DEFEAT_ARENA_POINTS,
                    xp = ArenaCatalog.Rewards.DEFEAT_XP,
                    isFirstWin = false
                )
            }
            ArenaMatchResult.DRAW -> {
                ServerArenaRewards(
                    gold = ArenaCatalog.Rewards.DEFEAT_GOLD,
                    cardShards = ArenaCatalog.Rewards.DEFEAT_SHARDS,
                    arenaPoints = ArenaCatalog.Rewards.DEFEAT_ARENA_POINTS,
                    xp = ArenaCatalog.Rewards.DEFEAT_XP,
                    isFirstWin = false
                )
            }
        }

        val completedSession = session.copy(
            state = ServerMatchLifecycleState.COMPLETED,
            completedAt = clock(),
            result = result,
            ratingDelta = ratingDelta,
            grantedRewards = baseRewards
        )
        _sessions[matchId] = completedSession
        _processedResults.add(matchId)

        // Persist match history (Requirement #13)
        val historyRecord = ServerArenaHistoryRecord(
            matchId = matchId,
            playerUid = currentState.uid,
            opponentId = session.opponentId,
            opponentName = session.opponentName,
            opponentArchetype = session.opponentArchetype,
            ratingBefore = session.playerRatingBefore,
            ratingAfter = newRating,
            result = result,
            turns = turns,
            damageDealt = damageDealt,
            damageTaken = damageTaken,
            rewards = baseRewards,
            timestamp = clock(),
            seasonId = currentState.currentSeasonId
        )
        _history.add(0, historyRecord)

        val updatedState = currentState.copy(
            rating = newRating,
            peakRating = newPeak,
            wins = if (result == ArenaMatchResult.VICTORY) currentState.wins + 1 else currentState.wins,
            losses = if (result == ArenaMatchResult.DEFEAT) currentState.losses + 1 else currentState.losses,
            currentWinStreak = newStreak,
            highestWinStreak = newHighestStreak,
            arenaPoints = currentState.arenaPoints + baseRewards.arenaPoints,
            firstWinClaimedDate = if (isFirstWin) today else currentState.firstWinClaimedDate,
            activeMatchId = null,
            updatedAt = clock()
        )

        return Pair(updatedState, completedSession)
    }

    /**
     * Resolves interrupted or abandoned matches (Requirement #24).
     */
    fun resolveAbandonedMatch(
        currentState: ServerArenaState,
        matchId: String
    ): Pair<ServerArenaState, ServerArenaMatchSession> {
        val session = _sessions[matchId] ?: return Pair(currentState.copy(activeMatchId = null), _sessions.values.first())
        if (session.state == ServerMatchLifecycleState.ACTIVE) {
            val abandoned = session.copy(
                state = ServerMatchLifecycleState.ABANDONED,
                completedAt = clock()
            )
            _sessions[matchId] = abandoned
            return Pair(currentState.copy(activeMatchId = null), abandoned)
        }
        return Pair(currentState.copy(activeMatchId = null), session)
    }

    /**
     * Season Reset Execution (Requirement #16).
     * Preserves peak rating, claims season reward if available, resets rating according to formula.
     */
    fun processSeasonReset(
        currentState: ServerArenaState,
        newSeasonId: String
    ): ServerArenaState {
        val newStartingRating = ArenaRatingCalculator.calculateSeasonResetRating(currentState.rating)
        return currentState.copy(
            currentSeasonId = newSeasonId,
            rating = newStartingRating,
            // peakRating is preserved for historical showcase
            wins = 0,
            losses = 0,
            currentWinStreak = 0,
            firstWinClaimedDate = "",
            activeMatchId = null,
            updatedAt = clock()
        )
    }
}
