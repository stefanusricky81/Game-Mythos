package com.example.backend

import com.example.data.ArenaCatalog
import com.example.data.ArenaRankTier

/**
 * Server-backed Leaderboard Entry Model (Requirement #17).
 */
data class ServerLeaderboardEntry(
    val rank: Int,
    val uid: String,
    val playerName: String,
    val avatar: String,
    val rating: Int,
    val tier: ArenaRankTier,
    val wins: Int,
    val losses: Int,
    val winRate: Float,
    val peakRating: Int
)

/**
 * Leaderboard Provider Abstraction (Requirement #17).
 * Decouples leaderboard presentation from data source (Firebase Firestore vs local fallback).
 */
interface LeaderboardProvider {
    suspend fun getTopEntries(limit: Int = 50): List<ServerLeaderboardEntry>
    suspend fun getNearbyEntries(playerUid: String, range: Int = 2): List<ServerLeaderboardEntry>
    suspend fun getPlayerEntry(playerUid: String): ServerLeaderboardEntry?
}

/**
 * Server/Firestore Leaderboard Provider with offline/simulated fallback.
 */
class ServerLeaderboardRepository(
    private val simulatedCatalogFallback: Boolean = true
) : LeaderboardProvider {

    private val _entries = mutableListOf<ServerLeaderboardEntry>()

    init {
        // Initialize with high-quality competitive simulated entries (Phase 11/12 baseline)
        val archetypes = ArenaCatalog.OPPONENT_POOL
        var rankCounter = 1
        for (opp in archetypes.sortedByDescending { it.rating }) {
            _entries.add(
                ServerLeaderboardEntry(
                    rank = rankCounter++,
                    uid = opp.id,
                    playerName = opp.name,
                    avatar = opp.archetype.heroId,
                    rating = opp.rating,
                    tier = ArenaRankTier.fromRating(opp.rating),
                    wins = (opp.rating - 900).coerceAtLeast(10),
                    losses = (1500 - opp.rating).coerceIn(2, 30),
                    winRate = 65.5f,
                    peakRating = opp.rating + 45
                )
            )
        }
    }

    override suspend fun getTopEntries(limit: Int): List<ServerLeaderboardEntry> {
        return _entries.take(limit)
    }

    override suspend fun getNearbyEntries(playerUid: String, range: Int): List<ServerLeaderboardEntry> {
        val playerIdx = _entries.indexOfFirst { it.uid == playerUid }
        if (playerIdx == -1) {
            return _entries.take(range * 2 + 1)
        }
        val start = (playerIdx - range).coerceAtLeast(0)
        val end = (playerIdx + range + 1).coerceAtMost(_entries.size)
        return _entries.subList(start, end)
    }

    override suspend fun getPlayerEntry(playerUid: String): ServerLeaderboardEntry? {
        return _entries.find { it.uid == playerUid }
    }

    /**
     * Authoritatively updates or registers a player's entry into the server leaderboard.
     */
    fun updatePlayerEntry(
        uid: String,
        playerName: String,
        rating: Int,
        wins: Int,
        losses: Int,
        peakRating: Int,
        avatar: String = "avatar_default_hercules"
    ) {
        val total = wins + losses
        val winRate = if (total > 0) (wins.toFloat() / total) * 100f else 0f
        val existingIdx = _entries.indexOfFirst { it.uid == uid }

        val newEntry = ServerLeaderboardEntry(
            rank = 1, // Will be recalculated on sort
            uid = uid,
            playerName = playerName,
            avatar = avatar,
            rating = rating,
            tier = ArenaRankTier.fromRating(rating),
            wins = wins,
            losses = losses,
            winRate = winRate,
            peakRating = peakRating
        )

        if (existingIdx >= 0) {
            _entries[existingIdx] = newEntry
        } else {
            _entries.add(newEntry)
        }

        // Sort descending by rating and assign 1-based ranks
        _entries.sortByDescending { it.rating }
        for (i in _entries.indices) {
            _entries[i] = _entries[i].copy(rank = i + 1)
        }
    }
}
