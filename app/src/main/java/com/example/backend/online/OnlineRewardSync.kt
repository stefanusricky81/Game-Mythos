package com.example.backend.online

import android.content.Context
import com.example.monetization.PlayerEconomyRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

/** Tiny persistence seam so the online layer is unit-testable without Android storage. */
interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
}

class SharedPrefsStore(context: Context, name: String) : KeyValueStore {
    private val prefs = context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)
    override fun getString(key: String): String? = prefs.getString(key, null)
    // commit(), not apply(): the "applied" ledger must be on disk before a reward is granted.
    override fun putString(key: String, value: String) { prefs.edit().putString(key, value).commit() }
}

class InMemoryStore : KeyValueStore {
    private val map = HashMap<String, String>()
    override fun getString(key: String): String? = map[key]
    override fun putString(key: String, value: String) { map[key] = value }
}

/** Where server rewards land locally. Only gold, XP and hero shards live in the client economy. */
interface RewardSink {
    fun addGold(amount: Int)
    fun addPlayerXp(xp: Int)
    fun addHeroShards(heroId: String, shards: Int)
}

/** Applies rewards through PlayerEconomyRepository's existing public API (no payment code involved). */
class EconomyRewardSink(private val repository: PlayerEconomyRepository) : RewardSink {
    override fun addGold(amount: Int) = repository.addGold(amount)
    override fun addPlayerXp(xp: Int) { repository.addPlayerXp(xp) }
    override fun addHeroShards(heroId: String, shards: Int) = repository.addHeroShards(heroId, shards)
}

/** Remembers which server reward ids this device has already applied (bounded FIFO). */
class RewardLedger(private val store: KeyValueStore, private val maxEntries: Int = 500) {
    private val ids: LinkedHashSet<String> = LinkedHashSet<String>().also { set ->
        store.getString(KEY)?.let { raw ->
            runCatching { JSONArray(raw) }.getOrNull()?.let { arr -> for (i in 0 until arr.length()) set.add(arr.getString(i)) }
        }
    }

    @Synchronized fun isApplied(claimId: String): Boolean = ids.contains(claimId)

    @Synchronized fun markApplied(claimId: String) {
        ids.add(claimId)
        while (ids.size > maxEntries) ids.remove(ids.first())
        store.putString(KEY, JSONArray(ids.toList()).toString())
    }

    private companion object { const val KEY = "applied_claim_ids" }
}

/**
 * Applies server-granted rewards EXACTLY ONCE, even across crashes, reconnects and duplicate syncs.
 *
 * The server keeps every reward in an outbox (PENDING until acknowledged). Here: (1) a claim id is
 * recorded in the persisted ledger BEFORE it is granted, so a reward can never be granted twice;
 * (2) it is granted through [RewardSink]; (3) the server is told to mark it APPLIED. If the ack is
 * lost the reward stays PENDING on the server, the next sync sees it in the ledger, grants nothing
 * and just re-acknowledges. The one accepted edge: a process kill between (1) and (2) forfeits that
 * single reward rather than risking a duplicate.
 */
class OnlineRewardSync(
    private val api: GameBackendApi,
    private val ledger: RewardLedger,
    private val sink: RewardSink
) {
    private val mutex = Mutex()

    /** Returns how many rewards were newly granted on this device. */
    suspend fun sync(): Int = mutex.withLock {
        val pending = api.getPendingRewards()
        var granted = 0
        for (reward in pending) {
            if (ledger.isApplied(reward.claimId)) continue
            ledger.markApplied(reward.claimId)
            if (reward.gold > 0) sink.addGold(reward.gold)
            if (reward.xp > 0) sink.addPlayerXp(reward.xp)
            reward.heroShards.forEach { (heroId, shards) -> if (shards > 0) sink.addHeroShards(heroId, shards) }
            granted++
        }
        if (pending.isNotEmpty()) api.ackRewards(pending.map { it.claimId })
        granted
    }
}

/**
 * Results that were earned offline/while the connection dropped and still have to reach the server.
 * Persisted so they survive process death; the server de-duplicates by session/match id, so replaying
 * one is always safe.
 */
class PendingSubmissionStore(private val store: KeyValueStore) {
    data class Pending(val function: String, val payload: Map<String, Any>)

    @Synchronized fun all(): List<Pending> {
        val raw = store.getString(KEY) ?: return emptyList()
        val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val payload = o.optJSONObject("payload") ?: return@mapNotNull null
            Pending(o.getString("function"), payload.keys().asSequence().associateWith { payload.get(it) })
        }
    }

    @Synchronized fun add(function: String, payload: Map<String, Any>) {
        val keyOf = { p: Pending -> p.function + "|" + (p.payload["matchId"] ?: p.payload["sessionId"]) }
        val next = Pending(function, payload)
        write(all().filterNot { keyOf(it) == keyOf(next) } + next)
    }

    @Synchronized fun remove(function: String, payload: Map<String, Any>) {
        val id = payload["matchId"] ?: payload["sessionId"]
        write(all().filterNot { it.function == function && (it.payload["matchId"] ?: it.payload["sessionId"]) == id })
    }

    private fun write(items: List<Pending>) {
        val arr = JSONArray()
        items.forEach { arr.put(JSONObject().put("function", it.function).put("payload", JSONObject(it.payload))) }
        store.putString(KEY, arr.toString())
    }

    private companion object { const val KEY = "pending_submissions" }
}
