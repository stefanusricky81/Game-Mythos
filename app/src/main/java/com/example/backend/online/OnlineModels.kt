package com.example.backend.online

import com.example.data.ArenaOpponent
import com.example.data.ArenaOpponentArchetype

/**
 * Typed views of the Cloud Function responses. The server decides every number in here; the client
 * only displays it (and applies the gold/XP/hero-shard parts of a reward through the reward outbox).
 */
typealias Json = Map<String, Any?>

// ---- tolerant, validating readers (Firebase decodes JSON numbers as Int, Long or Double) ----------
internal fun Json.str(key: String): String =
    this[key] as? String ?: throw BackendException("internal", "Malformed server response: '$key' missing.")

internal fun Json.strOrNull(key: String): String? = this[key] as? String
internal fun Json.int(key: String, default: Int = 0): Int = (this[key] as? Number)?.toInt() ?: default
internal fun Json.long(key: String, default: Long = 0L): Long = (this[key] as? Number)?.toLong() ?: default
internal fun Json.bool(key: String, default: Boolean = false): Boolean = this[key] as? Boolean ?: default

@Suppress("UNCHECKED_CAST")
internal fun Json.obj(key: String): Json? = this[key] as? Json

@Suppress("UNCHECKED_CAST")
internal fun Json.objList(key: String): List<Json> = (this[key] as? List<*>)?.mapNotNull { it as? Json } ?: emptyList()

