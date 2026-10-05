package com.example

import com.example.backend.online.BackendException
import com.example.backend.online.BackendTransport
import com.example.backend.online.GameBackendApi
import com.example.data.ActiveDeck
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Client half of the cross-language API contract (functions/src/game/apiContract.json). Drives every
 * GameBackendApi method and asserts it sends EXACTLY the documented keys. The server half
 * (apiContract.test.ts) asserts the real handlers accept those keys.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ApiContractTest {

    private class Recorder : BackendTransport {
        val sent = linkedMapOf<String, Set<String>>()
        override suspend fun call(function: String, payload: Map<String, Any?>): Map<String, Any?> {
            val discriminator = (payload["action"] ?: payload["kind"])?.let { ":$it" } ?: ""
            sent[function + discriminator] = payload.keys
            throw BackendException("internal", "stop") // non-transient: aborts without parsing a response
        }
    }

    @Test
    fun clientSendsExactlyTheContractedFields() = runBlocking {
        val recorder = Recorder()
        val api = GameBackendApi(recorder, newRequestId = { "contract-request-1" }, retryDelayMs = 0)
        val deck = ActiveDeck()

        suspend fun attempt(block: suspend () -> Unit) { runCatching { block() } }
        attempt { api.ensureProfile() }
        attempt { api.validateDeck(deck) }
        attempt { api.syncCollection(mapOf("c_x" to 1)) }
        attempt { api.startArenaMatch(deck) }
        attempt { api.submitArenaResult("arena_m", "VICTORY", 5, 10, 2) }
        attempt { api.claimArenaSeasonReward("season_1") }
        attempt { api.getArenaState() }
        attempt { api.getLeaderboard(50) }
        attempt { api.syncAlliance(true) }
        attempt { api.allianceAction("CREATE", mapOf("name" to "n", "emblem" to "e", "description" to "d")) }
        attempt { api.allianceAction("JOIN", mapOf("allianceId" to "a")) }
        attempt { api.allianceAction("LEAVE") }
        for (action in listOf("PROMOTE", "DEMOTE", "KICK", "TRANSFER")) attempt { api.allianceAction(action, mapOf("targetUid" to "u")) }
        attempt { api.startWorldBoss("world_boss_kronos") }
        attempt { api.startRaid("raid_olympus", 1, "normal") }
        attempt { api.submitWorldBoss("s", 1, true) }
        attempt { api.submitRaid("s", 1, true) }
        attempt { api.claimWorldBossReward("world_boss_kronos") }
        attempt { api.claimEventReward("event_wrath_of_olympus", "shop_gold_bounty") }
        attempt { api.getEndgameState() }
        attempt { api.getPendingRewards() }
        attempt { api.ackRewards(listOf("c1")) }

        val contract = JSONObject(File("../functions/src/game/apiContract.json").readText())
        val expected = contract.keys().asSequence().filterNot { it.startsWith("_") }.associateWith { name ->
            contract.getJSONArray(name).let { arr -> (0 until arr.length()).map { arr.getString(it) }.toSet() }
        }

        assertEquals("every contracted call must be exercised", expected.keys, recorder.sent.keys)
        expected.forEach { (name, keys) -> assertEquals("payload keys for $name", keys, recorder.sent[name]) }
        assertTrue(recorder.sent.values.none { "uid" in it }) // identity is never client-supplied
    }
}
