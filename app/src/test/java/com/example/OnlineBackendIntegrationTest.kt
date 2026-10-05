package com.example

import com.example.backend.BackendMode
import com.example.backend.MythosBackend
import com.example.backend.online.AllianceGateway
import com.example.backend.online.BackendException
import com.example.backend.online.BackendTransport
import com.example.backend.online.GameBackendApi
import com.example.backend.online.InMemoryStore
import com.example.backend.online.OnlineBackend
import com.example.backend.online.OnlineRewardSync
import com.example.backend.online.PendingSubmissionStore
import com.example.backend.online.RewardLedger
import com.example.backend.online.RewardSink
import com.example.data.ArenaCatalog
import com.example.data.BattleEncounterConfig
import com.example.data.Hero
import com.example.monetization.PlayerEconomyRepository
import com.example.viewmodel.BattleViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private typealias Payload = Map<String, Any?>

/** Records every call and answers through a scripted "server". */
private class FakeServer(var online: Boolean = true) : BackendTransport {
    val calls = mutableListOf<Pair<String, Payload>>()
    var pendingRewards = mutableListOf<Payload>()
    var submitFailure: BackendException? = null
    var lostAck = false
    var allianceError: BackendException? = null

    fun called(function: String) = calls.filter { it.first == function }

    override suspend fun call(function: String, payload: Payload): Payload {
        calls += function to payload
        if (!online) throw BackendException("unavailable", "offline")
        return when (function) {
            "submitArenaResult" -> {
                submitFailure?.let { throw it }
                arenaResult(payload["matchId"] as String, payload["outcome"] as String)
            }
            "getArenaState" -> mapOf("success" to true, "state" to arenaState(), "history" to emptyList<Any>(), "claimableSeasons" to emptyList<Any>())
            "getPendingRewards" -> mapOf("success" to true, "rewards" to pendingRewards.toList())
            "ackRewards" -> {
                if (lostAck) throw BackendException("unavailable", "ack lost")
                val ids = (payload["claimIds"] as List<*>).toSet()
                val before = pendingRewards.size
                pendingRewards.removeAll { it["claimId"] in ids }
                mapOf("success" to true, "acknowledged" to before - pendingRewards.size)
            }
            "allianceAction" -> {
                allianceError?.let { throw it }
                mapOf("success" to true, "action" to payload["action"])
            }
            "syncAlliance" -> mapOf("success" to true, "alliance" to null, "myRole" to null, "openAlliances" to emptyList<Any>())
            "getEndgameState", "getLeaderboard" -> emptyMap()
            else -> error("Unexpected call $function")
        }
    }

    fun queueReward(claimId: String, gold: Int, xp: Int) {
        pendingRewards += mapOf("claimId" to claimId, "source" to "ARENA_MATCH", "gold" to gold, "xp" to xp, "heroShards" to emptyMap<String, Int>())
    }

    private fun arenaState() = mapOf(
        "rating" to 1016, "peakRating" to 1016, "tier" to "SILVER", "wins" to 1, "losses" to 0, "streak" to 1,
        "bestStreak" to 1, "dailyAttempts" to 4, "maxDailyAttempts" to 5, "firstWinAvailable" to false,
        "seasonId" to "season_2", "seasonEndsAt" to 1_795_000_000_000L, "activeMatchId" to null
    )

    private fun arenaResult(matchId: String, outcome: String) = mapOf(
        "success" to true, "matchId" to matchId, "outcome" to outcome, "ratingBefore" to 1000, "ratingAfter" to 1016,
        "ratingDelta" to 16, "tierBefore" to "SILVER", "tierAfter" to "SILVER",
        "rewards" to mapOf("gold" to 1500, "xp" to 250, "cardShards" to 25, "arenaPoints" to 100, "isFirstWin" to true,
            "streak" to 1, "streakBonusGold" to 0, "streakBonusPoints" to 0),
        "claimId" to "arena_$matchId", "remainingAttempts" to 4, "arena" to arenaState()
    )
}