internal fun Json.strList(key: String): List<String> = (this[key] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

internal fun Json.intMap(key: String): Map<String, Int> =
    (this[key] as? Map<*, *>)?.entries?.mapNotNull { (k, v) -> (v as? Number)?.let { k.toString() to it.toInt() } }?.toMap() ?: emptyMap()

// ---- Arena ----------------------------------------------------------------------------------------
data class OnlineOpponent(
    val id: String, val name: String, val heroId: String, val heroName: String, val archetype: String,
    val title: String, val rating: Int, val combatPower: Int, val deck: List<String>
) {
    companion object {
        fun from(j: Json) = OnlineOpponent(
            j.str("id"), j.str("name"), j.str("heroId"), j.str("heroName"), j.str("archetype"),
            j.strOrNull("title") ?: "", j.int("rating"), j.int("combatPower"), j.strList("deck")
        )
    }
}

/** The opponent the SERVER chose, shaped for the existing Arena battle setup. */
fun OnlineOpponent.toArenaOpponent(): ArenaOpponent = ArenaOpponent(
    id = id, name = name, heroId = heroId, heroName = heroName,
    archetype = runCatching { ArenaOpponentArchetype.valueOf(archetype) }.getOrDefault(ArenaOpponentArchetype.OLYMPUS_VANGUARD),
    rating = rating, combatPower = combatPower, deckCardIds = deck, title = title
)

data class OnlineArenaMatch(
    val matchId: String, val opponent: OnlineOpponent, val ratingBefore: Int, val remainingAttempts: Int,
    val resumed: Boolean, val expiresAt: Long
) {
    companion object {
        fun from(j: Json) = OnlineArenaMatch(
            j.str("matchId"), OnlineOpponent.from(j.obj("opponent") ?: throw BackendException("internal", "Malformed server response: opponent missing.")),
            j.int("ratingBefore"), j.int("remainingAttempts"), j.bool("resumed"), j.long("expiresAt")
        )
    }
}

data class OnlineArenaRewards(
    val gold: Int, val xp: Int, val cardShards: Int, val arenaPoints: Int, val isFirstWin: Boolean,
    val streak: Int, val streakBonusGold: Int, val streakBonusPoints: Int
) {
    companion object {
        fun from(j: Json) = OnlineArenaRewards(
            j.int("gold"), j.int("xp"), j.int("cardShards"), j.int("arenaPoints"), j.bool("isFirstWin"),
            j.int("streak"), j.int("streakBonusGold"), j.int("streakBonusPoints")
        )
    }
}

data class OnlineArenaState(
    val rating: Int, val peakRating: Int, val tier: String, val wins: Int, val losses: Int, val streak: Int,
    val bestStreak: Int, val dailyAttempts: Int, val maxDailyAttempts: Int, val firstWinAvailable: Boolean,
    val seasonId: String, val seasonEndsAt: Long, val activeMatchId: String?
) {
    companion object {
        fun from(j: Json) = OnlineArenaState(
            j.int("rating"), j.int("peakRating"), j.str("tier"), j.int("wins"), j.int("losses"), j.int("streak"),
            j.int("bestStreak"), j.int("dailyAttempts"), j.int("maxDailyAttempts"), j.bool("firstWinAvailable"),
            j.str("seasonId"), j.long("seasonEndsAt"), j.strOrNull("activeMatchId")
        )
    }
}

data class OnlineArenaResult(
    val matchId: String, val outcome: String, val ratingBefore: Int, val ratingAfter: Int, val ratingDelta: Int,
    val tierBefore: String, val tierAfter: String, val rewards: OnlineArenaRewards, val claimId: String,
    val remainingAttempts: Int, val arena: OnlineArenaState?, val replay: Boolean
) {
    companion object {
        fun from(j: Json) = OnlineArenaResult(
            j.str("matchId"), j.str("outcome"), j.int("ratingBefore"), j.int("ratingAfter"), j.int("ratingDelta"),
            j.str("tierBefore"), j.str("tierAfter"),
            OnlineArenaRewards.from(j.obj("rewards") ?: throw BackendException("internal", "Malformed server response: rewards missing.")),
            j.str("claimId"), j.int("remainingAttempts"), j.obj("arena")?.let { OnlineArenaState.from(it) },
            j.bool("idempotentReplay")
        )
    }
}

data class OnlineMatchRecord(
    val matchId: String, val opponentName: String, val opponentHeroName: String, val opponentArchetype: String,
    val outcome: String, val ratingBefore: Int, val ratingAfter: Int, val ratingChange: Int, val turns: Int,
    val gold: Int, val xp: Int, val cardShards: Int, val arenaPoints: Int, val completedAt: Long
) {
    companion object {
        fun from(j: Json): OnlineMatchRecord {
            val opponent = j.obj("opponent") ?: emptyMap()
            val rewards = j.obj("rewards") ?: emptyMap()
            return OnlineMatchRecord(
                j.str("matchId"), opponent.strOrNull("name") ?: "Unknown", opponent.strOrNull("heroName") ?: "",
                opponent.strOrNull("archetype") ?: "", j.str("outcome"), j.int("ratingBefore"), j.int("ratingAfter"),
                j.int("ratingDelta"), j.int("turns"), rewards.int("gold"), rewards.int("xp"), rewards.int("cardShards"),
                rewards.int("arenaPoints"), j.long("completedAt")
            )
        }
    }
}

data class OnlineClaimableSeason(val seasonId: String, val finalRating: Int, val tier: String)

data class OnlineArenaSnapshot(
    val state: OnlineArenaState, val history: List<OnlineMatchRecord>, val claimable: List<OnlineClaimableSeason>
) {
    companion object {
        fun from(j: Json) = OnlineArenaSnapshot(
            OnlineArenaState.from(j.obj("state") ?: throw BackendException("internal", "Malformed server response: state missing.")),
            j.objList("history").map { OnlineMatchRecord.from(it) },
            j.objList("claimableSeasons").map { OnlineClaimableSeason(it.str("seasonId"), it.int("finalRating"), it.str("tier")) }
        )
    }
}

data class OnlineLeaderboardEntry(
    val rank: Int, val uid: String, val displayName: String, val rating: Int, val peakRating: Int,
    val wins: Int, val losses: Int, val tier: String
) {
    companion object {
        fun from(j: Json) = OnlineLeaderboardEntry(
            j.int("rank"), j.str("uid"), j.str("displayName"), j.int("rating"), j.int("peakRating"),
            j.int("wins"), j.int("losses"), j.str("tier")
        )
    }
}

data class OnlineLeaderboard(val seasonId: String, val entries: List<OnlineLeaderboardEntry>, val me: OnlineLeaderboardEntry?) {
    companion object {
        fun from(j: Json) = OnlineLeaderboard(
            j.str("seasonId"), j.objList("entries").map { OnlineLeaderboardEntry.from(it) }, j.obj("me")?.let { OnlineLeaderboardEntry.from(it) }
        )
    }
}

data class OnlineSeasonClaim(val seasonId: String, val tier: String, val claimId: String, val gold: Int, val cardShards: Int, val heroShards: Int) {
    companion object {
        fun from(j: Json): OnlineSeasonClaim {
            val reward = j.obj("reward") ?: emptyMap()
            return OnlineSeasonClaim(j.str("seasonId"), j.str("tier"), j.str("claimId"), reward.int("gold"), reward.int("cardShards"), reward.int("heroShards"))
        }
    }
}

// ---- Endgame --------------------------------------------------------------------------------------
data class OnlineBossContribution(
    val totalDamage: Long, val battles: Int, val victories: Int, val highestHit: Long, val claimed: Boolean, val claimedTier: String?
)

data class OnlineBossState(
    val bossId: String, val maxHp: Long, val currentHp: Long, val defeated: Boolean, val active: Boolean,
    val rewardsUnlocked: Boolean, val mine: OnlineBossContribution?
)

data class OnlineEndgameState(
    val eventTokens: Int, val cardShards: Int, val arenaPoints: Int,
    val worldBossRemaining: Int, val worldBossMax: Int, val raidRemaining: Int, val raidMax: Int,
    val raidProgress: Map<String, Int>, val bosses: List<OnlineBossState>,
    val activeEventIds: List<String>, val eventPurchases: Map<String, Map<String, Int>>
) {
    companion object {
        fun from(j: Json): OnlineEndgameState {
            val wallet = j.obj("wallet") ?: emptyMap()
            val attempts = j.obj("attempts") ?: emptyMap()
            val purchases = (j["eventPurchases"] as? Map<*, *>)?.entries?.associate { (k, v) ->
                k.toString() to ((v as? Map<*, *>)?.entries?.mapNotNull { (ik, iv) -> (iv as? Number)?.let { ik.toString() to it.toInt() } }?.toMap() ?: emptyMap())
            } ?: emptyMap()
            return OnlineEndgameState(
                wallet.int("eventTokens"), wallet.int("cardShards"), wallet.int("arenaPoints"),
                attempts.int("worldBossRemaining"), attempts.int("worldBossMax"), attempts.int("raidRemaining"), attempts.int("raidMax"),
                j.intMap("raidProgress"),
                j.objList("worldBosses").map { b ->
                    OnlineBossState(
                        b.str("bossId"), b.long("maxHp"), b.long("currentHp"), b.bool("defeated"), b.bool("active"), b.bool("rewardsUnlocked"),
                        b.obj("mine")?.let { OnlineBossContribution(it.long("totalDamage"), it.int("battles"), it.int("victories"), it.long("highestHit"), it.bool("claimed"), it.strOrNull("claimedTier")) }
                    )
                },
                j.objList("activeEvents").map { it.str("eventId") },
                purchases
            )
        }
    }
}

/** A server-issued attempt: its [enemyHp] is what the client must fight; submitting is bound to [sessionId]. */
data class OnlineSession(
    val sessionId: String, val kind: String, val targetId: String, val stage: Int, val difficulty: String,
    val enemyHp: Int, val attemptsRemaining: Int
) {
    companion object {
        fun from(j: Json) = OnlineSession(
            j.str("sessionId"), j.str("kind"), j.strOrNull("bossId") ?: j.strOrNull("raidId") ?: "", j.int("stage"),
            j.strOrNull("difficulty") ?: "", j.int("enemyHp"), j.int("attemptsRemaining")
        )
    }
}

data class OnlineBossResult(
    val sessionId: String, val bossId: String, val damageApplied: Long, val bossHpRemaining: Long, val bossDefeated: Boolean,
    val gold: Int, val xp: Int, val eventTokens: Int, val claimId: String, val replay: Boolean
) {
    companion object {
        fun from(j: Json): OnlineBossResult {
            val rewards = j.obj("rewards") ?: emptyMap()
            return OnlineBossResult(
                j.str("sessionId"), j.str("bossId"), j.long("damageApplied"), j.long("bossHpRemaining"), j.bool("bossDefeated"),
                rewards.int("gold"), rewards.int("xp"), rewards.int("eventTokens"), j.str("claimId"), j.bool("idempotentReplay")
            )
        }
    }
}

data class OnlineRaidResult(
    val sessionId: String, val raidId: String, val stage: Int, val cleared: Boolean, val progress: Int,
    val gold: Int, val xp: Int, val eventTokens: Int, val claimId: String, val replay: Boolean
) {
    companion object {
        fun from(j: Json): OnlineRaidResult {
            val rewards = j.obj("rewards") ?: emptyMap()
            return OnlineRaidResult(
                j.str("sessionId"), j.str("raidId"), j.int("stage"), j.bool("cleared"), j.int("progress"),
                rewards.int("gold"), rewards.int("xp"), rewards.int("eventTokens"), j.str("claimId"), j.bool("idempotentReplay")
            )
        }
    }
}

data class OnlineClaim(val claimId: String, val tier: String?, val tokensRemaining: Int?) {
    companion object {
        fun from(j: Json) = OnlineClaim(j.str("claimId"), j.strOrNull("tier"), (j["tokensRemaining"] as? Number)?.toInt())
    }
}

// ---- Alliance -------------------------------------------------------------------------------------
data class OnlineAllianceMember(
    val uid: String, val displayName: String, val role: String, val joinedAt: Long,
    val contribution: Long, val battlesWon: Int, val damageDealt: Long
)

data class OnlineAlliance(
    val allianceId: String, val name: String, val emblem: String, val description: String,
    val level: Int, val xp: Int, val memberCount: Int, val leaderUid: String, val createdAt: Long,
    val members: List<OnlineAllianceMember>
) {
    companion object {
        fun from(j: Json) = OnlineAlliance(
            j.str("allianceId"), j.str("name"), j.str("emblem"), j.strOrNull("description") ?: "",
            j.int("level", 1), j.int("xp"), j.int("memberCount"), j.strOrNull("leaderUid") ?: "", j.long("createdAt"),
            j.objList("members").map {
                OnlineAllianceMember(it.str("uid"), it.strOrNull("displayName") ?: "Hero", it.str("role"), it.long("joinedAt"), it.long("contribution"), it.int("battlesWon"), it.long("damageDealt"))
            }
        )
    }
}

data class OnlineAllianceView(val alliance: OnlineAlliance?, val myRole: String?, val open: List<OnlineAlliance>) {
    companion object {
        fun from(j: Json) = OnlineAllianceView(
            j.obj("alliance")?.let { OnlineAlliance.from(it) }, j.strOrNull("myRole"), j.objList("openAlliances").map { OnlineAlliance.from(it) }
        )
    }
}

// ---- Reward outbox --------------------------------------------------------------------------------
data class OnlinePendingReward(val claimId: String, val source: String, val gold: Int, val xp: Int, val heroShards: Map<String, Int>) {
    companion object {
        fun from(j: Json) = OnlinePendingReward(j.str("claimId"), j.strOrNull("source") ?: "", j.int("gold"), j.int("xp"), j.intMap("heroShards"))
    }
}
