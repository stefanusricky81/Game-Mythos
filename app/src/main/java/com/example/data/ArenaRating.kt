package com.example.data

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Centralized Arena Rating & ELO Calculation Engine (Requirement #4, #5, #9).
 * Single authoritative source of truth for competitive rating math.
 */
object ArenaRatingCalculator {

    const val STARTING_RATING = 1000
    const val MINIMUM_RATING = 0
    const val K_FACTOR = 32

    /**
     * Calculates the expected score using standard ELO formula (Requirement #4):
     * E = 1 / (1 + 10^((opponentRating - playerRating) / 400))
     */
    fun calculateExpectedScore(playerRating: Int, opponentRating: Int): Double {
        val exponent = (opponentRating - playerRating).toDouble() / 400.0
        return 1.0 / (1.0 + 10.0.pow(exponent))
    }

    /**
     * Calculates the deterministic rating change based on match outcome (Requirement #4):
     * Δ = K × (actualScore - expectedScore)
     *
     * Result guarantees:
     * - Win increases rating (minimum +1 even against much weaker opponents)
     * - Loss decreases rating (minimum -1 even against much stronger opponents)
     * - Rating cannot fall below 0
     */
    fun calculateRatingDelta(
        playerRating: Int,
        opponentRating: Int,
        result: ArenaMatchResult
    ): Int {
        val expected = calculateExpectedScore(playerRating, opponentRating)
        val actualScore = when (result) {
            ArenaMatchResult.VICTORY -> 1.0
            ArenaMatchResult.DEFEAT -> 0.0
            ArenaMatchResult.DRAW -> 0.5
        }
        val rawDelta = (K_FACTOR * (actualScore - expected)).roundToInt()

        return when (result) {
            ArenaMatchResult.VICTORY -> maxOf(1, rawDelta)
            ArenaMatchResult.DEFEAT -> minOf(-1, rawDelta)
            ArenaMatchResult.DRAW -> rawDelta
        }
    }

    /**
     * Calculates the new rating clamped to minimum 0.
     */
    fun applyRatingChange(currentRating: Int, delta: Int): Int {
        return maxOf(MINIMUM_RATING, currentRating + delta)
    }

    /**
     * Seasonal Soft Reset Rule (Requirement #9):
     * At season end, players retain a portion of their progress above 1000 base:
     * resetRating = maxOf(1000, 1000 + (rating - 1000) / 2)
     * If rating was below 1000, it is reset back up to baseline 1000 for the new season.
     */
    fun calculateSeasonResetRating(currentRating: Int): Int {
        return if (currentRating > STARTING_RATING) {
            STARTING_RATING + (currentRating - STARTING_RATING) / 2
        } else {
            STARTING_RATING
        }
    }
}
