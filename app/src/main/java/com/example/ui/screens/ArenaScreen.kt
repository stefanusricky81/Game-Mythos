package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.monetization.PlayerEconomyRepository
import com.example.ui.components.MythosButton
import com.example.ui.components.MythosButtonStyle
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

enum class ArenaTab {
    OVERVIEW,
    LEADERBOARD,
    HISTORY,
    SEASON
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArenaScreen(
    onNavigateBack: () -> Unit,
    onStartArenaBattle: (BattleEncounterConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    val economyState by PlayerEconomyRepository.instance.economyState.collectAsState()
    var selectedTab by remember { mutableStateOf(ArenaTab.OVERVIEW) }
    var isMatchmakingActive by remember { mutableStateOf(false) }
    var foundOpponent by remember { mutableStateOf<ArenaOpponent?>(null) }
    var showNoAttemptsDialog by remember { mutableStateOf(false) }
    var seasonRewardClaimMessage by remember { mutableStateOf<String?>(null) }

    val currentRating = economyState.arenaRating
    val currentTier = economyState.arenaTier
    val nextTier = ArenaRankTier.getNextTier(currentTier)
    val tierProgress = ArenaRankTier.calculateProgressToNextTier(currentRating)
    val numberFormat = NumberFormat.getNumberInstance(Locale.US)

    // Calculate simulated season remaining days
    val currentSeason = economyState.arenaCurrentSeason
    val now = System.currentTimeMillis()
    val remainingDays = maxOf(1L, (currentSeason.endDateMs - now) / (24L * 60L * 60L * 1000L))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "PVP ARENA",
                            style = MythosTypography.HeroName.copy(fontSize = 18.sp),
                            color = MythosTokens.PrimaryGold
                        )
                        Text(
                            text = "Competitive Gladiators • Season ${currentSeason.seasonNumber}",
                            style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                            color = MythosTokens.TextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("arena_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MythosTokens.PrimaryGold
                        )
                    }
                },
                actions = {
                    // Arena Points & Gold Badges
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MythosTokens.PanelElevated,
                            border = BorderStroke(1.dp, MythosTokens.PanelBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = "🏆", fontSize = 12.sp)
                                Text(
                                    text = "${numberFormat.format(economyState.arenaPoints)} AP",
                                    style = MythosTypography.HeroName.copy(fontSize = 11.sp),
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MythosTokens.Background
                )
            )
        },
        containerColor = MythosTokens.Background
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Navigation Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MythosTokens.PanelElevated)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TabButton(
                    text = "OVERVIEW",
                    icon = Icons.Default.SportsMartialArts,
                    isSelected = selectedTab == ArenaTab.OVERVIEW,
                    testTag = "arena_overview_tab",
                    onClick = { selectedTab = ArenaTab.OVERVIEW },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    text = "RANKING",
                    icon = Icons.Default.Leaderboard,
                    isSelected = selectedTab == ArenaTab.LEADERBOARD,
                    testTag = "arena_leaderboard_tab",
                    onClick = { selectedTab = ArenaTab.LEADERBOARD },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    text = "HISTORY",
                    icon = Icons.Default.History,
                    isSelected = selectedTab == ArenaTab.HISTORY,
                    testTag = "arena_history_tab",
                    onClick = { selectedTab = ArenaTab.HISTORY },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    text = "SEASON",
                    icon = Icons.Default.EmojiEvents,
                    isSelected = selectedTab == ArenaTab.SEASON,
                    testTag = "arena_season_tab",
                    onClick = { selectedTab = ArenaTab.SEASON },
                    modifier = Modifier.weight(1f)
                )
            }

            // Tab Content
            when (selectedTab) {
                ArenaTab.OVERVIEW -> {
                    ArenaOverviewContent(
                        economyState = economyState,
                        currentTier = currentTier,
                        nextTier = nextTier,
                        tierProgress = tierProgress,
                        remainingDays = remainingDays,
                        numberFormat = numberFormat,
                        onFindMatch = {
                            if (economyState.arenaDailyAttempts > 0) {
                                isMatchmakingActive = true
                                foundOpponent = ArenaCatalog.findOpponent(economyState.arenaRating)
                            } else {
                                showNoAttemptsDialog = true
                            }
                        }
                    )
                }
                ArenaTab.LEADERBOARD -> {
                    ArenaLeaderboardContent(
                        playerRating = currentRating,
                        playerWins = economyState.arenaWins,
                        playerLosses = economyState.arenaLosses,
                        playerPeak = economyState.arenaPeakRating,
                        numberFormat = numberFormat
                    )
                }
                ArenaTab.HISTORY -> {
                    ArenaHistoryContent(
                        matchHistory = economyState.arenaMatchHistory,
                        numberFormat = numberFormat
                    )
                }
                ArenaTab.SEASON -> {
                    ArenaSeasonContent(
                        season = currentSeason,
                        currentTier = currentTier,
                        claimedSeasons = economyState.arenaSeasonRewardsClaimed,
                        numberFormat = numberFormat,
                        onClaimReward = { seasonId ->
                            val reward = PlayerEconomyRepository.instance.claimSeasonReward(seasonId)
                            if (reward != null) {
                                seasonRewardClaimMessage = "Claimed Season ${currentSeason.seasonNumber} Rewards! +${numberFormat.format(reward.gold)} Gold, +${reward.cardShards} Shards"
                            }
                        }
                    )
                }
            }
        }
    }

    // Matchmaking Modal Dialog (Requirement #6)
    if (isMatchmakingActive && foundOpponent != null) {
        val opponent = foundOpponent!!
        Dialog(onDismissRequest = { isMatchmakingActive = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, MythosTokens.PrimaryGold, RoundedCornerShape(16.dp))
                    .testTag("arena_matchmaking_dialog"),
                color = MythosTokens.PanelElevated
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "MATCH FOUND!",
                        style = MythosTypography.HeroName.copy(fontSize = 20.sp),
                        color = MythosTokens.PrimaryGold
                    )

                    Text(
                        text = "Opponent matched within competitive rating range.",
                        style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                        color = MythosTokens.TextMuted,
                        textAlign = TextAlign.Center
                    )

                    // Opponent Preview Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MythosTokens.PanelBorder, RoundedCornerShape(12.dp))
                            .testTag("arena_match_found_card"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF181324))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = opponent.tier.badgeSymbol, fontSize = 32.sp)

                            Text(
                                text = opponent.name,
                                style = MythosTypography.HeroName.copy(fontSize = 18.sp),
                                color = Color.White
                            )

                            Text(
                                text = "${opponent.title} • ${opponent.archetype.title}",
                                style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                                color = MythosTokens.PrimaryGold
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "RATING", style = MythosTypography.RarityLabel.copy(fontSize = 9.sp), color = MythosTokens.TextMuted)
                                    Text(text = "${opponent.rating} ELO", style = MythosTypography.HeroName.copy(fontSize = 14.sp), color = MythosTokens.PrimaryGold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "HERO", style = MythosTypography.RarityLabel.copy(fontSize = 9.sp), color = MythosTokens.TextMuted)
                                    Text(text = opponent.heroName, style = MythosTypography.HeroName.copy(fontSize = 14.sp), color = Color(0xFF38BDF8))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "POWER", style = MythosTypography.RarityLabel.copy(fontSize = 9.sp), color = MythosTokens.TextMuted)
                                    Text(text = numberFormat.format(opponent.combatPower), style = MythosTypography.HeroName.copy(fontSize = 14.sp), color = MythosTokens.SecondaryGold)
                                }
                            }

                            Text(
                                text = "Strategy: ${opponent.archetype.preferredStrategy}",
                                style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                                color = Color(0xFFA59EB5),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // Attempt Notice
                    Text(
                        text = "1 Daily Arena Attempt will be consumed when starting.",
                        style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                        color = MythosTokens.TextMuted
                    )

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                isMatchmakingActive = false
                                foundOpponent = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("arena_cancel_matchmaking"),
                            border = BorderStroke(1.dp, MythosTokens.PanelBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("CANCEL")
                        }

                        MythosButton(
                            text = "ENTER BATTLE",
                            onClick = {
                                val opp = opponent
                                isMatchmakingActive = false
                                foundOpponent = null

                                // Consume attempt and launch battle (Requirement #7, #19)
                                val consumed = PlayerEconomyRepository.instance.consumeArenaAttempt()
                                if (consumed) {
                                    val encounter = BattleEncounterConfig(
                                        encounterName = "Arena: ${opp.name}",
                                        enemyHero = HeroCatalog.findHero(opp.heroId)?.toBattleHero(level = 5) ?: Hero.createAres(),
                                        rewards = BattleRewards(
                                            gold = ArenaCatalog.Rewards.NORMAL_WIN_GOLD,
                                            xp = ArenaCatalog.Rewards.NORMAL_WIN_XP
                                        ),
                                        isArenaMatch = true,
                                        arenaOpponent = opp
                                    )
                                    onStartArenaBattle(encounter)
                                }
                            },
                            style = MythosButtonStyle.PRIMARY,
                            icon = Icons.Default.SportsMartialArts,
                            testTag = "arena_start_battle_button",
                            modifier = Modifier
                                .weight(1.5f)
                                .height(48.dp)
                        )
                    }
                }
            }
        }
    }

    // No Attempts Dialog
    if (showNoAttemptsDialog) {
        AlertDialog(
            onDismissRequest = { showNoAttemptsDialog = false },
            title = { Text("No Arena Attempts Remaining", color = MythosTokens.PrimaryGold) },
            text = {
                Text(
                    "You have used all 5 daily Arena attempts for today.\n\nAttempts reset daily at midnight (00:00). Please return tomorrow to continue climbing the ranks!",
                    color = Color.White
                )
            },
            confirmButton = {
                TextButton(onClick = { showNoAttemptsDialog = false }) {
                    Text("OK", color = MythosTokens.PrimaryGold)
                }
            },
            containerColor = MythosTokens.PanelElevated
        )
    }

    // Season Claim Message Dialog
    if (seasonRewardClaimMessage != null) {
        AlertDialog(
            onDismissRequest = { seasonRewardClaimMessage = null },
            title = { Text("Season Rewards Claimed!", color = MythosTokens.PrimaryGold) },
            text = { Text(seasonRewardClaimMessage!!, color = Color.White) },
            confirmButton = {
                TextButton(onClick = { seasonRewardClaimMessage = null }) {
                    Text("GREAT!", color = MythosTokens.PrimaryGold)
                }
            },
            containerColor = MythosTokens.PanelElevated
        )
    }
}

