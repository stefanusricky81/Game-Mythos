package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.backend.MythosBackend
import com.example.backend.online.BackendException
import com.example.backend.online.OnlineBackend
import com.example.data.*
import com.example.ui.components.OnlineStatusBanner
import com.example.ui.components.rememberDisplayEconomyState
import kotlinx.coroutines.launch
import com.example.monetization.PlayerEconomyRepository
import com.example.ui.components.MythosButton
import com.example.ui.components.MythosButtonStyle
import com.example.ui.theme.MythosTokens
import com.example.ui.theme.MythosTypography
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaidScreen(
    raidId: String = "raid_olympus",
    onNavigateBack: () -> Unit,
    onStartRaidStageBattle: (BattleEncounterConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val economyState by rememberDisplayEconomyState()
    val raid = remember(raidId) { EndgameCatalog.findRaid(raidId) ?: EndgameCatalog.OLYMPUS_RAID }
    var selectedDifficulty by remember { mutableStateOf(raid.difficulty) }
    var selectedStageIndex by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val isOnline = MythosBackend.isOnline
    LaunchedEffect(Unit) { if (isOnline) MythosBackend.online.refreshAll() }
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }
    val remainingAttempts = economyState.raidDailyAttempts
    val completedStagesForRaid = economyState.raidStageProgress[raid.raidId] ?: 0

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
                            text = raid.name.uppercase(),
                            style = MythosTypography.GameTitle.copy(fontSize = 18.sp),
                            color = MythosTokens.PrimaryGold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("raid_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MythosTokens.PrimaryGold
                            )
                        }
                    },
                    actions = {
                        // Daily Attempts Badge (Phase 10 Section 9)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MythosTokens.PanelElevated)
                                .border(1.dp, MythosTokens.PrimaryGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = "🛡️", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ATTEMPTS: $remainingAttempts/3",
                                style = MythosTypography.RarityLabel.copy(fontSize = 11.sp),
                                color = if (remainingAttempts > 0) MythosTokens.PrimaryGold else MythosTokens.Error
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OnlineStatusBanner(onRetry = { coroutineScope.launch { MythosBackend.online.refreshAll() } })

                // Raid Banner & Subtitle
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MythosTokens.PanelBorder, RoundedCornerShape(12.dp))
                        .testTag("raid_banner_card"),
                    colors = CardDefaults.cardColors(containerColor = MythosTokens.PanelElevated)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = raid.subtitle,
                            style = MythosTypography.HeroName.copy(fontSize = 16.sp),
                            color = MythosTokens.PrimaryGold
                        )
                        Text(
                            text = "${raid.mythology} • ${raid.entryRequirements}",
                            style = MythosTypography.HeroTitle.copy(fontSize = 11.sp),
                            color = MythosTokens.TextMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Rewards: ${raid.rewardSummary}",
                            style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                            color = MythosTokens.DivineBlueLight
                        )
                    }
                }

                // Difficulty Selector (Phase 10 Section 1)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "DIFFICULTY TIER",
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
                                    .clickable { selectedDifficulty = diff }
                                    .padding(vertical = 8.dp)
                                    .testTag("raid_difficulty_${diff.id}"),
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

                // Stages List (Phase 10 Section 8)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "RAID PATH (${raid.stages.size} STAGES)",
                        style = MythosTypography.RarityLabel,
                        color = MythosTokens.PrimaryGold
                    )

                    raid.stages.forEachIndexed { index, stage ->
                        val isSelected = selectedStageIndex == index
                        val isCleared = completedStagesForRaid >= stage.stageNumber
                        val power = (stage.recommendedPower * selectedDifficulty.recommendedPowerMultiplier).toInt()

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    1.dp,
                                    if (isSelected) MythosTokens.PrimaryGold else MythosTokens.PanelBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedStageIndex = index }
                                .testTag("raid_stage_card_${stage.stageNumber}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MythosTokens.PanelElevated else MythosTokens.Panel
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isCleared) MythosTokens.Success else MythosTokens.PanelBorder),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isCleared) "✓" else "${stage.stageNumber}",
                                            style = MythosTypography.HeroName.copy(fontSize = 13.sp),
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = stage.name.uppercase(),
                                            style = MythosTypography.CardName.copy(fontSize = 13.sp),
                                            color = if (stage.isBossStage) MythosTokens.Damage else MythosTokens.PrimaryGold
                                        )
                                        Text(
                                            text = "${stage.enemyName} • Power: ${numberFormat.format(power)}",
                                            style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                                            color = MythosTokens.TextMuted
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "+${stage.eventTokensReward} Tokens",
                                        style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                                        color = MythosTokens.PrimaryGold
                                    )
                                    Text(
                                        text = "+${numberFormat.format((stage.goldReward * selectedDifficulty.rewardMultiplier).toInt())} G",
                                        style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                                        color = MythosTokens.TextMuted
                                    )
                                }
                            }
                        }
                    }
                }

                // Error Message if attempts exhausted
                errorMessage?.let { errorText: String ->
                    Text(
                        text = errorText,
                        style = MythosTypography.CardDescription,
                        color = MythosTokens.Error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // START RAID BATTLE CTA (Consumes 1 daily attempt)
                val currentSelectedStage = raid.stages.getOrNull(selectedStageIndex) ?: raid.stages.first()

                MythosButton(
                    text = "ENGAGE STAGE ${currentSelectedStage.stageNumber}",
                    subtitle = "Consumes 1 Daily Attempt (${remainingAttempts}/3 Left)",
                    onClick = {
                        if (isOnline) {
                            // The server verifies Alliance membership, stage unlocks and the 3 daily attempts,
                            // consumes the attempt, and tells us exactly which enemy HP to fight.
                            coroutineScope.launch {
                                MythosBackend.online.startRaid(raid.raidId, currentSelectedStage.stageNumber, selectedDifficulty.id).fold(
                                    onSuccess = { session ->
                                        errorMessage = null
                                        val base = currentSelectedStage.toBattleEncounterConfig(raidId = raid.raidId, difficulty = selectedDifficulty)
                                        onStartRaidStageBattle(
                                            base.copy(
                                                enemyHero = base.enemyHero.copy(currentHp = session.enemyHp, maxHp = session.enemyHp),
                                                rewards = BattleRewards(gold = 0, xp = 0, cardRewardName = "Decided by the server", cardRewardRarity = CardRarity.RARE),
                                                isRaid = true,
                                                raidId = raid.raidId,
                                                eventTokensReward = 0,
                                                onlineSessionId = session.sessionId
                                            )
                                        )
                                    },
                                    onFailure = { e ->
                                        errorMessage = (e as? BackendException)?.let(OnlineBackend::userMessage) ?: e.message ?: "Could not start the raid."
                                    }
                                )
                            }
                            return@MythosButton
                        }
                        val consumed = PlayerEconomyRepository.instance.consumeRaidAttempt()
                        if (!consumed) {
                            errorMessage = "No daily raid attempts remaining! Resets daily at midnight."
                        } else {
                            errorMessage = null
                            val encounter = currentSelectedStage.toBattleEncounterConfig(
                                raidId = raid.raidId,
                                difficulty = selectedDifficulty
                            ).copy(
                                isRaid = true,
                                raidId = raid.raidId,
                                eventTokensReward = currentSelectedStage.eventTokensReward
                            )
                            onStartRaidStageBattle(encounter)
                        }
                    },
                    style = MythosButtonStyle.PRIMARY,
                    icon = Icons.Default.PlayArrow,
                    testTag = "start_raid_stage_button",
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                )
            }
        }
    }
}