private class RecordingSink : RewardSink {
    var gold = 0
    var xp = 0
    val heroShards = mutableMapOf<String, Int>()
    override fun addGold(amount: Int) { gold += amount }
    override fun addPlayerXp(xp: Int) { this.xp += xp }
    override fun addHeroShards(heroId: String, shards: Int) { heroShards.merge(heroId, shards, Int::plus) }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OnlineBackendIntegrationTest {

    private lateinit var server: FakeServer
    private lateinit var sink: RecordingSink
    private lateinit var pending: PendingSubmissionStore
    private var requestCounter = 0

    private fun backend(): OnlineBackend {
        val api = GameBackendApi(server, newRequestId = { "request-${++requestCounter}-abcdef" }, maxAttempts = 3, retryDelayMs = 0)
        return OnlineBackend(api, OnlineRewardSync(api, RewardLedger(InMemoryStore()), sink), pending, uidProvider = { "uid_me" })
    }

    @Before
    fun setUp() {
        server = FakeServer()
        sink = RecordingSink()
        pending = PendingSubmissionStore(InMemoryStore())
        MythosBackend.installForTesting(backend(), BackendMode.ONLINE_AUTHORITATIVE)
    }

    @After
    fun tearDown() {
        MythosBackend.installForTesting(null, BackendMode.LOCAL_DEVELOPMENT)
    }

    private fun arenaConfig(sessionId: String?) = BattleEncounterConfig(
        encounterName = "Arena: test",
        enemyHero = Hero.createAres(),
        isArenaMatch = true,
        arenaOpponent = ArenaCatalog.OPPONENT_POOL.first(),
        onlineSessionId = sessionId
    )

    @Test
    fun onlineArenaVictoryGoesVmToServerAndShowsTheServersResult() {
        val goldBefore = PlayerEconomyRepository.instance.economyState.value.gold
        server.queueReward("arena_arena_m1", gold = 1500, xp = 250)

        val vm = BattleViewModel()
        assertTrue(vm.startNewBattle(encounterConfig = arenaConfig("arena_m1")))
        vm.debugForceVictory()

        val submits = server.called("submitArenaResult")
        assertEquals(1, submits.size)
        // Only the claimed outcome and battle stats are sent: no reward, rating or opponent fields.
        assertEquals(setOf("matchId", "outcome", "turns", "damageDealt", "damageTaken"), submits[0].second.keys)
        assertEquals("arena_m1", submits[0].second["matchId"])
        assertEquals("VICTORY", submits[0].second["outcome"])

        val ui = vm.uiState.value
        assertFalse(ui.isSubmittingOnlineResult)
        val summary = ui.arenaBattleResultSummary
        assertNotNull(summary)
        assertEquals(1016, summary!!.ratingAfter)
        assertEquals(1500, summary.goldAwarded)

        // The reward arrived through the server outbox, once, and the local wallet was not credited directly.
        assertEquals(1500, sink.gold)
        assertEquals(250, sink.xp)
        assertEquals(1, server.called("ackRewards").size)
        assertEquals(goldBefore, PlayerEconomyRepository.instance.economyState.value.gold)
    }

    @Test
    fun offlineResultIsQueuedNotGrantedAndDeliveredExactlyOnceOnReconnect() = runBlocking {
        val goldBefore = PlayerEconomyRepository.instance.economyState.value.gold
        server.online = false

        val vm = BattleViewModel()
        vm.startNewBattle(encounterConfig = arenaConfig("arena_m3"))
        vm.debugForceVictory()

        val ui = vm.uiState.value
        assertNull("No authoritative result may be invented locally", ui.arenaBattleResultSummary)
        assertNotNull(ui.onlineResultMessage)
        assertTrue(ui.onlineResultMessage!!.contains("offline", ignoreCase = true))
        assertEquals(goldBefore, PlayerEconomyRepository.instance.economyState.value.gold)
        assertEquals(1, pending.all().size)
        assertEquals(0, sink.gold)

        // Connection returns: the queued result is delivered and the reward is applied one time.
        server.online = true
        server.queueReward("arena_arena_m3", gold = 1500, xp = 250)
        val online = MythosBackend.online
        online.retryPending()
        online.refreshAll()
        online.refreshAll()

        assertEquals(0, pending.all().size)
        assertEquals(1500, sink.gold)
        assertEquals("a second reconnect must not double-grant", 250, sink.xp)
    }

    @Test
    fun aRewardWhoseAckWasLostIsNeverAppliedTwice() = runBlocking {
        server.lostAck = true
        server.queueReward("arena_dup", gold = 700, xp = 10)
        val online = MythosBackend.online

        online.refreshAll()
        online.refreshAll()
        online.refreshAll()
        assertEquals(700, sink.gold)

        server.lostAck = false
        online.refreshAll()
        assertEquals(700, sink.gold)
        assertTrue(server.pendingRewards.isEmpty())
    }

    @Test
    fun aRefusedSubmissionIsDroppedNotRetriedForever() = runBlocking {
        server.submitFailure = BackendException("failed-precondition", "Result submitted faster than a real battle can finish.")
        val vm = BattleViewModel()
        vm.startNewBattle(encounterConfig = arenaConfig("arena_m4"))
        vm.debugForceVictory()

        assertEquals("Result submitted faster than a real battle can finish.", vm.uiState.value.onlineResultMessage)
        assertEquals(0, pending.all().size)
    }

    @Test
    fun apiRetriesTransientFailuresWithTheSameRequestIdOnly() = runBlocking {
        var attempts = 0
        val ids = mutableListOf<Any?>()
        val flaky = object : BackendTransport {
            override suspend fun call(function: String, payload: Payload): Payload {
                ids += payload["requestId"]
                attempts++
                if (attempts < 3) throw BackendException("unavailable", "blip")
                return mapOf("success" to true, "claimId" to "c1", "tier" to "PARTICIPATION")
            }
        }
        val api = GameBackendApi(flaky, newRequestId = { "one-logical-request" }, maxAttempts = 3, retryDelayMs = 0)
        assertEquals("c1", api.claimWorldBossReward("world_boss_kronos").claimId)
        assertEquals(3, attempts)
        assertEquals(setOf<Any?>("one-logical-request"), ids.toSet())

        var refusals = 0
        val refusing = object : BackendTransport {
            override suspend fun call(function: String, payload: Payload): Payload { refusals++; throw BackendException("already-exists", "claimed") }
        }
        val failure = runCatching { GameBackendApi(refusing, retryDelayMs = 0).claimWorldBossReward("world_boss_kronos") }.exceptionOrNull()
        assertEquals("already-exists", (failure as BackendException).code)
        assertEquals("a definitive refusal is not retried", 1, refusals)
    }

    @Test
    fun overlayShowsServerStateThroughTheExistingEconomyModel() = runBlocking {
        val online = MythosBackend.online
        online.refreshArena()
        val shown = online.overlay(PlayerEconomyRepository.instance.economyState.value)
        assertEquals(1016, shown.arenaRating)
        assertEquals(4, shown.arenaDailyAttempts)
        assertEquals("season_2", shown.arenaCurrentSeason.seasonId)
    }

    @Test
    fun allianceActionsAreSentToTheServerAndRefusalsReachTheUser() = runBlocking {
        assertTrue(AllianceGateway.join("al_123").isSuccess)
        val call = server.called("allianceAction").single().second
        assertEquals("JOIN", call["action"])
        assertEquals("al_123", call["allianceId"])
        assertNotNull(call["requestId"])

        server.allianceError = BackendException("failed-precondition", "Alliance is full (maximum 20 members).")
        val refused = AllianceGateway.join("al_123")
        assertEquals("Alliance is full (maximum 20 members).", refused.exceptionOrNull()?.message)

        server.allianceError = null
        assertTrue(AllianceGateway.kick("uid_other").isSuccess)
        assertEquals("uid_other", server.called("allianceAction").last().second["targetUid"])
    }

    @Test
    fun localModeNeverTouchesTheBackend() {
        MythosBackend.installForTesting(null, BackendMode.LOCAL_DEVELOPMENT)
        val vm = BattleViewModel()
        vm.startNewBattle(encounterConfig = arenaConfig(null))
        vm.debugForceVictory()
        assertTrue(server.calls.isEmpty())
        assertNotNull("offline arena still produces its local result", vm.uiState.value.arenaBattleResultSummary)
    }
}
