package com.example

import com.example.data.ActiveDeck
import com.example.data.Alliance
import com.example.data.AllianceEmblems
import com.example.data.AllianceProgressionConfig
import com.example.data.ArenaCatalog
import com.example.data.ArenaRankTier
import com.example.data.ArenaRatingCalculator
import com.example.data.CardCatalog
import com.example.data.EndgameCatalog
import com.example.data.EndgameDifficulty
import com.example.data.EventCatalog
import com.example.data.HeroCatalog
import com.example.data.WorldBossRewardTier
import com.example.monetization.PlayerEconomyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The Cloud Functions validate against functions/src/game/gameCatalog.json. That file is generated
 * from the Kotlin catalogs so the server and the client can never silently disagree about card IDs,
 * the starter collection, Arena rules, boss/raid/event definitions or alliance limits.
 *
 * Regenerate after changing game data:  UPDATE_GAME_CATALOG=1 ./gradlew test --tests "*GameCatalogParityTest*"
 */
class GameCatalogParityTest {

    private val catalogFile = File("../functions/src/game/gameCatalog.json")

    @Test
    fun serverCatalogMatchesKotlinGameData() {
        val generated = Json.write(buildCatalog()) + "\n"

        if (System.getenv("UPDATE_GAME_CATALOG") == "1") {
            catalogFile.parentFile.mkdirs()
            catalogFile.writeText(generated)
        }

        assertTrue("Missing ${catalogFile.absolutePath}. Run with UPDATE_GAME_CATALOG=1.", catalogFile.exists())
        assertEquals(
            "functions/src/game/gameCatalog.json is stale. Regenerate with UPDATE_GAME_CATALOG=1.",
            generated.replace("\r\n", "\n"),
            catalogFile.readText().replace("\r\n", "\n")
        )
    }

    @Test
    fun everyArenaOpponentDeckIsAValidServerDeck() {
        ArenaCatalog.OPPONENT_POOL.forEach { opponent ->
            assertEquals(ActiveDeck.REQUIRED_DECK_SIZE, opponent.deckCardIds.size)
            assertTrue(opponent.deckCardIds.all { CardCatalog.findDefinition(it) != null })
            assertTrue(opponent.deckCardIds.groupingBy { it }.eachCount().values.all { it <= ActiveDeck.MAX_DUPLICATES_PER_CARD })
        }
    }

    @Test
    fun worldBossTierThresholdsMatchTheCatalogOrdering() {
        val thresholds = listOf(
            WorldBossRewardTier.TOP_1_PERCENT to 1_000_000L,
            WorldBossRewardTier.TOP_5_PERCENT to 500_000L,
            WorldBossRewardTier.TOP_10_PERCENT to 200_000L,
            WorldBossRewardTier.TOP_25_PERCENT to 50_000L,
            WorldBossRewardTier.PARTICIPATION to 0L
        )
        thresholds.forEach { (tier, minDamage) ->
            assertEquals(tier, WorldBossRewardTier.determineTier(minDamage))
            if (minDamage > 0) assertTrue(WorldBossRewardTier.determineTier(minDamage - 1) != tier)
        }
    }

