package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.Alliance
import com.example.data.AllianceEmblems
import com.example.data.AllianceProgressionConfig
import com.example.backend.online.AllianceGateway
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
fun AllianceDetailScreen(
    onNavigateBack: () -> Unit,
    onOpenMembers: () -> Unit,
    onAllianceLeft: () -> Unit,
    modifier: Modifier = Modifier
) {
    val economyState by rememberDisplayEconomyState()
    val coroutineScope = rememberCoroutineScope()
    val alliance = economyState.playerAlliance
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }
    var showLeaveDialog by remember { mutableStateOf(false) }

    if (alliance == null) {
        LaunchedEffect(Unit) {
            onNavigateBack()
        }
        return
    }

    val emblem = remember(alliance.emblem) { AllianceEmblems.find(alliance.emblem) }
    val leader = alliance.leader

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
                            text = "ALLIANCE DETAILS",
                            style = MythosTypography.GameTitle.copy(fontSize = 18.sp),
                            color = MythosTokens.PrimaryGold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("alliance_detail_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MythosTokens.PrimaryGold
                            )
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Main Header Banner
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, MythosTokens.PrimaryGold, RoundedCornerShape(16.dp))
                            .testTag("alliance_detail_header_card"),
                        colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(Color(emblem.colorHex).copy(alpha = 0.25f))
                                        .border(2.dp, Color(emblem.colorHex), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emblem.iconEmoji, fontSize = 32.sp)
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = alliance.name,
                                        style = MythosTypography.HeroName.copy(fontSize = 20.sp),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Leader: ${leader?.name ?: "Unknown"}",
                                        style = MythosTypography.CardDescription.copy(fontSize = 12.sp),
                                        color = MythosTokens.PrimaryGold
                                    )
                                    Text(
                                        text = "Founded: ${alliance.createdDate}",
                                        style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                                        color = MythosTokens.TextMuted
                                    )
                                }
                            }

                            Text(
                                text = "\"${alliance.description}\"",
                                style = MythosTypography.CardDescription.copy(fontSize = 12.sp),
                                color = Color(0xFFD4CCE6)
                            )

                            // Quick Stats Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DetailStatBadge(
                                    modifier = Modifier.weight(1f),
                                    title = "LEVEL",
                                    value = "${alliance.level}",
                                    color = MythosTokens.PrimaryGold
                                )
                                DetailStatBadge(
                                    modifier = Modifier.weight(1f),
                                    title = "POWER",
                                    value = numberFormat.format(alliance.totalPower),
                                    color = MythosTokens.DivineBlueLight
                                )
                                DetailStatBadge(
                                    modifier = Modifier.weight(1f),
                                    title = "MEMBERS",
                                    value = "${alliance.memberCount}/${Alliance.MAX_MEMBERS}",
                                    color = MythosTokens.Success
                                )
                            }

                            // XP Progress bar
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "LEVEL PROGRESSION",
                                        style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                                        color = MythosTokens.PrimaryGold
                                    )
                                    Text(
                                        text = if (alliance.level >= AllianceProgressionConfig.MAX_ALLIANCE_LEVEL) "MAX LEVEL REACHED"
                                        else "${numberFormat.format(alliance.xp)} / ${numberFormat.format(alliance.xpRequiredForNextLevel)} XP",
                                        style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Color(0xFF1E182A))
                                        .border(0.5.dp, MythosTokens.PanelBorder, RoundedCornerShape(5.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(alliance.xpProgressFraction)
                                            .clip(RoundedCornerShape(5.dp))
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

                // Current Active Perk
                item {
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
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = "🛡️", fontSize = 24.sp)
                            Column {
                                Text(
                                    text = "ACTIVE ALLIANCE BLESSING",
                                    style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                                    color = MythosTokens.PrimaryGold
                                )
                                Text(
                                    text = AllianceProgressionConfig.getPerkDescription(alliance.level),
                                    style = MythosTypography.CardDescription.copy(fontSize = 12.sp),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Roster Navigation CTA
                item {
                    MythosButton(
                        text = "VIEW FULL MEMBER ROSTER",
                        subtitle = "Manage roles, permissions & members",
                        onClick = onOpenMembers,
                        style = MythosButtonStyle.SECONDARY,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("detail_view_roster_button")
                    )
                }

                // Contribution Ranking Header
                item {
                    Text(
                        text = "CONTRIBUTION RANKINGS",
                        style = MythosTypography.RarityLabel.copy(fontSize = 12.sp),
                        color = MythosTokens.PrimaryGold
                    )
                }

                // Contribution ranking rows
                itemsIndexed(alliance.sortedByContribution) { index, member ->
                    val isTop3 = index < 3
                    val rankBadgeColor = when (index) {
                        0 -> MythosTokens.PrimaryGold
                        1 -> MythosTokens.SecondaryGold
                        2 -> MythosTokens.MetallicBronze
                        else -> MythosTokens.TextMuted
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                0.5.dp,
                                if (isTop3) rankBadgeColor.copy(alpha = 0.6f) else MythosTokens.PanelBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .testTag("contribution_row_${member.playerId}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isTop3) MythosTokens.PanelElevated else MythosTokens.Panel
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(rankBadgeColor.copy(alpha = 0.2f))
                                        .border(1.dp, rankBadgeColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "#${index + 1}",
                                        style = MythosTypography.HeroName.copy(fontSize = 12.sp),
                                        color = rankBadgeColor
                                    )
                                }

                                Column {
                                    Text(
                                        text = member.name,
                                        style = MythosTypography.CardName.copy(fontSize = 13.sp),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${member.battlesWon} Wins • ${numberFormat.format(member.damageDealt)} Dmg • ${member.campaignClears} Clears",
                                        style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                                        color = MythosTokens.TextMuted
                                    )
                                }
                            }

                            Text(
                                text = "${numberFormat.format(member.contributionScore)} pts",
                                style = MythosTypography.HeroName.copy(fontSize = 13.sp),
                                color = MythosTokens.PrimaryGold
                            )
                        }
                    }
                }

                // Leave Alliance button
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { showLeaveDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("detail_leave_alliance_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MythosTokens.Damage),
                        border = BorderStroke(1.dp, MythosTokens.Damage.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("LEAVE ALLIANCE", style = MythosTypography.RarityLabel.copy(fontSize = 11.sp))
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        if (showLeaveDialog) {
            AlertDialog(
                onDismissRequest = { showLeaveDialog = false },
                title = { Text("Leave Alliance?", color = MythosTokens.Damage) },
                text = {
                    Text(
                        "Are you sure you want to leave ${alliance.name}? You will lose alliance perks and active membership.",
                        color = Color.White
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showLeaveDialog = false
                            coroutineScope.launch {
                                if (AllianceGateway.leave().isSuccess) onAllianceLeft()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MythosTokens.Damage)
                    ) {
                        Text("CONFIRM LEAVE")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLeaveDialog = false }) {
                        Text("CANCEL", color = MythosTokens.TextMuted)
                    }
                },
                containerColor = MythosTokens.Panel
            )
        }
    }
}

@Composable
private fun DetailStatBadge(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MythosTokens.PanelElevated),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                color = MythosTokens.TextMuted
            )
            Text(
                text = value,
                style = MythosTypography.HeroName.copy(fontSize = 13.sp),
                color = color
            )
        }
    }
}
