package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.DailyLoginReward
import com.example.data.DailyLoginRewardConfig
import com.example.data.MythosDateUtil
import com.example.monetization.PlayerEconomyRepository
import com.example.ui.theme.MythosTokens
import com.example.ui.theme.MythosTypography

@Composable
fun DailyRewardDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val economyState by PlayerEconomyRepository.instance.economyState.collectAsState()
    val currentDay = economyState.loginRewardDay
    val canClaim = remember(economyState.lastLoginRewardDate) {
        PlayerEconomyRepository.instance.canClaimDailyLoginReward()
    }
    var claimedFeedback by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MythosTokens.PrimaryGold, RoundedCornerShape(16.dp))
                .testTag("daily_reward_dialog"),
            color = MythosTokens.Panel
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "DAILY REWARD",
                            style = MythosTypography.GameTitle.copy(fontSize = 18.sp),
                            color = MythosTokens.PrimaryGold
                        )
                        Text(
                            text = "7-Day Olympus Login Bounties",
                            style = MythosTypography.HeroTitle.copy(fontSize = 11.sp),
                            color = MythosTokens.TextMuted
                        )
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(28.dp).testTag("close_daily_reward_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MythosTokens.TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                claimedFeedback?.let { msg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MythosTokens.Success.copy(alpha = 0.2f))
                            .border(1.dp, MythosTokens.Success, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = msg,
                            style = MythosTypography.CardName.copy(fontSize = 12.sp),
                            color = MythosTokens.Success,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // 7-day reward calendar grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DailyLoginRewardConfig.REWARDS.forEach { reward ->
                        val isToday = reward.day == currentDay
                        val isPast = reward.day < currentDay || (!canClaim && isToday)
                        val isFuture = reward.day > currentDay

                        DayRewardRow(
                            reward = reward,
                            isToday = isToday,
                            isClaimed = isPast,
                            canClaimNow = isToday && canClaim,
                            onClaim = {
                                val result = PlayerEconomyRepository.instance.claimDailyLoginReward()
                                if (result.isSuccess) {
                                    claimedFeedback = "Claimed Day ${reward.day}: ${reward.title}!"
                                }
                            }
                        )
                    }
                }

                // Close button
                MythosButton(
                    text = "CLOSE",
                    onClick = onDismissRequest,
                    style = MythosButtonStyle.SECONDARY,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    testTag = "dismiss_daily_reward_button"
                )
            }
        }
    }
}

@Composable
private fun DayRewardRow(
    reward: DailyLoginReward,
    isToday: Boolean,
    isClaimed: Boolean,
    canClaimNow: Boolean,
    onClaim: () -> Unit
) {
    val highlightBorder = when {
        canClaimNow -> MythosTokens.PrimaryGold
        isToday -> MythosTokens.SecondaryGold
        else -> MythosTokens.PanelBorder
    }

    val containerBg = when {
        canClaimNow -> MythosTokens.PanelElevated
        isToday -> MythosTokens.PanelElevated.copy(alpha = 0.7f)
        else -> MythosTokens.Panel
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(if (isToday) 1.5.dp else 0.5.dp, highlightBorder, RoundedCornerShape(10.dp))
            .testTag("reward_day_${reward.day}"),
        colors = CardDefaults.cardColors(containerColor = containerBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Day number badge
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (isToday) MythosTokens.PrimaryGold.copy(alpha = 0.2f)
                            else Color(0xFF201A2C)
                        )
                        .border(
                            1.dp,
                            if (isToday) MythosTokens.PrimaryGold else MythosTokens.PanelBorder,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "D${reward.day}",
                        style = MythosTypography.HeroName.copy(fontSize = 11.sp),
                        color = if (isToday) MythosTokens.PrimaryGold else MythosTokens.TextMuted
                    )
                }

                // Reward details
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = reward.title,
                            style = MythosTypography.CardName.copy(fontSize = 13.sp),
                            color = if (isToday) MythosTokens.PrimaryGold else Color.White
                        )
                        if (isToday) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MythosTokens.PrimaryGold.copy(alpha = 0.25f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "TODAY",
                                    style = MythosTypography.RarityLabel.copy(fontSize = 8.sp),
                                    color = MythosTokens.PrimaryGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        text = reward.description,
                        style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                        color = MythosTokens.TextMuted
                    )
                }
            }

            // Action / Status
            when {
                canClaimNow -> {
                    Button(
                        onClick = onClaim,
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("claim_login_reward_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MythosTokens.PrimaryGold,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "CLAIM",
                            style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                isClaimed -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MythosTokens.Success,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "CLAIMED",
                            style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                            color = MythosTokens.Success
                        )
                    }
                }
                else -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MythosTokens.TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "LOCKED",
                            style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                            color = MythosTokens.TextMuted
                        )
                    }
                }
            }
        }
    }
}