    private fun buildCatalog(): Map<String, Any?> {
        val starter = PlayerEconomyState()
        return linkedMapOf(
            "version" to 1,
            "cards" to CardCatalog.ALL_CARDS.map { it.id },
            "heroes" to HeroCatalog.SELECTION_HEROES.map { it.id },
            "deck" to linkedMapOf(
                "size" to ActiveDeck.REQUIRED_DECK_SIZE,
                "maxCopies" to ActiveDeck.MAX_DUPLICATES_PER_CARD
            ),
            "starter" to linkedMapOf(
                "heroId" to starter.activeDeck.heroId,
                "deck" to starter.activeDeck.cardIds,
                "ownedCardCounts" to LinkedHashMap(starter.ownedCardCounts.toSortedMap())
            ),
            "arena" to linkedMapOf(
                "maxDailyAttempts" to ArenaCatalog.MAX_DAILY_ATTEMPTS,
                "seasonDays" to ArenaCatalog.SEASON_DURATION_DAYS,
                "startingRating" to ArenaRatingCalculator.STARTING_RATING,
                "minRating" to ArenaRatingCalculator.MINIMUM_RATING,
                "kFactor" to ArenaRatingCalculator.K_FACTOR,
                "searchRange" to 150,
                "rewards" to linkedMapOf(
                    "firstWin" to rewards(
                        ArenaCatalog.Rewards.FIRST_WIN_GOLD, ArenaCatalog.Rewards.FIRST_WIN_SHARDS,
                        ArenaCatalog.Rewards.FIRST_WIN_ARENA_POINTS, ArenaCatalog.Rewards.FIRST_WIN_XP
                    ),
                    "normalWin" to rewards(
                        ArenaCatalog.Rewards.NORMAL_WIN_GOLD, ArenaCatalog.Rewards.NORMAL_WIN_SHARDS,
                        ArenaCatalog.Rewards.NORMAL_WIN_ARENA_POINTS, ArenaCatalog.Rewards.NORMAL_WIN_XP
                    ),
                    "defeat" to rewards(
                        ArenaCatalog.Rewards.DEFEAT_GOLD, ArenaCatalog.Rewards.DEFEAT_SHARDS,
                        ArenaCatalog.Rewards.DEFEAT_ARENA_POINTS, ArenaCatalog.Rewards.DEFEAT_XP
                    ),
                    "streak" to linkedMapOf<String, Any?>().also { map ->
                        listOf(3, 5, 10).forEach { streak ->
                            val (gold, points) = ArenaCatalog.Rewards.getStreakBonus(streak)
                            map[streak.toString()] = linkedMapOf("gold" to gold, "arenaPoints" to points)
                        }
                    }
                ),
                "tiers" to ArenaRankTier.entries.map {
                    linkedMapOf("name" to it.name, "min" to it.minRating, "max" to it.maxRating)
                },
                "seasonRewards" to LinkedHashMap<String, Any?>().also { map ->
                    ArenaRankTier.entries.forEach { tier ->
                        val r = ArenaCatalog.SEASON_REWARDS.getValue(tier)
                        map[tier.name] = linkedMapOf(
                            "gold" to r.gold,
                            "cardShards" to r.cardShards,
                            "heroShards" to r.heroShards,
                            "frameId" to r.exclusiveFrameId,
                            "cosmeticId" to r.exclusiveCosmeticId,
                            "title" to r.exclusiveTitle
                        )
                    }
                },
                "opponents" to ArenaCatalog.OPPONENT_POOL.map {
                    linkedMapOf(
                        "id" to it.id,
                        "name" to it.name,
                        "heroId" to it.heroId,
                        "heroName" to it.heroName,
                        "archetype" to it.archetype.name,
                        "title" to it.title,
                        "rating" to it.rating,
                        "deck" to it.deckCardIds
                    )
                }
            ),
            "difficulties" to EndgameDifficulty.entries.map {
                linkedMapOf(
                    "id" to it.id,
                    "hpMultiplier" to it.multiplier.toDouble(),
                    "rewardMultiplier" to it.rewardMultiplier.toDouble()
                )
            },
            "worldBosses" to EndgameCatalog.ALL_WORLD_BOSSES.map {
                linkedMapOf("bossId" to it.bossId, "maxHp" to it.maxHp, "start" to it.startTime, "end" to it.endTime)
            },
            "worldBossTiers" to listOf(
                WorldBossRewardTier.TOP_1_PERCENT to 1_000_000L,
                WorldBossRewardTier.TOP_5_PERCENT to 500_000L,
                WorldBossRewardTier.TOP_10_PERCENT to 200_000L,
                WorldBossRewardTier.TOP_25_PERCENT to 50_000L,
                WorldBossRewardTier.PARTICIPATION to 0L
            ).map { (tier, minDamage) ->
                linkedMapOf(
                    "id" to tier.name,
                    "minDamage" to minDamage,
                    "gold" to tier.goldReward,
                    "eventTokens" to tier.eventTokensReward,
                    "heroShards" to tier.heroShardsReward,
                    "cardShards" to tier.cardShardsReward
                )
            },
            "raids" to EndgameCatalog.ALL_RAIDS.map { raid ->
                linkedMapOf(
                    "raidId" to raid.raidId,
                    "defaultDifficulty" to raid.difficulty.id,
                    "stages" to raid.stages.map {
                        linkedMapOf(
                            "stage" to it.stageNumber,
                            "hp" to it.enemyHp,
                            "gold" to it.goldReward,
                            "eventTokens" to it.eventTokensReward,
                            "xp" to it.xpReward,
                            "boss" to it.isBossStage
                        )
                    }
                )
            },
            "events" to EventCatalog.getDefaultEvents().map {
                linkedMapOf(
                    "eventId" to it.eventId,
                    "type" to it.eventType.name,
                    "start" to it.startDate,
                    "end" to it.endDate,
                    "active" to it.isActive
                )
            },
            "eventShop" to EndgameCatalog.DEFAULT_EVENT_SHOP_ITEMS.map {
                linkedMapOf(
                    "itemId" to it.itemId,
                    "category" to it.category,
                    "tokenPrice" to it.tokenPrice,
                    "limit" to it.purchaseLimit,
                    "gold" to it.rewardGold,
                    "heroShards" to it.rewardHeroShards,
                    "heroId" to it.rewardHeroId,
                    "cardShards" to it.rewardCardShards,
                    "cardId" to it.rewardCardId,
                    "cosmeticId" to it.rewardCosmeticId
                )
            },
            "alliance" to linkedMapOf(
                "maxMembers" to Alliance.MAX_MEMBERS,
                "maxLevel" to AllianceProgressionConfig.MAX_ALLIANCE_LEVEL,
                "xpTable" to (1 until AllianceProgressionConfig.MAX_ALLIANCE_LEVEL).map {
                    AllianceProgressionConfig.getXpRequiredForNextLevel(it)
                },
                "emblems" to AllianceEmblems.ALL_EMBLEMS.map { it.id }
            )
        )
    }

