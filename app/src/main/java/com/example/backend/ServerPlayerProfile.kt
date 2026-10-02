package com.example.backend

/**
 * Server-authoritative Player Profile Model (Requirement #7).
 *
 * Represents the server-owned record for players/{uid}.
 * Mutations must be executed via explicit operations rather than raw client overwrites.
 */
data class ServerPlayerProfile(
    val uid: String,
    val playerLevel: Int = 1,
    val playerXp: Long = 0L,
    val gold: Long = 1000L,
    val mythGems: Long = 100L,
    val cardShards: Long = 50L,
    val heroShards: Long = 0L,
    val arenaPoints: Long = 0L,
    val lifetimeBattles: Int = 0,
    val lifetimeWins: Int = 0,
    val lifetimeLosses: Int = 0,
    val lifetimeDamage: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(uid.isNotBlank()) { "UID cannot be blank" }
        require(playerLevel >= 1) { "Player level cannot be less than 1" }
        require(playerXp >= 0) { "Player XP cannot be negative" }
        require(gold >= 0) { "Gold cannot be negative" }
        require(mythGems >= 0) { "Myth Gems cannot be negative" }
        require(cardShards >= 0) { "Card Shards cannot be negative" }
        require(heroShards >= 0) { "Hero Shards cannot be negative" }
        require(arenaPoints >= 0) { "Arena Points cannot be negative" }
    }

    /**
     * Awards gold with overflow and negative validation.
     */
    fun awardGold(amount: Long, now: Long = System.currentTimeMillis()): ServerPlayerProfile {
        require(amount >= 0) { "Gold award amount must be non-negative, got $amount" }
        val newGold = safeAdd(gold, amount)
        return copy(gold = newGold, updatedAt = now)
    }

    /**
     * Spends gold with balance validation.
     */
    fun spendGold(amount: Long, now: Long = System.currentTimeMillis()): ServerPlayerProfile {
        require(amount >= 0) { "Gold spend amount must be non-negative, got $amount" }
        check(gold >= amount) { "Insufficient gold: required $amount, current balance $gold" }
        return copy(gold = gold - amount, updatedAt = now)
    }

    /**
     * Awards myth gems.
     */
    fun awardGems(amount: Long, now: Long = System.currentTimeMillis()): ServerPlayerProfile {
        require(amount >= 0) { "Gems award amount must be non-negative, got $amount" }
        val newGems = safeAdd(mythGems, amount)
        return copy(mythGems = newGems, updatedAt = now)
    }

    /**
     * Spends myth gems with balance validation.
     */
    fun spendGems(amount: Long, now: Long = System.currentTimeMillis()): ServerPlayerProfile {
        require(amount >= 0) { "Gems spend amount must be non-negative, got $amount" }
        check(mythGems >= amount) { "Insufficient myth gems: required $amount, balance $mythGems" }
        return copy(mythGems = mythGems - amount, updatedAt = now)
    }

    /**
     * Awards card shards.
     */
    fun awardCardShards(amount: Long, now: Long = System.currentTimeMillis()): ServerPlayerProfile {
        require(amount >= 0) { "Card shards award amount must be non-negative, got $amount" }
        return copy(cardShards = safeAdd(cardShards, amount), updatedAt = now)
    }

    /**
     * Awards hero shards.
     */
    fun awardHeroShards(amount: Long, now: Long = System.currentTimeMillis()): ServerPlayerProfile {
        require(amount >= 0) { "Hero shards award amount must be non-negative, got $amount" }
        return copy(heroShards = safeAdd(heroShards, amount), updatedAt = now)
    }

    /**
     * Awards arena points.
     */
    fun awardArenaPoints(amount: Long, now: Long = System.currentTimeMillis()): ServerPlayerProfile {
        require(amount >= 0) { "Arena points award amount must be non-negative, got $amount" }
        return copy(arenaPoints = safeAdd(arenaPoints, amount), updatedAt = now)
    }

    /**
     * Awards player XP and recalculates player level according to progression curve.
     */
    fun awardPlayerXp(amount: Long, now: Long = System.currentTimeMillis()): ServerPlayerProfile {
        require(amount >= 0) { "XP award amount must be non-negative, got $amount" }
        val newXp = safeAdd(playerXp, amount)
        val newLevel = calculateLevelFromXp(newXp)
        return copy(playerXp = newXp, playerLevel = newLevel, updatedAt = now)
    }

    /**
     * Records battle completion statistics.
     */
    fun recordBattle(isVictory: Boolean, damageDealt: Long, now: Long = System.currentTimeMillis()): ServerPlayerProfile {
        return copy(
            lifetimeBattles = lifetimeBattles + 1,
            lifetimeWins = if (isVictory) lifetimeWins + 1 else lifetimeWins,
            lifetimeLosses = if (!isVictory) lifetimeLosses + 1 else lifetimeLosses,
            lifetimeDamage = safeAdd(lifetimeDamage, maxOf(0L, damageDealt)),
            updatedAt = now
        )
    }

    companion object {
        fun calculateLevelFromXp(xp: Long): Int {
            // Level formula: 1 + floor(xp / 1000)
            return (1 + (xp / 1000L)).toInt().coerceIn(1, 100)
        }

        private fun safeAdd(a: Long, b: Long): Long {
            val sum = a + b
            return if (sum < 0) Long.MAX_VALUE else sum
        }
    }
}
