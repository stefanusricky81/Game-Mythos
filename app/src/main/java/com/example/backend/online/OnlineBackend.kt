package com.example.backend.online

import android.content.Context
import com.example.backend.FirebasePlayerIdentity
import com.example.data.ActiveDeck
import com.example.data.Alliance
import com.example.data.AllianceMember
import com.example.data.AllianceRole
import com.example.data.ArenaLeaderboardEntry
import com.example.data.ArenaMatchRecord
import com.example.data.ArenaMatchResult
import com.example.data.ArenaRankTier
import com.example.data.ArenaSeason
import com.example.data.BattleStats
import com.example.data.EndgameCatalog
import com.example.data.HeroCatalog
import com.example.data.WorldBossContribution
import com.example.monetization.PlayerEconomyRepository
import com.example.monetization.PlayerEconomyState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface OnlineStatus {
    data object Idle : OnlineStatus
    data object Syncing : OnlineStatus
    data object Ready : OnlineStatus
    /** [offline] = the server could not be reached (as opposed to the server refusing the request). */
    data class Error(val message: String, val offline: Boolean) : OnlineStatus
}

data class OnlineSnapshot(
    val myUid: String? = null,
    val arena: OnlineArenaSnapshot? = null,
    val leaderboard: OnlineLeaderboard? = null,
    val endgame: OnlineEndgameState? = null,
    val alliance: OnlineAllianceView? = null
) {
    /** The oldest finished season whose reward is still unclaimed (the server refuses unfinished ones). */
    val claimableSeason: OnlineClaimableSeason? get() = arena?.claimable?.firstOrNull()
}

/**
 * The ONLINE_AUTHORITATIVE game backend as the app sees it: server-issued sessions, server-computed
 * results, a reward outbox applied exactly once, and a snapshot of server state that screens overlay
 * onto the local economy for display. Every mutating call returns a Result so the UI can show the
 * server's explicit refusal ("No daily Arena attempts remaining.") or an offline notice - it never
 * falls back to granting anything locally.
 */
