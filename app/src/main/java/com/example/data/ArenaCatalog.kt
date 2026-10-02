package com.example.data

/**
 * Centralized Arena Catalog & Service Configuration (Requirements #3, #6, #7, #8, #10, #11, #13).
 */
object ArenaCatalog {

    const val MAX_DAILY_ATTEMPTS = 5
    const val SEASON_DURATION_DAYS = 28
    const val SEASON_DURATION_MS = SEASON_DURATION_DAYS * 24L * 60L * 60L * 1000L

    // Rewards (Requirement #8)
    object Rewards {
        const val FIRST_WIN_GOLD = 1500
        const val FIRST_WIN_SHARDS = 25
        const val FIRST_WIN_ARENA_POINTS = 100
        const val FIRST_WIN_XP = 250

        const val NORMAL_WIN_GOLD = 750
        const val NORMAL_WIN_SHARDS = 10
        const val NORMAL_WIN_ARENA_POINTS = 50
        const val NORMAL_WIN_XP = 150

        const val DEFEAT_GOLD = 250
        const val DEFEAT_SHARDS = 5
        const val DEFEAT_ARENA_POINTS = 20
        const val DEFEAT_XP = 75

        // Win Streak Bonuses (Requirement #13)
        fun getStreakBonus(streak: Int): Pair<Int, Int> = when (streak) {
            3 -> Pair(200, 10) // +200 Gold, +10 Points
            5 -> Pair(500, 25) // +500 Gold, +25 Points
            10 -> Pair(1500, 50) // +1500 Gold, +50 Points
            else -> Pair(0, 0)
        }
    }

    // Season Rewards Table (Requirement #10)
    val SEASON_REWARDS: Map<ArenaRankTier, ArenaSeasonReward> = mapOf(
        ArenaRankTier.BRONZE to ArenaSeasonReward(
            tier = ArenaRankTier.BRONZE,
            gold = 5000,
            cardShards = 50,
            heroShards = 0,
            exclusiveFrameId = "frame_default_bronze"
        ),
        ArenaRankTier.SILVER to ArenaSeasonReward(
            tier = ArenaRankTier.SILVER,
            gold = 10000,
            cardShards = 100,
            heroShards = 0,
            exclusiveFrameId = "frame_silver_warrior"
        ),
        ArenaRankTier.GOLD to ArenaSeasonReward(
            tier = ArenaRankTier.GOLD,
            gold = 20000,
            cardShards = 150,
            heroShards = 25,
            exclusiveFrameId = "frame_gold_champion"
        ),
        ArenaRankTier.PLATINUM to ArenaSeasonReward(
            tier = ArenaRankTier.PLATINUM,
            gold = 35000,
            cardShards = 200,
            heroShards = 40,
            exclusiveFrameId = "frame_platinum_gladiator"
        ),
        ArenaRankTier.DIAMOND to ArenaSeasonReward(
            tier = ArenaRankTier.DIAMOND,
            gold = 50000,
            cardShards = 300,
            heroShards = 60,
            exclusiveFrameId = "frame_diamond_warlord"
        ),
        ArenaRankTier.MASTER to ArenaSeasonReward(
            tier = ArenaRankTier.MASTER,
            gold = 75000,
            cardShards = 400,
            heroShards = 100,
            exclusiveFrameId = "frame_master_conqueror"
        ),
        ArenaRankTier.GRANDMASTER to ArenaSeasonReward(
            tier = ArenaRankTier.GRANDMASTER,
            gold = 100000,
            cardShards = 500,
            heroShards = 150,
            exclusiveCosmeticId = "cosmetic_gm_aura",
            exclusiveFrameId = "frame_grandmaster_mythic"
        ),
        ArenaRankTier.MYTHIC to ArenaSeasonReward(
            tier = ArenaRankTier.MYTHIC,
            gold = 150000,
            cardShards = 1000,
            heroShards = 250,
            exclusiveTitle = "Mythic Sovereign",
            exclusiveCosmeticId = "cosmetic_mythic_wings",
            exclusiveFrameId = "frame_mythic_sovereign"
        )
    )

    // Archetype 20-card Decks (Requirement #3, #19)
    // Every single archetype deck contains EXACTLY 20 valid card IDs
    val OLYMPUS_VANGUARD_DECK = listOf(
        "c_olympian_guard", "c_olympian_guard",
        "c_titans_wrath",
        "c_power_strike", "c_power_strike",
        "c_heroic_rage", "c_heroic_rage",
        "c_nectar_gods", "c_nectar_gods",
        "c_spartan_phalanx", "c_spartan_phalanx",
        "c_hydra_blade", "c_hydra_blade",
        "c_nemean_hide", "c_nemean_hide",
        "c_divine_challenge", "c_divine_challenge",
        "c_zeus_thunderstone",
        "c_divine_aegis", "c_ambrosia_draft"
    )