@Composable
private fun ArenaOverviewContent(
    economyState: com.example.monetization.PlayerEconomyState,
    currentTier: ArenaRankTier,
    nextTier: ArenaRankTier?,
    tierProgress: Float,
    remainingDays: Long,
    numberFormat: NumberFormat,
    onFindMatch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Main Competitive Badge Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.5.dp, currentTier.color, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MythosTokens.PanelElevated)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SEASON 1 • COMPETITIVE",
                        style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                        color = MythosTokens.PrimaryGold
                    )
                    Text(
                        text = "⏳ ${remainingDays}d Left",
                        style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                        color = MythosTokens.TextMuted
                    )
                }

                // Tier Symbol & Name
                Text(text = currentTier.badgeSymbol, fontSize = 48.sp)

                Text(
                    text = "${currentTier.tierName.uppercase()} TIER",
                    style = MythosTypography.HeroName.copy(fontSize = 22.sp),
                    color = currentTier.color
                )

                Text(
                    text = "${numberFormat.format(economyState.arenaRating)} ELO RATING",
                    style = MythosTypography.HeroName.copy(fontSize = 16.sp),
                    color = MythosTokens.PrimaryGold
                )

                // Progress to next tier
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = currentTier.tierName,
                            style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                            color = MythosTokens.TextMuted
                        )
                        Text(
                            text = nextTier?.let { "Next: ${it.tierName} (${it.minRating} ELO)" } ?: "MAX TIER",
                            style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                            color = MythosTokens.PrimaryGold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1E182A))
                            .border(0.5.dp, MythosTokens.PanelBorder, RoundedCornerShape(4.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(tierProgress)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(currentTier.color.copy(alpha = 0.6f), currentTier.color)
                                    )
                                )
                        )
                    }
                }

                Divider(color = MythosTokens.PanelBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

                // Stats Row: Record, Win Rate, Streak, Peak
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        label = "RECORD",
                        value = "${economyState.arenaWins}W - ${economyState.arenaLosses}L",
                        color = Color.White
                    )
                    StatItem(
                        label = "WIN RATE",
                        value = String.format(Locale.US, "%.1f%%", economyState.arenaWinRate),
                        color = Color(0xFF38BDF8)
                    )
                    StatItem(
                        label = "WIN STREAK",
                        value = "🔥 ${economyState.arenaCurrentStreak}",
                        color = MythosTokens.PrimaryGold
                    )
                    StatItem(
                        label = "PEAK RATING",
                        value = "${economyState.arenaPeakRating}",
                        color = MythosTokens.SecondaryGold
                    )
                }
            }
        }

        // Daily Attempts & First Win Status Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MythosTokens.PanelBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = MythosTokens.PanelElevated)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "DAILY ATTEMPTS",
                        style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                        color = MythosTokens.TextMuted
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "⚡", fontSize = 16.sp)
                        Text(
                            text = "${economyState.arenaDailyAttempts} / ${ArenaCatalog.MAX_DAILY_ATTEMPTS}",
                            style = MythosTypography.HeroName.copy(fontSize = 16.sp),
                            color = if (economyState.arenaDailyAttempts > 0) MythosTokens.PrimaryGold else MythosTokens.Damage
                        )
                    }
                    Text(
                        text = "Resets daily at 00:00",
                        style = MythosTypography.CardDescription.copy(fontSize = 9.sp),
                        color = MythosTokens.TextMuted
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MythosTokens.PanelBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = MythosTokens.PanelElevated)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "DAILY FIRST WIN",
                        style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                        color = MythosTokens.TextMuted
                    )
                    val isFirstWinClaimed = economyState.arenaFirstWinClaimedDate == MythosDateUtil.getCurrentLocalDate()
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = if (isFirstWinClaimed) "✅" else "⭐", fontSize = 16.sp)
                        Text(
                            text = if (isFirstWinClaimed) "CLAIMED" else "AVAILABLE",
                            style = MythosTypography.HeroName.copy(fontSize = 14.sp),
                            color = if (isFirstWinClaimed) MythosTokens.TextMuted else MythosTokens.PrimaryGold
                        )
                    }
                    Text(
                        text = "+1,500 Gold • +25 Shards",
                        style = MythosTypography.CardDescription.copy(fontSize = 9.sp),
                        color = MythosTokens.TextMuted
                    )
                }
            }
        }

        // Active Deck Verification Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MythosTokens.PanelBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = MythosTokens.PanelElevated)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "🛡️", fontSize = 24.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ACTIVE DECK: ${economyState.activeDeck.name.uppercase()}",
                        style = MythosTypography.HeroName.copy(fontSize = 13.sp),
                        color = Color.White
                    )
                    Text(
                        text = "20/20 Cards Verified • Tournament Legal",
                        style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                        color = MythosTokens.Success
                    )
                }
            }
        }

        // Primary Matchmaking CTA (Requirement #15)
        MythosButton(
            text = "FIND MATCH",
            subtitle = if (economyState.arenaDailyAttempts > 0) "${economyState.arenaDailyAttempts} Attempts Remaining Today" else "No Attempts Left Today",
            onClick = onFindMatch,
            style = MythosButtonStyle.PRIMARY,
            icon = Icons.Default.SportsMartialArts,
            testTag = "arena_find_match_button",
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
        )
    }
}