class OnlineBackend(
    val api: GameBackendApi,
    private val rewards: OnlineRewardSync,
    private val pending: PendingSubmissionStore,
    private val uidProvider: () -> String? = { null }
) {
    private val _snapshot = MutableStateFlow(OnlineSnapshot())
    val snapshot: StateFlow<OnlineSnapshot> = _snapshot.asStateFlow()

    private val _status = MutableStateFlow<OnlineStatus>(OnlineStatus.Idle)
    val status: StateFlow<OnlineStatus> = _status.asStateFlow()

    private suspend fun <T> tracked(block: suspend () -> T): Result<T> {
        _status.value = OnlineStatus.Syncing
        return try {
            val value = block()
            _status.value = OnlineStatus.Ready
            Result.success(value)
        } catch (e: CancellationException) {
            throw e
        } catch (e: BackendException) {
            _status.value = OnlineStatus.Error(userMessage(e), offline = e.isTransient)
            Result.failure(e)
        }
    }

    private fun touchUid() {
        val uid = runCatching { uidProvider() }.getOrNull()
        if (uid != null && _snapshot.value.myUid != uid) _snapshot.update { it.copy(myUid = uid) }
    }

    // ---- refresh ---------------------------------------------------------------------------------
    suspend fun refreshArena(): Result<Unit> = tracked {
        val arena = api.getArenaState()
        touchUid()
        _snapshot.update { it.copy(arena = arena) }
    }

    suspend fun refreshLeaderboard(): Result<Unit> = tracked {
        val board = api.getLeaderboard()
        touchUid()
        _snapshot.update { it.copy(leaderboard = board) }
    }

    suspend fun refreshEndgame(): Result<Unit> = tracked {
        val endgame = api.getEndgameState()
        touchUid()
        _snapshot.update { it.copy(endgame = endgame) }
    }

    suspend fun refreshAlliance(): Result<Unit> = tracked {
        val alliance = api.syncAlliance()
        touchUid()
        _snapshot.update { it.copy(alliance = alliance) }
    }

    /** Re-sends results that could not be delivered earlier, applies pending rewards, refreshes state. */
    suspend fun refreshAll(): Result<Unit> {
        retryPending()
        val steps = listOf(
            tracked { rewards.sync() }.map { },
            refreshArena(), refreshEndgame(), refreshAlliance()
        )
        return steps.firstOrNull { it.isFailure } ?: Result.success(Unit)
    }

    private suspend fun syncRewardsQuietly() {
        try { rewards.sync() } catch (e: CancellationException) { throw e } catch (e: BackendException) { /* stays PENDING; next sync applies it */ }
    }

    // ---- arena -----------------------------------------------------------------------------------
    suspend fun startArenaMatch(deck: ActiveDeck): Result<OnlineArenaMatch> = tracked {
        val match = api.startArenaMatch(deck)
        refreshArena()
        match
    }

    suspend fun submitArenaResult(matchId: String, victory: Boolean, stats: BattleStats): Result<OnlineArenaResult> {
        val payload: Map<String, Any> = mapOf(
            "matchId" to matchId,
            "outcome" to if (victory) "VICTORY" else "DEFEAT",
            "turns" to stats.turnsCount.coerceAtLeast(1),
            "damageDealt" to stats.totalDamageDealt.coerceAtLeast(0),
            "damageTaken" to stats.totalDamageTaken.coerceAtLeast(0)
        )
        return deliver("submitArenaResult", payload) {
            val result = api.submitArenaResult(
                payload["matchId"] as String, payload["outcome"] as String, payload["turns"] as Int,
                payload["damageDealt"] as Int, payload["damageTaken"] as Int
            )
            result.arena?.let { state -> _snapshot.update { s -> s.copy(arena = s.arena?.copy(state = state)) } }
            syncRewardsQuietly()
            refreshArena()
            result
        }
    }

    suspend fun claimSeasonReward(seasonId: String): Result<OnlineSeasonClaim> = tracked {
        val claim = api.claimArenaSeasonReward(seasonId)
        syncRewardsQuietly()
        refreshArena()
        claim
    }

    // ---- endgame ---------------------------------------------------------------------------------
    suspend fun startWorldBoss(bossId: String): Result<OnlineSession> = tracked { api.startWorldBoss(bossId).also { refreshEndgame() } }

    suspend fun startRaid(raidId: String, stage: Int, difficulty: String): Result<OnlineSession> =
        tracked { api.startRaid(raidId, stage, difficulty).also { refreshEndgame() } }

    suspend fun submitWorldBoss(sessionId: String, damage: Int, victory: Boolean): Result<OnlineBossResult> {
        val payload: Map<String, Any> = mapOf("sessionId" to sessionId, "damage" to damage.coerceAtLeast(0), "victory" to victory)
        return deliver("submitWorldBossContribution", payload) {
            val result = api.submitWorldBoss(sessionId, payload["damage"] as Int, victory)
            syncRewardsQuietly()
            refreshEndgame()
            result
        }
    }

    suspend fun submitRaid(sessionId: String, damage: Int, cleared: Boolean): Result<OnlineRaidResult> {
        val payload: Map<String, Any> = mapOf("sessionId" to sessionId, "damage" to damage.coerceAtLeast(0), "cleared" to cleared)
        return deliver("submitRaidContribution", payload) {
            val result = api.submitRaid(sessionId, payload["damage"] as Int, cleared)
            syncRewardsQuietly()
            refreshEndgame()
            result
        }
    }

    suspend fun claimWorldBossReward(bossId: String): Result<OnlineClaim> = tracked {
        val claim = api.claimWorldBossReward(bossId)
        syncRewardsQuietly()
        refreshEndgame()
        claim
    }

    suspend fun claimEventReward(eventId: String, itemId: String): Result<OnlineClaim> = tracked {
        val claim = api.claimEventReward(eventId, itemId)
        syncRewardsQuietly()
        refreshEndgame()
        claim
    }

    // ---- alliance --------------------------------------------------------------------------------
    private suspend fun allianceAction(action: String, extra: Json = emptyMap()): Result<Unit> = tracked {
        api.allianceAction(action, extra)
        refreshAlliance()
        Unit
    }

    suspend fun createAlliance(name: String, emblem: String, description: String) =
        allianceAction("CREATE", mapOf("name" to name, "emblem" to emblem, "description" to description))
    suspend fun joinAlliance(allianceId: String) = allianceAction("JOIN", mapOf("allianceId" to allianceId))
    suspend fun leaveAlliance() = allianceAction("LEAVE")
    suspend fun promoteMember(uid: String) = allianceAction("PROMOTE", mapOf("targetUid" to uid))
    suspend fun demoteMember(uid: String) = allianceAction("DEMOTE", mapOf("targetUid" to uid))
    suspend fun kickMember(uid: String) = allianceAction("KICK", mapOf("targetUid" to uid))
    suspend fun transferLeadership(uid: String) = allianceAction("TRANSFER", mapOf("targetUid" to uid))

    // ---- delivery with offline persistence --------------------------------------------------------
    /**
     * Persists the result BEFORE sending it. If the connection drops it stays queued and is retried by
     * [refreshAll]; a definitive server answer (accepted or refused) removes it. The server de-duplicates
     * by match/session id, so a retry after an unacknowledged success returns the original result.
     */
    private suspend fun <T> deliver(function: String, payload: Map<String, Any>, send: suspend () -> T): Result<T> {
        pending.add(function, payload)
        val result = tracked(send)
        val failure = result.exceptionOrNull()
        if (failure !is BackendException || !failure.isTransient) pending.remove(function, payload)
        return result
    }

    suspend fun retryPending() {
        for (item in pending.all()) {
            val p = item.payload
            try {
                when (item.function) {
                    "submitArenaResult" -> api.submitArenaResult(
                        p["matchId"] as String, p["outcome"] as String, (p["turns"] as Number).toInt(),
                        (p["damageDealt"] as Number).toInt(), (p["damageTaken"] as Number).toInt()
                    )
                    "submitWorldBossContribution" -> api.submitWorldBoss(p["sessionId"] as String, (p["damage"] as Number).toInt(), p["victory"] as Boolean)
                    "submitRaidContribution" -> api.submitRaid(p["sessionId"] as String, (p["damage"] as Number).toInt(), p["cleared"] as Boolean)
                    else -> Unit
                }
                pending.remove(item.function, p)
            } catch (e: CancellationException) {
                throw e
            } catch (e: BackendException) {
                if (!e.isTransient) pending.remove(item.function, p) else return
            } catch (e: Exception) {
                pending.remove(item.function, p) // corrupt entry: drop rather than loop forever
            }
        }
    }

    // ---- overlay: server state shown through the existing UI models --------------------------------
    fun overlay(base: PlayerEconomyState, s: OnlineSnapshot = snapshot.value): PlayerEconomyState {
        var out = base
        s.arena?.let { out = out.withArena(it, s.endgame?.arenaPoints) }
        s.endgame?.let { out = out.withEndgame(it) }
        s.alliance?.let { out = out.withAlliance(it, s.myUid) }
        return out
    }

    private fun PlayerEconomyState.withArena(a: OnlineArenaSnapshot, arenaPoints: Int?): PlayerEconomyState {
        val st = a.state
        val seasonNumber = st.seasonId.removePrefix("season_").toIntOrNull() ?: 1
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val heroName = HeroCatalog.findHero(selectedHeroId)?.name ?: "Hercules"
        return copy(
            arenaRating = st.rating,
            arenaPeakRating = st.peakRating,
            arenaWins = st.wins,
            arenaLosses = st.losses,
            arenaCurrentStreak = st.streak,
            arenaHighestStreak = st.bestStreak,
            arenaDailyAttempts = st.dailyAttempts,
            arenaPoints = arenaPoints ?: this.arenaPoints,
            arenaFirstWinClaimedDate = if (st.firstWinAvailable) "" else com.example.data.MythosDateUtil.getCurrentLocalDate(),
            arenaCurrentSeason = ArenaSeason(
                seasonId = st.seasonId,
                seasonNumber = seasonNumber,
                startDateMs = st.seasonEndsAt - 28L * 24 * 60 * 60 * 1000,
                endDateMs = st.seasonEndsAt,
                rating = st.rating,
                peakRating = st.peakRating,
                wins = st.wins,
                losses = st.losses,
                matches = st.wins + st.losses
            ),
            arenaSeasonRewardsClaimed = emptySet(),
            arenaMatchHistory = a.history.map { h ->
                ArenaMatchRecord(
                    matchId = h.matchId,
                    timestamp = h.completedAt,
                    dateFormatted = dateFormat.format(Date(h.completedAt)),
                    opponentName = h.opponentName,
                    opponentHeroName = h.opponentHeroName,
                    playerHeroName = heroName,
                    opponentArchetype = h.opponentArchetype,
                    result = runCatching { ArenaMatchResult.valueOf(h.outcome) }.getOrDefault(ArenaMatchResult.DEFEAT),
                    ratingBefore = h.ratingBefore,
                    ratingAfter = h.ratingAfter,
                    ratingChange = h.ratingChange,
                    turns = h.turns,
                    durationSeconds = maxOf(15, h.turns * 8),
                    goldAwarded = h.gold,
                    xpAwarded = h.xp,
                    shardsAwarded = h.cardShards,
                    arenaPointsAwarded = h.arenaPoints
                )
            }
        )
    }

    private fun PlayerEconomyState.withEndgame(e: OnlineEndgameState): PlayerEconomyState {
        val activeBossId = EndgameCatalog.getActiveWorldBoss().bossId
        val activeBoss = e.bosses.firstOrNull { it.bossId == activeBossId }
        val currentEvent = e.activeEventIds.firstOrNull()
        return copy(
            eventTokens = e.eventTokens,
            worldBossCurrentHp = activeBoss?.currentHp ?: worldBossCurrentHp,
            worldBossContributions = e.bosses.mapNotNull { b ->
                b.mine?.let { m ->
                    b.bossId to WorldBossContribution(
                        bossId = b.bossId,
                        totalDamage = m.totalDamage,
                        highestSingleHit = m.highestHit,
                        battlesCompleted = m.battles,
                        victories = m.victories,
                        participationCount = m.battles,
                        contributionScore = m.totalDamage + m.battles * 1_000L + m.victories * 5_000L,
                        claimedRewardTier = m.claimedTier
                    )
                }
            }.toMap(),
            raidDailyAttempts = e.raidRemaining,
            raidStageProgress = e.raidProgress,
            eventShopPurchases = currentEvent?.let { e.eventPurchases[it] } ?: emptyMap()
        )
    }

    private fun PlayerEconomyState.withAlliance(view: OnlineAllianceView, myUid: String?): PlayerEconomyState {
        val mine = view.alliance?.toAlliance(myUid)
        val others = view.open.filter { it.allianceId != mine?.allianceId }.map { it.toAlliance(null) }
        val me = mine?.members?.firstOrNull { it.playerId == LOCAL_PLAYER_ID }
        return copy(
            alliances = listOfNotNull(mine) + others,
            playerAllianceId = mine?.allianceId,
            playerProgress = playerProgress.copy(
                allianceId = mine?.allianceId,
                allianceName = mine?.name,
                allianceRole = me?.role?.title,
                allianceContribution = me?.contributionScore ?: 0L
            )
        )
    }

    /**
     * The existing UI identifies "me" by the placeholder id [LOCAL_PLAYER_ID]; other members keep their
     * real uid, which is what the promote/kick/transfer calls need. Open alliances only expose a count
     * and a leader, so the remaining slots are anonymous placeholders purely so memberCount is right.
     */
    private fun OnlineAlliance.toAlliance(myUid: String?): Alliance {
        val real = members.map { m ->
            AllianceMember(
                playerId = if (myUid != null && m.uid == myUid) LOCAL_PLAYER_ID else m.uid,
                name = m.displayName,
                combatPower = 0,
                role = runCatching { AllianceRole.valueOf(m.role) }.getOrDefault(AllianceRole.MEMBER),
                joinedDate = formatDate(m.joinedAt),
                battlesWon = m.battlesWon,
                damageDealt = m.damageDealt,
                contributionScore = m.contribution
            )
        }
        val roster = if (real.isNotEmpty()) real else List(memberCount) { i ->
            AllianceMember(
                playerId = if (i == 0) leaderUid else "hidden_${allianceId}_$i",
                name = if (i == 0) "Alliance Leader" else "Member",
                combatPower = 0,
                role = if (i == 0) AllianceRole.LEADER else AllianceRole.MEMBER
            )
        }
        return Alliance(
            allianceId = allianceId, name = name, emblem = emblem, description = description,
            level = level, xp = xp, createdDate = formatDate(createdAt), members = roster
        )
    }

    private fun formatDate(ms: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(ms))

    /** Leaderboard rows shaped for the existing Arena UI. */
    fun leaderboardEntries(s: OnlineSnapshot = snapshot.value): List<ArenaLeaderboardEntry> =
        s.leaderboard?.entries?.map { e ->
            val total = e.wins + e.losses
            ArenaLeaderboardEntry(
                rank = e.rank,
                playerName = e.displayName,
                avatarId = "avatar_default_hercules",
                rating = e.rating,
                tier = runCatching { ArenaRankTier.valueOf(e.tier) }.getOrDefault(ArenaRankTier.fromRating(e.rating)),
                wins = e.wins,
                losses = e.losses,
                winRate = if (total > 0) e.wins * 100f / total else 0f,
                peakRating = e.peakRating,
                isCurrentPlayer = e.uid == s.myUid
            )
        } ?: emptyList()

    companion object {
        const val LOCAL_PLAYER_ID = "player_local"

        fun userMessage(e: BackendException): String = when (e.code) {
            "unavailable", "deadline-exceeded" -> "You appear to be offline. Nothing was lost - your result will be sent when the connection returns."
            "unauthenticated" -> "Sign-in is required. Please try again."
            "resource-exhausted" -> "Too many requests. Please wait a moment."
            else -> e.message
        }

        fun create(context: Context): OnlineBackend {
            val identity = FirebasePlayerIdentity()
            val transport = FirebaseCallableTransport(identity)
            val api = GameBackendApi(transport)
            val rewardsStore = SharedPrefsStore(context, "mythos_online_rewards")
            return OnlineBackend(
                api = api,
                rewards = OnlineRewardSync(api, RewardLedger(rewardsStore), EconomyRewardSink(PlayerEconomyRepository.instance)),
                pending = PendingSubmissionStore(SharedPrefsStore(context, "mythos_online_pending")),
                uidProvider = { if (identity.isAuthenticated) identity.currentUid else null }
            )
        }
    }
}