    val SPARTAN_WARBORN_DECK = listOf(
        "c_spartan_phalanx", "c_spartan_phalanx",
        "c_ares_retribution", "c_ares_retribution",
        "c_hydra_blade", "c_hydra_blade",
        "c_power_strike", "c_power_strike",
        "c_heroic_rage", "c_heroic_rage",
        "c_cyclops_hammer", "c_cyclops_hammer",
        "c_divine_challenge", "c_divine_challenge",
        "c_titans_wrath",
        "c_nemean_hide", "c_nemean_hide",
        "c_olympian_guard", "c_olympian_guard",
        "c_nectar_gods"
    )

    val VALHALLA_STORM_DECK = listOf(
        "c_zeus_thunderstone",
        "c_power_strike", "c_power_strike",
        "c_cyclops_hammer", "c_cyclops_hammer",
        "c_celestial_arrow", "c_celestial_arrow",
        "c_heroic_rage", "c_heroic_rage",
        "c_spartan_phalanx", "c_spartan_phalanx",
        "c_hydra_blade", "c_hydra_blade",
        "c_titans_wrath",
        "c_nemean_hide", "c_nemean_hide",
        "c_nectar_gods", "c_nectar_gods",
        "c_ambrosia_draft", "c_ambrosia_draft"
    )

    val UNDERWORLD_REAPER_DECK = listOf(
        "c_cerberus_bite",
        "c_medusa_gaze",
        "c_phoenix_rebirth",
        "c_hydra_blade", "c_hydra_blade",
        "c_shield_bash", "c_shield_bash",
        "c_power_strike", "c_power_strike",
        "c_olympian_guard", "c_olympian_guard",
        "c_divine_challenge", "c_divine_challenge",
        "c_nemean_hide", "c_nemean_hide",
        "c_nectar_gods", "c_nectar_gods",
        "c_heroic_rage", "c_heroic_rage",
        "c_titans_wrath"
    )

    val EGYPTIAN_JUDGMENT_DECK = listOf(
        "c_divine_aegis",
        "c_athena_blessing",
        "c_celestial_arrow", "c_celestial_arrow",
        "c_shield_bash", "c_shield_bash",
        "c_olympian_guard", "c_olympian_guard",
        "c_power_strike", "c_power_strike",
        "c_divine_challenge", "c_divine_challenge",
        "c_nemean_hide", "c_nemean_hide",
        "c_ambrosia_draft", "c_ambrosia_draft",
        "c_nectar_gods", "c_nectar_gods",
        "c_heroic_rage",
        "c_titans_wrath"
    )

    val CELESTIAL_GUARDIAN_DECK = listOf(
        "c_divine_aegis",
        "c_athena_blessing",
        "c_shield_bash", "c_shield_bash",
        "c_olympian_guard", "c_olympian_guard",
        "c_nemean_hide", "c_nemean_hide",
        "c_spartan_phalanx", "c_spartan_phalanx",
        "c_nectar_gods", "c_nectar_gods",
        "c_ambrosia_draft", "c_ambrosia_draft",
        "c_divine_challenge", "c_divine_challenge",
        "c_power_strike", "c_power_strike",
        "c_phoenix_rebirth",
        "c_titans_wrath"
    )

    fun getDeckForArchetype(archetype: ArenaOpponentArchetype): List<String> = when (archetype) {
        ArenaOpponentArchetype.OLYMPUS_VANGUARD -> OLYMPUS_VANGUARD_DECK
        ArenaOpponentArchetype.SPARTAN_WARBORN -> SPARTAN_WARBORN_DECK
        ArenaOpponentArchetype.VALHALLA_STORM -> VALHALLA_STORM_DECK
        ArenaOpponentArchetype.UNDERWORLD_REAPER -> UNDERWORLD_REAPER_DECK
        ArenaOpponentArchetype.EGYPTIAN_JUDGMENT -> EGYPTIAN_JUDGMENT_DECK
        ArenaOpponentArchetype.CELESTIAL_GUARDIAN -> CELESTIAL_GUARDIAN_DECK
    }

