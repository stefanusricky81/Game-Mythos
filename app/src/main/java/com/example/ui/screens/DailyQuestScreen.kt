package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.DailyQuest
import com.example.data.MythosDateUtil
import com.example.data.QuestReward
import com.example.monetization.PlayerEconomyRepository
import com.example.ui.components.MythosButton
import com.example.ui.components.MythosButtonStyle
import com.example.ui.theme.MythosTokens
import com.example.ui.theme.MythosTypography
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyQuestScreen(
    onNavigateBack: () -> Unit,
    onOpenDailyLoginRewards: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val economyState by PlayerEconomyRepository.instance.economyState.collectAsState()
    val quests = economyState.dailyQuests
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }
    var claimMessage by remember { mutableStateOf<String?>(null) }

    val completedCount = quests.count { it.isCompleted }
    val readyToClaimCount = quests.count { it.isCompleted && !it.isClaimed }
    val canClaimLoginReward = remember(economyState.lastLoginRewardDate) {
        PlayerEconomyRepository.instance.canClaimDailyLoginReward()
    }

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
                        Column {
                            Text(
                                text = "DAILY QUESTS",
                                style = MythosTypography.GameTitle.copy(fontSize = 18.sp),
                                color = MythosTokens.PrimaryGold
                            )
                            Text(
                                text = "Resets Daily • ${MythosDateUtil.getCurrentLocalDate()}",
                                style = MythosTypography.HeroTitle.copy(fontSize = 11.sp),
                                color = MythosTokens.TextMuted
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("quests_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MythosTokens.PrimaryGold
                            )
                        }
                    },
                    actions = {
                        // Quick button to open Daily Rewards
                        TextButton(
                            onClick = onOpenDailyLoginRewards,
                            modifier = Modifier.testTag("open_login_rewards_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = "🎁", fontSize = 14.sp)
                                Text(
                                    text = "LOGIN REWARD",
                                    style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                                    color = if (canClaimLoginReward) MythosTokens.PrimaryGold else MythosTokens.TextMuted
                                )
                                if (canClaimLoginReward) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MythosTokens.Damage)
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MythosTokens.Panel.copy(alpha = 0.95f)
                    )
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                // Retention Hub: Streak & Weekly Activity Summary (Phase 8 Section 5)
                item {
                    val progress = economyState.playerProgress
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MythosTokens.PanelBorder, RoundedCornerShape(12.dp))
                            .testTag("retention_summary_card"),
                        colors = CardDefaults.cardColors(containerColor = MythosTokens.PanelElevated)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(text = "🔥", fontSize = 18.sp)
                                    Column {
                                        Text(
                                            text = "${progress.loginStreak}-DAY STREAK",
                                            style = MythosTypography.HeroName.copy(fontSize = 13.sp),
                                            color = MythosTokens.PrimaryGold
                                        )
                                        Text(
                                            text = "Best: ${progress.highestLoginStreak} Days",
                                            style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                                            color = MythosTokens.TextMuted
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(text = "⚔️", fontSize = 16.sp)
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "WEEKLY ACTIVITY",
                                            style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                                            color = MythosTokens.TextMuted
                                        )
                                        Text(
                                            text = "${progress.weeklyBattlesWon} Battles • ${progress.weeklyQuestsCompleted} Quests",
                                            style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            // Daily Quests Overall Completion Progress
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "DAILY QUEST COMPLETION",
                                        style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                                        color = MythosTokens.SecondaryGold
                                    )
                                    Text(
                                        text = "$completedCount / ${quests.size} Complete",
                                        style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                                        color = if (completedCount == quests.size) MythosTokens.Success else MythosTokens.DivineBlueLight
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF1E182A))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth((completedCount.toFloat() / quests.size.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f))
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFF0284C7), MythosTokens.PrimaryGold)
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                // Header Status Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MythosTokens.PanelBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "OLYMPIAN TRIALS",
                                    style = MythosTypography.RarityLabel.copy(fontSize = 11.sp),
                                    color = MythosTokens.PrimaryGold
                                )
                                Text(
                                    text = "$completedCount of ${quests.size} Completed",
                                    style = MythosTypography.HeroName.copy(fontSize = 16.sp),
                                    color = Color.White
                                )
                            }

                            if (readyToClaimCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(MythosTokens.PrimaryGold)
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "$readyToClaimCount READY TO CLAIM",
                                        style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(MythosTokens.PanelElevated)
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (completedCount == quests.size) "ALL CLAIMED" else "IN PROGRESS",
                                        style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                                        color = MythosTokens.TextMuted
                                    )
                                }
                            }
                        }
                    }
                }

                // Temporary claim message toast
                claimMessage?.let { msg ->
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MythosTokens.Success, RoundedCornerShape(8.dp)),
                            colors = CardDefaults.cardColors(containerColor = MythosTokens.Success.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = msg,
                                    style = MythosTypography.CardName.copy(fontSize = 12.sp),
                                    color = MythosTokens.Success
                                )
                                IconButton(
                                    onClick = { claimMessage = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = MythosTokens.Success,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 5 Daily Quests (Phase 7D Section 10, 14)
                items(quests, key = { it.questId }) { quest ->
                    DailyQuestCard(
                        quest = quest,
                        numberFormat = numberFormat,
                        onClaim = {
                            val result = PlayerEconomyRepository.instance.claimQuestReward(quest.questId)
                            if (result.isSuccess) {
                                claimMessage = "Claimed reward: ${quest.reward.toDisplayText()}"
                            }
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun DailyQuestCard(
    quest: DailyQuest,
    numberFormat: NumberFormat,
    onClaim: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompleted = quest.isCompleted
    val isClaimed = quest.isClaimed
    val canClaim = isCompleted && !isClaimed

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (canClaim) MythosTokens.PrimaryGold else MythosTokens.PanelBorder,
                RoundedCornerShape(12.dp)
            )
            .testTag("quest_card_${quest.questId}"),
        colors = CardDefaults.cardColors(
            containerColor = if (canClaim) MythosTokens.PanelElevated else MythosTokens.Panel
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Title & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quest.title,
                        style = MythosTypography.CardName.copy(fontSize = 15.sp),
                        color = if (canClaim) MythosTokens.PrimaryGold else Color.White
                    )
                    Text(
                        text = quest.description,
                        style = MythosTypography.CardDescription.copy(fontSize = 12.sp),
                        color = MythosTokens.TextMuted
                    )
                }

                // Status tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                isClaimed -> MythosTokens.PanelBorder
                                canClaim -> MythosTokens.Success.copy(alpha = 0.2f)
                                else -> Color(0xFF231C30)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when {
                            isClaimed -> "CLAIMED"
                            canClaim -> "COMPLETE"
                            else -> "IN PROGRESS"
                        },
                        style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                        color = when {
                            isClaimed -> MythosTokens.TextMuted
                            canClaim -> MythosTokens.Success
                            else -> MythosTokens.SecondaryGold
                        }
                    )
                }
            }

            // Progress bar & numeric progress
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "PROGRESS",
                        style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                        color = MythosTokens.TextMuted
                    )
                    Text(
                        text = "${numberFormat.format(quest.progress)} / ${numberFormat.format(quest.target)}",
                        style = MythosTypography.HeroName.copy(fontSize = 11.sp),
                        color = if (isCompleted) MythosTokens.Success else Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1B1527))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(quest.progressFraction)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isCompleted) MythosTokens.Success
                                else MythosTokens.PrimaryGold
                            )
                    )
                }
            }

            // Reward Row & Claim Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Reward Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "REWARD:",
                        style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                        color = MythosTokens.TextMuted
                    )
                    RewardChips(reward = quest.reward, numberFormat = numberFormat)
                }

                // Action Button
                if (canClaim) {
                    Button(
                        onClick = onClaim,
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("claim_quest_${quest.questId}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MythosTokens.PrimaryGold,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "CLAIM",
                            style = MythosTypography.RarityLabel.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.Black
                        )
                    }
                } else if (isClaimed) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MythosTokens.Success,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "CLAIMED",
                            style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                            color = MythosTokens.TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RewardChips(
    reward: QuestReward,
    numberFormat: NumberFormat
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (reward.gold > 0) {
            RewardPill(icon = "🪙", label = numberFormat.format(reward.gold), color = MythosTokens.PrimaryGold)
        }
        if (reward.mythGems > 0) {
            RewardPill(icon = "💎", label = numberFormat.format(reward.mythGems), color = MythosTokens.DivineBlueLight)
        }
        if (reward.cardShards > 0) {
            RewardPill(icon = "🃏", label = "+${reward.cardShards}", color = MythosTokens.Epic)
        }
        if (reward.heroShards > 0) {
            RewardPill(icon = "🛡️", label = "+${reward.heroShards}", color = Color(0xFFA78BFA))
        }
        if (reward.playerXp > 0) {
            RewardPill(icon = "✦", label = "+${reward.playerXp} XP", color = Color(0xFF38BDF8))
        }
    }
}

@Composable
private fun RewardPill(
    icon: String,
    label: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF261F33))
            .border(0.5.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = icon, fontSize = 10.sp)
            Text(
                text = label,
                style = MythosTypography.HeroName.copy(fontSize = 10.sp),
                color = color
            )
        }
    }
}