@Composable
private fun ArenaLeaderboardContent(
    playerRating: Int,
    playerWins: Int,
    playerLosses: Int,
    playerPeak: Int,
    numberFormat: NumberFormat
) {
    val entries = remember(playerRating, playerWins, playerLosses) {
        ArenaCatalog.getLeaderboard(
            playerRating = playerRating,
            playerWins = playerWins,
            playerLosses = playerLosses,
            playerPeakRating = playerPeak
        )
    }

    val playerEntry = entries.find { it.isCurrentPlayer }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Sticky "MY RANK" Header
        if (playerEntry != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.5.dp, MythosTokens.PrimaryGold, RoundedCornerShape(12.dp))
                        .testTag("arena_my_rank_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF261D12))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "#${playerEntry.rank}",
                            style = MythosTypography.HeroName.copy(fontSize = 20.sp),
                            color = MythosTokens.PrimaryGold
                        )

                        Text(text = playerEntry.tier.badgeSymbol, fontSize = 24.sp)

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = playerEntry.playerName,
                                    style = MythosTypography.HeroName.copy(fontSize = 14.sp),
                                    color = Color.White
                                )
                                Text(
                                    text = "(YOU)",
                                    style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                    color = MythosTokens.PrimaryGold
                                )
                            }
                            Text(
                                text = "${playerEntry.tier.tierName} • ${playerEntry.wins}W - ${playerEntry.losses}L",
                                style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                                color = MythosTokens.TextMuted
                            )
                        }

                        Text(
                            text = "${numberFormat.format(playerEntry.rating)} ELO",
                            style = MythosTypography.HeroName.copy(fontSize = 15.sp),
                            color = MythosTokens.PrimaryGold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "TOP GLADIATORS",
                    style = MythosTypography.RarityLabel.copy(fontSize = 11.sp),
                    color = MythosTokens.PrimaryGold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        // Leaderboard List
        items(entries) { entry ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(
                        0.5.dp,
                        if (entry.isCurrentPlayer) MythosTokens.PrimaryGold else MythosTokens.PanelBorder,
                        RoundedCornerShape(10.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (entry.isCurrentPlayer) Color(0xFF261D12) else MythosTokens.PanelElevated
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = when (entry.rank) {
                            1 -> "🥇"
                            2 -> "🥈"
                            3 -> "🥉"
                            else -> "#${entry.rank}"
                        },
                        style = MythosTypography.HeroName.copy(fontSize = 14.sp),
                        color = if (entry.rank <= 3) MythosTokens.PrimaryGold else MythosTokens.TextMuted,
                        modifier = Modifier.width(36.dp)
                    )

                    Text(text = entry.tier.badgeSymbol, fontSize = 18.sp)

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.playerName,
                            style = MythosTypography.HeroName.copy(fontSize = 13.sp),
                            color = if (entry.isCurrentPlayer) MythosTokens.PrimaryGold else Color.White
                        )
                        Text(
                            text = "${entry.tier.tierName} • ${entry.wins}W - ${entry.losses}L (${String.format(Locale.US, "%.0f%%", entry.winRate)})",
                            style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                            color = MythosTokens.TextMuted
                        )
                    }

                    Text(
                        text = "${numberFormat.format(entry.rating)} ELO",
                        style = MythosTypography.HeroName.copy(fontSize = 13.sp),
                        color = entry.tier.color
                    )
                }
            }
        }
    }
}