    // Deterministic Pool of Simulated Opponents (Requirement #3, #6)
    val OPPONENT_POOL: List<ArenaOpponent> = listOf(
        ArenaOpponent(
            id = "pvp_opp_1",
            name = "Leonidas IX",
            heroId = "ares",
            heroName = "Ares",
            archetype = ArenaOpponentArchetype.SPARTAN_WARBORN,
            rating = 980,
            combatPower = 8400,
            deckCardIds = SPARTAN_WARBORN_DECK,
            title = "Warmonger of Sparta"
        ),
        ArenaOpponent(
            id = "pvp_opp_2",
            name = "Thalassa Star",
            heroId = HerculesIdentity.HERO_ID,
            heroName = "Hercules",
            archetype = ArenaOpponentArchetype.OLYMPUS_VANGUARD,
            rating = 1010,
            combatPower = 8600,
            deckCardIds = OLYMPUS_VANGUARD_DECK,
            title = "Acolyte of Olympus"
        ),
        ArenaOpponent(
            id = "pvp_opp_3",
            name = "Sigurd Thunder",
            heroId = "thor",
            heroName = "Thor",
            archetype = ArenaOpponentArchetype.VALHALLA_STORM,
            rating = 1045,
            combatPower = 8900,
            deckCardIds = VALHALLA_STORM_DECK,
            title = "Hammer of Asgard"
        ),
        ArenaOpponent(
            id = "pvp_opp_4",
            name = "Osiris_Judge",
            heroId = "anubis",
            heroName = "Anubis",
            archetype = ArenaOpponentArchetype.EGYPTIAN_JUDGMENT,
            rating = 1120,
            combatPower = 9300,
            deckCardIds = EGYPTIAN_JUDGMENT_DECK,
            title = "Weigher of Hearts"
        ),
        ArenaOpponent(
            id = "pvp_opp_5",
            name = "Aegis_Kallisto",
            heroId = "athena",
            heroName = "Athena",
            archetype = ArenaOpponentArchetype.CELESTIAL_GUARDIAN,
            rating = 1220,
            combatPower = 9800,
            deckCardIds = CELESTIAL_GUARDIAN_DECK,
            title = "Strategist of Athens"
        ),
        ArenaOpponent(
            id = "pvp_opp_6",
            name = "Moros_Eclipse",
            heroId = "hades",
            heroName = "Hades",
            archetype = ArenaOpponentArchetype.UNDERWORLD_REAPER,
            rating = 1350,
            combatPower = 10500,
            deckCardIds = UNDERWORLD_REAPER_DECK,
            title = "Reaper of Tartarus"
        ),
        ArenaOpponent(
            id = "pvp_opp_7",
            name = "Valkyrie_Freya",
            heroId = "thor",
            heroName = "Thor",
            archetype = ArenaOpponentArchetype.VALHALLA_STORM,
            rating = 1480,
            combatPower = 11200,
            deckCardIds = VALHALLA_STORM_DECK,
            title = "Shieldmaiden of Valhalla"
        ),
        ArenaOpponent(
            id = "pvp_opp_8",
            name = "Archon_Drakos",
            heroId = "ares",
            heroName = "Ares",
            archetype = ArenaOpponentArchetype.SPARTAN_WARBORN,
            rating = 1650,
            combatPower = 12400,
            deckCardIds = SPARTAN_WARBORN_DECK,
            title = "High Warlord"
        ),
        ArenaOpponent(
            id = "pvp_opp_9",
            name = "Titan_Pallas",
            heroId = "athena",
            heroName = "Athena",
            archetype = ArenaOpponentArchetype.CELESTIAL_GUARDIAN,
            rating = 1950,
            combatPower = 14200,
            deckCardIds = CELESTIAL_GUARDIAN_DECK,
            title = "Diamond Aegis"
        ),
        ArenaOpponent(
            id = "pvp_opp_10",
            name = "Grandmaster_Zephyr",
            heroId = HerculesIdentity.HERO_ID,
            heroName = "Hercules",
            archetype = ArenaOpponentArchetype.OLYMPUS_VANGUARD,
            rating = 2250,
            combatPower = 16500,
            deckCardIds = OLYMPUS_VANGUARD_DECK,
            title = "Master Gladiator"
        )
    )

    /**
     * Deterministic simulated matchmaking engine (Requirement #6).
     * Selects an opponent within search range based on player rating.
     */
    fun findOpponent(playerRating: Int, searchRange: Int = 150): ArenaOpponent {
        val eligible = OPPONENT_POOL.filter {
            it.rating in (playerRating - searchRange)..(playerRating + searchRange)
        }
        val target = if (eligible.isNotEmpty()) {
            eligible.minByOrNull { kotlin.math.abs(it.rating - playerRating) }!!
        } else {
            OPPONENT_POOL.minByOrNull { kotlin.math.abs(it.rating - playerRating) }!!
        }

        // Adjust target rating dynamically to closely reflect the competitive match
        val ratingOffset = when {
            playerRating < 800 -> -30
            playerRating > 2000 -> 25
            else -> if (playerRating % 2 == 0) 15 else -15
        }
        val adjustedRating = maxOf(ArenaRatingCalculator.MINIMUM_RATING, playerRating + ratingOffset)

        return target.copy(
            rating = adjustedRating,
            combatPower = (adjustedRating * 8.5).toInt()
        )
    }