    private fun rewards(gold: Int, cardShards: Int, arenaPoints: Int, xp: Int) =
        linkedMapOf("gold" to gold, "cardShards" to cardShards, "arenaPoints" to arenaPoints, "xp" to xp)

    /** Minimal deterministic JSON writer (2-space indent, insertion-ordered maps). */
    private object Json {
        fun write(value: Any?, indent: Int = 0): String = when (value) {
            null -> "null"
            is String -> quote(value)
            is Boolean, is Int, is Long -> value.toString()
            is Double -> if (value % 1.0 == 0.0) value.toString() else value.toString()
            is Map<*, *> -> if (value.isEmpty()) "{}" else {
                val pad = "  ".repeat(indent + 1)
                value.entries.joinToString(",\n", "{\n", "\n" + "  ".repeat(indent) + "}") {
                    "$pad${quote(it.key.toString())}: ${write(it.value, indent + 1)}"
                }
            }
            is List<*> -> when {
                value.isEmpty() -> "[]"
                value.all { it is String || it is Number || it is Boolean } ->
                    value.joinToString(", ", "[", "]") { write(it, indent) }
                else -> {
                    val pad = "  ".repeat(indent + 1)
                    value.joinToString(",\n", "[\n", "\n" + "  ".repeat(indent) + "]") { "$pad${write(it, indent + 1)}" }
                }
            }
            else -> error("Unsupported JSON value: ${value::class}")
        }

        private fun quote(s: String): String =
            "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    }
}
