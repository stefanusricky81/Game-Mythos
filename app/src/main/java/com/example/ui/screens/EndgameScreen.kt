package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.*
import com.example.monetization.PlayerEconomyRepository
import com.example.ui.components.MythosButton
import com.example.ui.components.MythosButtonStyle
import com.example.ui.theme.MythosTokens
import com.example.ui.theme.MythosTypography
import java.text.NumberFormat
import java.util.Locale

enum class EndgameTab(val title: String) {
    WORLD_BOSS("WORLD BOSS"),
    RAIDS("RAIDS"),
    TRIALS("TRIALS"),
    WEEKLY("WEEKLY"),
    EVENTS("EVENTS")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EndgameScreen(
    onNavigateBack: () -> Unit,
    onOpenWorldBoss: (String) -> Unit,
    onOpenRaid: (String) -> Unit,
    onStartTrialBattle: (BattleEncounterConfig) -> Unit,
    onOpenEvents: () -> Unit,
    modifier: Modifier = Modifier
) {
    val economyState by PlayerEconomyRepository.instance.economyState.collectAsState()
    var selectedTab by remember { mutableStateOf(EndgameTab.WORLD_BOSS) }
    var selectedDifficulty by remember { mutableStateOf(EndgameDifficulty.NORMAL) }
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }
    val activeBoss = EndgameCatalog.getActiveWorldBoss()
    val bossHp = economyState.worldBossCurrentHp
    val bossHpRatio = (bossHp.toFloat() / activeBoss.maxHp.toFloat()).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MythosTokens.Background)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_battlefield_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.2f
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "ENDGAME SANCTUARY",
                            style = MythosTypography.GameTitle.copy(fontSize = 18.sp),
                            color = MythosTokens.PrimaryGold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("endgame_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MythosTokens.PrimaryGold
                            )
                        }
                    },
                    actions = {
                        // Event Tokens Balance Display
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MythosTokens.PanelElevated)
                                .border(1.dp, MythosTokens.PrimaryGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = "🪙", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${numberFormat.format(economyState.eventTokens)}",
                                style = MythosTypography.HeroName.copy(fontSize = 12.sp),
                                color = MythosTokens.PrimaryGold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MythosTokens.Panel.copy(alpha = 0.95f)
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Tab Navigation
                ScrollableTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = MythosTokens.Panel,
                    contentColor = MythosTokens.PrimaryGold,
                    edgePadding = 12.dp
                ) {
                    EndgameTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = {
                                Text(
                                    text = tab.title,
                                    style = MythosTypography.RarityLabel.copy(fontSize = 11.sp),
                                    color = if (selectedTab == tab) MythosTokens.PrimaryGold else MythosTokens.TextMuted
                                )
                            },
                            modifier = Modifier.testTag("endgame_tab_${tab.name.lowercase()}")
                        )
                    }
                }

                // Tab Content
                when (selectedTab) {
                    EndgameTab.WORLD_BOSS -> {
                        WorldBossTabContent(
                            boss = activeBoss,
                            bossHp = bossHp,
                            bossHpRatio = bossHpRatio,
                            onOpenWorldBoss = { onOpenWorldBoss(activeBoss.bossId) }
                        )
                    }
                    EndgameTab.RAIDS -> {
                        RaidsTabContent(
                            attempts = economyState.raidDailyAttempts,
                            onOpenRaid = onOpenRaid
                        )
                    }
                    EndgameTab.TRIALS -> {
                        TrialsTabContent(
                            selectedDifficulty = selectedDifficulty,
                            onSelectDifficulty = { selectedDifficulty = it },
                            starsMap = economyState.trialStars,
                            onStartTrialBattle = onStartTrialBattle
                        )
                    }
                    EndgameTab.WEEKLY -> {
                        WeeklyObjectivesTabContent(
                            objectives = economyState.weeklyObjectives,
                            onClaim = { id -> PlayerEconomyRepository.instance.claimWeeklyObjective(id) }
                        )
                    }
                    EndgameTab.EVENTS -> {
                        EventsShortcutTabContent(
                            events = economyState.activeEvents,
                            onOpenEvents = onOpenEvents
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorldBossTabContent(
    boss: WorldBossDefinition,
    bossHp: Long,
    bossHpRatio: Float,
    onOpenWorldBoss: () -> Unit
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, MythosTokens.Damage, RoundedCornerShape(16.dp))
                    .clickable { onOpenWorldBoss() }
                    .testTag("endgame_world_boss_card"),
                colors = CardDefaults.cardColors(containerColor = MythosTokens.PanelElevated)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(MythosTokens.Panel)
                            .border(2.dp, MythosTokens.Damage, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = boss.portraitResId),
                            contentDescription = boss.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = boss.name.uppercase(),
                        style = MythosTypography.HeroName.copy(fontSize = 20.sp),
                        color = MythosTokens.PrimaryGold
                    )
                    Text(
                        text = "${boss.title} • ${boss.mythology}",
                        style = MythosTypography.HeroTitle.copy(fontSize = 11.sp),
                        color = MythosTokens.TextMuted
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // HP Bar
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "BOSS HEALTH",
                                style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                                color = MythosTokens.Damage
                            )
                            Text(
                                text = "${numberFormat.format(bossHp)} / ${numberFormat.format(boss.maxHp)}",
                                style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { bossHpRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = MythosTokens.Damage,
                            trackColor = MythosTokens.Background
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    MythosButton(
                        text = "VIEW WORLD BOSS & BATTLE",
                        subtitle = "Contribution • Leaderboard • Enter Combat",
                        onClick = onOpenWorldBoss,
                        style = MythosButtonStyle.PRIMARY,
                        icon = Icons.Default.Whatshot,
                        testTag = "open_world_boss_screen_button",
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RaidsTabContent(
    attempts: Int,
    onOpenRaid: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MythosTokens.PanelElevated)
                    .border(1.dp, MythosTokens.PanelBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DAILY RAID ATTEMPTS",
                            style = MythosTypography.RarityLabel,
                            color = MythosTokens.PrimaryGold
                        )
                        Text(
                            text = "Resets daily at 00:00 local time",
                            style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                            color = MythosTokens.TextMuted
                        )
                    }
                    Text(
                        text = "$attempts / 3",
                        style = MythosTypography.HeroName.copy(fontSize = 18.sp),
                        color = if (attempts > 0) MythosTokens.PrimaryGold else MythosTokens.Error
                    )
                }
            }
        }

        items(EndgameCatalog.ALL_RAIDS) { raid ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MythosTokens.PanelBorder, RoundedCornerShape(12.dp))
                    .clickable { onOpenRaid(raid.raidId) }
                    .testTag("endgame_raid_card_${raid.raidId}"),
                colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = raid.name.uppercase(),
                            style = MythosTypography.HeroName.copy(fontSize = 16.sp),
                            color = MythosTokens.PrimaryGold
                        )
                        Text(
                            text = raid.difficulty.displayName.uppercase(),
                            style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                            color = MythosTokens.Damage
                        )
                    }

                    Text(
                        text = "${raid.subtitle} • ${raid.stages.size} Stages",
                        style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                        color = MythosTokens.TextMuted
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = raid.rewardSummary,
                        style = MythosTypography.CardName.copy(fontSize = 11.sp),
                        color = MythosTokens.DivineBlueLight
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    MythosButton(
                        text = "ENTER RAID",
                        subtitle = "Recommended Power: ${raid.recommendedPower}",
                        onClick = { onOpenRaid(raid.raidId) },
                        style = MythosButtonStyle.SECONDARY,
                        icon = Icons.Default.Shield,
                        testTag = "enter_raid_button_${raid.raidId}",
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrialsTabContent(
    selectedDifficulty: EndgameDifficulty,
    onSelectDifficulty: (EndgameDifficulty) -> Unit,
    starsMap: Map<String, Int>,
    onStartTrialBattle: (BattleEncounterConfig) -> Unit
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Difficulty Selector
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "TRIAL DIFFICULTY",
                    style = MythosTypography.RarityLabel,
                    color = MythosTokens.PrimaryGold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EndgameDifficulty.entries.forEach { diff ->
                        val isSelected = selectedDifficulty == diff
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MythosTokens.PrimaryGold else MythosTokens.PanelElevated)
                                .border(
                                    1.dp,
                                    if (isSelected) MythosTokens.PrimaryGold else MythosTokens.PanelBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelectDifficulty(diff) }
                                .padding(vertical = 8.dp)
                                .testTag("trial_difficulty_${diff.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = diff.displayName.uppercase(),
                                style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                                color = if (isSelected) Color.Black else Color.White
                            )
                        }
                    }
                }
            }
        }

        items(EndgameCatalog.ALL_TRIALS) { trial ->
            val stars = starsMap[trial.stageId] ?: 0
            val power = (trial.baseRecommendedPower * selectedDifficulty.recommendedPowerMultiplier).toInt()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MythosTokens.PanelBorder, RoundedCornerShape(12.dp))
                    .testTag("endgame_trial_card_${trial.stageId}"),
                colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = trial.name.uppercase(),
                            style = MythosTypography.HeroName.copy(fontSize = 15.sp),
                            color = MythosTokens.PrimaryGold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            for (i in 1..3) {
                                Text(
                                    text = if (i <= stars) "★" else "☆",
                                    fontSize = 14.sp,
                                    color = if (i <= stars) MythosTokens.PrimaryGold else MythosTokens.TextMuted
                                )
                            }
                        }
                    }

                    Text(
                        text = "${trial.world} • ${trial.enemyName} • Power: ${numberFormat.format(power)}",
                        style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                        color = MythosTokens.TextMuted
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Rewards: +${trial.baseEventTokens} Tokens • +${numberFormat.format((trial.baseGoldReward * selectedDifficulty.rewardMultiplier).toInt())} G",
                        style = MythosTypography.CardName.copy(fontSize = 11.sp),
                        color = MythosTokens.DivineBlueLight
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    MythosButton(
                        text = "START TRIAL",
                        subtitle = "Difficulty: ${selectedDifficulty.displayName}",
                        onClick = {
                            val encounter = trial.toBattleEncounterConfig(selectedDifficulty).copy(
                                isTrial = true,
                                trialStageId = trial.stageId,
                                endgameDifficulty = selectedDifficulty,
                                eventTokensReward = trial.baseEventTokens
                            )
                            onStartTrialBattle(encounter)
                        },
                        style = MythosButtonStyle.SECONDARY,
                        icon = Icons.Default.PlayArrow,
                        testTag = "start_trial_button_${trial.stageId}",
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyObjectivesTabContent(
    objectives: List<WeeklyObjective>,
    onClaim: (String) -> Unit
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "WEEKLY OBJECTIVES (RESETS WEEKLY)",
                style = MythosTypography.RarityLabel,
                color = MythosTokens.PrimaryGold
            )
        }

        items(objectives) { obj ->
            val ratio = (obj.progress.toFloat() / obj.target.toFloat()).coerceIn(0f, 1f)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(
                        1.dp,
                        if (obj.canClaim) MythosTokens.PrimaryGold else MythosTokens.PanelBorder,
                        RoundedCornerShape(10.dp)
                    )
                    .testTag("weekly_objective_card_${obj.id}"),
                colors = CardDefaults.cardColors(containerColor = MythosTokens.PanelElevated)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = obj.title.uppercase(),
                            style = MythosTypography.CardName.copy(fontSize = 13.sp),
                            color = if (obj.canClaim) MythosTokens.PrimaryGold else Color.White
                        )
                        Text(
                            text = "${numberFormat.format(obj.progress)} / ${numberFormat.format(obj.target)}",
                            style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                            color = if (obj.isCompleted) MythosTokens.Success else MythosTokens.TextMuted
                        )
                    }

                    Text(
                        text = obj.description,
                        style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                        color = MythosTokens.TextMuted
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (obj.isCompleted) MythosTokens.Success else MythosTokens.PrimaryGold,
                        trackColor = MythosTokens.Background
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reward: +${numberFormat.format(obj.goldReward)} G • +${obj.eventTokensReward} Tokens",
                            style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                            color = MythosTokens.DivineBlueLight
                        )

                        if (obj.isClaimed) {
                            Text(
                                text = "CLAIMED",
                                style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                                color = MythosTokens.TextMuted
                            )
                        } else {
                            Button(
                                onClick = { onClaim(obj.id) },
                                enabled = obj.canClaim,
                                modifier = Modifier.testTag("claim_weekly_${obj.id}"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MythosTokens.PrimaryGold,
                                    disabledContainerColor = MythosTokens.PanelBorder
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "CLAIM",
                                    style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                                    color = if (obj.canClaim) Color.Black else MythosTokens.TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventsShortcutTabContent(
    events: List<MythosEvent>,
    onOpenEvents: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            MythosButton(
                text = "OPEN FULL EVENTS & SHOP HUB",
                subtitle = "Active Campaigns • Event Token Reward Shop",
                onClick = onOpenEvents,
                style = MythosButtonStyle.PRIMARY,
                icon = Icons.Default.Whatshot,
                testTag = "open_events_hub_from_endgame_button",
                modifier = Modifier.fillMaxWidth().height(48.dp)
            )
        }

        items(events) { event ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, MythosTokens.PanelBorder, RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = event.title,
                            style = MythosTypography.CardName.copy(fontSize = 13.sp),
                            color = MythosTokens.PrimaryGold
                        )
                        Text(
                            text = if (event.isCurrentlyActive) "LIVE" else "UPCOMING",
                            style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                            color = if (event.isCurrentlyActive) MythosTokens.Success else MythosTokens.TextMuted
                        )
                    }
                    Text(
                        text = event.description,
                        style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                        color = MythosTokens.TextMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Rewards: ${event.rewardSummary}",
                        style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                        color = MythosTokens.DivineBlueLight
                    )
                }
            }
        }
    }
}