@Composable
private fun ArenaHistoryContent(
    matchHistory: List<ArenaMatchRecord>,
    numberFormat: NumberFormat
) {
    if (matchHistory.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "⚔️", fontSize = 42.sp)
                Text(
                    text = "NO MATCHES PLAYED YET",
                    style = MythosTypography.HeroName.copy(fontSize = 16.sp),
                    color = MythosTokens.PrimaryGold
                )
                Text(
                    text = "Enter the Arena and defeat rival gladiators to establish your match history.",
                    style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                    color = MythosTokens.TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(matchHistory) { match ->
                val isVic = match.result == ArenaMatchResult.VICTORY
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            1.dp,
                            if (isVic) MythosTokens.Success.copy(alpha = 0.5f) else MythosTokens.Damage.copy(alpha = 0.5f),
                            RoundedCornerShape(10.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = MythosTokens.PanelElevated)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (isVic) "VICTORY" else "DEFEAT",
                                    style = MythosTypography.HeroName.copy(fontSize = 13.sp),
                                    color = if (isVic) MythosTokens.Success else MythosTokens.Damage
                                )
                                Text(
                                    text = "vs ${match.opponentName}",
                                    style = MythosTypography.HeroName.copy(fontSize = 13.sp),
                                    color = Color.White
                                )
                            }

                            Text(
                                text = if (match.ratingChange >= 0) "+${match.ratingChange} ELO" else "${match.ratingChange} ELO",
                                style = MythosTypography.HeroName.copy(fontSize = 13.sp),
                                color = if (match.ratingChange >= 0) MythosTokens.Success else MythosTokens.Damage
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${match.opponentArchetype} • ${match.turns} Turns",
                                style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                                color = MythosTokens.TextMuted
                            )

                            Text(
                                text = "${match.ratingBefore} → ${match.ratingAfter} ELO",
                                style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                                color = MythosTokens.PrimaryGold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "+${numberFormat.format(match.goldAwarded)} Gold",
                                style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                color = MythosTokens.PrimaryGold
                            )
                            Text(
                                text = "+${match.shardsAwarded} Shards",
                                style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                color = Color(0xFF38BDF8)
                            )
                            Text(
                                text = "+${match.arenaPointsAwarded} AP",
                                style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                color = MythosTokens.SecondaryGold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArenaSeasonContent(
    season: ArenaSeason,
    currentTier: ArenaRankTier,
    claimedSeasons: Set<String>,
    numberFormat: NumberFormat,
    onClaimReward: (String) -> Unit
) {
    val isClaimed = claimedSeasons.contains(season.seasonId)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MythosTokens.PrimaryGold, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = MythosTokens.PanelElevated)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "SEASON ${season.seasonNumber} REWARDS",
                        style = MythosTypography.HeroName.copy(fontSize = 16.sp),
                        color = MythosTokens.PrimaryGold
                    )
                    Text(
                        text = "Rewards are determined by your highest achieved rank tier at the end of the 28-day season. Each reward can be claimed once.",
                        style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                        color = MythosTokens.TextMuted
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Current Tier: ${currentTier.badgeSymbol} ${currentTier.tierName}",
                            style = MythosTypography.HeroName.copy(fontSize = 13.sp),
                            color = currentTier.color
                        )

                        Button(
                            onClick = { onClaimReward(season.seasonId) },
                            enabled = !isClaimed,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MythosTokens.PrimaryGold,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("arena_claim_season_reward_button")
                        ) {
                            Text(if (isClaimed) "CLAIMED" else "CLAIM REWARD", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "TIER REWARD SCHEDULE",
                style = MythosTypography.RarityLabel.copy(fontSize = 11.sp),
                color = MythosTokens.PrimaryGold
            )
        }

        // Tiers table
        items(ArenaRankTier.entries.toList().reversed()) { tier ->
            val reward = ArenaCatalog.SEASON_REWARDS[tier]
            val isCurrentPlayerTier = tier == currentTier

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(
                        if (isCurrentPlayerTier) 1.5.dp else 0.5.dp,
                        if (isCurrentPlayerTier) tier.color else MythosTokens.PanelBorder,
                        RoundedCornerShape(10.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrentPlayerTier) Color(0xFF231C2D) else MythosTokens.PanelElevated
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = tier.badgeSymbol, fontSize = 28.sp)

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = tier.tierName,
                                style = MythosTypography.HeroName.copy(fontSize = 14.sp),
                                color = tier.color
                            )
                            if (isCurrentPlayerTier) {
                                Text(
                                    text = "★ YOUR TIER",
                                    style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                    color = MythosTokens.PrimaryGold
                                )
                            }
                        }

                        Text(
                            text = "${tier.minRating} - ${if (tier.maxRating == Int.MAX_VALUE) "+" else tier.maxRating} ELO",
                            style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                            color = MythosTokens.TextMuted
                        )

                        if (reward != null) {
                            Text(
                                text = buildString {
                                    append("${numberFormat.format(reward.gold)} Gold • ${reward.cardShards} Card Shards")
                                    if (reward.heroShards > 0) append(" • ${reward.heroShards} Hero Shards")
                                    reward.exclusiveTitle?.let { append(" • Title: '$it'") }
                                    reward.exclusiveCosmeticId?.let { append(" • Cosmetic") }
                                },
                                style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                                color = MythosTokens.PrimaryGold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MythosTokens.PrimaryGold else Color.Transparent)
            .border(
                1.dp,
                if (isSelected) MythosTokens.PrimaryGold else MythosTokens.PanelBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 8.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = if (isSelected) Color.Black else MythosTokens.TextMuted,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                color = if (isSelected) Color.Black else MythosTokens.TextMuted,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
            color = MythosTokens.TextMuted
        )
        Text(
            text = value,
            style = MythosTypography.HeroName.copy(fontSize = 12.sp),
            color = color
        )
    }
}