    /**
     * Deterministic Leaderboard Generator (Requirement #11).
     * Generates a 20-entry competitive leaderboard including the current player.
     */
    fun getLeaderboard(
        playerRating: Int,
        playerName: String = "Hercules (You)",
        playerAvatar: String = "avatar_default_hercules",
        playerWins: Int = 0,
        playerLosses: Int = 0,
        playerPeakRating: Int = playerRating
    ): List<ArenaLeaderboardEntry> {
        val simulated = listOf(
            ArenaLeaderboardEntry(1, "MythicZeus_99", "avatar_zeus", 3120, ArenaRankTier.MYTHIC, 142, 18, 88.8f, 3120),
            ArenaLeaderboardEntry(2, "OdinAllfather", "avatar_thor", 2980, ArenaRankTier.MYTHIC, 130, 22, 85.5f, 3010),
            ArenaLeaderboardEntry(3, "KronosBane", "avatar_hercules", 2850, ArenaRankTier.MYTHIC, 115, 25, 82.1f, 2890),
            ArenaLeaderboardEntry(4, "AresWrath_Alpha", "avatar_ares", 2740, ArenaRankTier.GRANDMASTER, 108, 31, 77.7f, 2760),
            ArenaLeaderboardEntry(5, "AnubisReaper", "avatar_anubis", 2620, ArenaRankTier.GRANDMASTER, 95, 28, 77.2f, 2650),
            ArenaLeaderboardEntry(6, "ValkyrieQueen", "avatar_thor", 2480, ArenaRankTier.MASTER, 88, 30, 74.6f, 2510),
            ArenaLeaderboardEntry(7, "SpartanGeneral", "avatar_ares", 2350, ArenaRankTier.MASTER, 82, 34, 70.7f, 2390),
            ArenaLeaderboardEntry(8, "AthenaStrategos", "avatar_athena", 2210, ArenaRankTier.MASTER, 76, 35, 68.5f, 2250),
            ArenaLeaderboardEntry(9, "StyxShadow", "avatar_hades", 2120, ArenaRankTier.DIAMOND, 70, 36, 66.0f, 2150),
            ArenaLeaderboardEntry(10, "OlympusShield", "avatar_hercules", 2010, ArenaRankTier.DIAMOND, 65, 38, 63.1f, 2040),
            ArenaLeaderboardEntry(11, "GladiatorKallisto", "avatar_athena", 1880, ArenaRankTier.PLATINUM, 58, 37, 61.1f, 1920),
            ArenaLeaderboardEntry(12, "ThorHammerStrike", "avatar_thor", 1750, ArenaRankTier.PLATINUM, 52, 39, 57.1f, 1790),
            ArenaLeaderboardEntry(13, "LeonidasBlade", "avatar_ares", 1620, ArenaRankTier.PLATINUM, 46, 40, 53.5f, 1660),
            ArenaLeaderboardEntry(14, "SphinxWatcher", "avatar_anubis", 1490, ArenaRankTier.GOLD, 40, 38, 51.3f, 1520),
            ArenaLeaderboardEntry(15, "CentaurRider", "avatar_hercules", 1380, ArenaRankTier.GOLD, 35, 36, 49.3f, 1410),
            ArenaLeaderboardEntry(16, "NorseRaider", "avatar_thor", 1250, ArenaRankTier.SILVER, 28, 30, 48.3f, 1280),
            ArenaLeaderboardEntry(17, "AthenianHopLite", "avatar_athena", 1150, ArenaRankTier.SILVER, 22, 25, 46.8f, 1190),
            ArenaLeaderboardEntry(18, "SpartanRecruit", "avatar_ares", 1020, ArenaRankTier.SILVER, 18, 22, 45.0f, 1050),
            ArenaLeaderboardEntry(19, "UnderworldNovice", "avatar_hades", 920, ArenaRankTier.BRONZE, 12, 20, 37.5f, 950),
            ArenaLeaderboardEntry(20, "BronzeChallenger", "avatar_hercules", 820, ArenaRankTier.BRONZE, 8, 18, 30.8f, 850)
        )

        val playerTotal = playerWins + playerLosses
        val playerWinRate = if (playerTotal > 0) (playerWins.toFloat() / playerTotal) * 100f else 0f
        val playerEntry = ArenaLeaderboardEntry(
            rank = 1, // Will be computed dynamically
            playerName = playerName,
            avatarId = playerAvatar,
            rating = playerRating,
            tier = ArenaRankTier.fromRating(playerRating),
            wins = playerWins,
            losses = playerLosses,
            winRate = playerWinRate,
            peakRating = playerPeakRating,
            isCurrentPlayer = true
        )

        val combined = (simulated + playerEntry)
            .sortedByDescending { it.rating }
            .mapIndexed { index, entry ->
                entry.copy(rank = index + 1)
            }

        return combined
    }
}
