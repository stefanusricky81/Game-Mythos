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
import com.example.data.Alliance
import com.example.data.AllianceEmblems
import com.example.data.AllianceProgressionConfig
import com.example.backend.online.AllianceGateway
import com.example.ui.components.rememberDisplayEconomyState
import kotlinx.coroutines.launch
import com.example.backend.MythosBackend
import com.example.ui.components.OnlineStatusBanner
import com.example.monetization.PlayerEconomyRepository
import com.example.ui.components.MythosButton
import com.example.ui.components.MythosButtonStyle
import com.example.ui.theme.MythosTokens
import com.example.ui.theme.MythosTypography
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllianceScreen(
    onNavigateBack: () -> Unit,
    onOpenCreateAlliance: () -> Unit,
    onOpenJoinAlliance: () -> Unit,
    onOpenAllianceDetail: () -> Unit,
    onOpenAllianceMembers: () -> Unit,
    modifier: Modifier = Modifier
) {
    val economyState by rememberDisplayEconomyState()
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) { if (MythosBackend.isOnline) MythosBackend.online.refreshAll() }
    val playerAlliance = economyState.playerAlliance
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }
    var showLeaveConfirmDialog by remember { mutableStateOf(false) }
    var actionErrorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MythosTokens.Background)
    ) {
        // Background art overlay
        Image(
            painter = painterResource(id = R.drawable.img_battlefield_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.2f
        )

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = { OnlineStatusBanner(onRetry = { coroutineScope.launch { MythosBackend.online.refreshAll() } }) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (playerAlliance != null) playerAlliance.name.uppercase() else "ALLIANCE HALL",
                            style = MythosTypography.GameTitle.copy(fontSize = 18.sp),
                            color = MythosTokens.PrimaryGold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("alliance_back_button")
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
            if (playerAlliance != null) {
                // In Alliance view
                AllianceActiveHub(
                    alliance = playerAlliance,
                    numberFormat = numberFormat,
                    onOpenDetail = onOpenAllianceDetail,
                    onOpenMembers = onOpenAllianceMembers,
                    onRequestLeave = { showLeaveConfirmDialog = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            } else {
                // Not in Alliance view (Browse & Create)
                AllianceLobbyView(
                    alliances = economyState.alliances,
                    playerGold = economyState.gold,
                    numberFormat = numberFormat,
                    onOpenCreate = onOpenCreateAlliance,
                    onOpenJoin = onOpenJoinAlliance,
                    onQuickJoin = { allianceId ->
                        coroutineScope.launch {
                            val result = AllianceGateway.join(allianceId)
                            if (result.isFailure) {
                                actionErrorMessage = result.exceptionOrNull()?.message ?: "Failed to join alliance"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }

        // Leave Confirmation Dialog
        if (showLeaveConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showLeaveConfirmDialog = false },
                title = {
                    Text(
                        text = "LEAVE ALLIANCE?",
                        style = MythosTypography.HeroName.copy(fontSize = 18.sp),
                        color = MythosTokens.Damage
                    )
                },
                text = {
                    Text(
                        text = if (economyState.isLeaderOfAlliance) {
                            "You are the Leader. Leaving will transfer leadership to the next highest contributor, or disband the alliance if no members remain."
                        } else {
                            "Are you sure you want to leave ${playerAlliance?.name}? You will forfeit any uncollected alliance perks."
                        },
                        style = MythosTypography.CardDescription,
                        color = Color(0xFFD4CCE6)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showLeaveConfirmDialog = false
                            coroutineScope.launch {
                                val result = AllianceGateway.leave()
                                if (result.isFailure) {
                                    actionErrorMessage = result.exceptionOrNull()?.message
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MythosTokens.Damage),
                        modifier = Modifier.testTag("confirm_leave_alliance_button")
                    ) {
                        Text("LEAVE", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLeaveConfirmDialog = false }) {
                        Text("CANCEL", color = MythosTokens.TextMuted)
                    }
                },
                containerColor = MythosTokens.Panel,
                tonalElevation = 8.dp
            )
        }

        // Error snackbar / dialog
        actionErrorMessage?.let { msg ->
            AlertDialog(
                onDismissRequest = { actionErrorMessage = null },
                title = { Text("Alliance Notice", color = MythosTokens.PrimaryGold) },
                text = { Text(msg, color = Color.White) },
                confirmButton = {
                    Button(onClick = { actionErrorMessage = null }) {
                        Text("OK")
                    }
                },
                containerColor = MythosTokens.Panel
            )
        }
    }
}

@Composable
private fun AllianceActiveHub(
    alliance: Alliance,
    numberFormat: NumberFormat,
    onOpenDetail: () -> Unit,
    onOpenMembers: () -> Unit,
    onRequestLeave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emblem = remember(alliance.emblem) { AllianceEmblems.find(alliance.emblem) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Heroic Alliance Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MythosTokens.PrimaryGold, RoundedCornerShape(16.dp))
                    .testTag("alliance_hub_card"),
                colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(emblem.colorHex).copy(alpha = 0.25f))
                                .border(2.dp, Color(emblem.colorHex), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emblem.iconEmoji, fontSize = 28.sp)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "LEVEL ${alliance.level} ALLIANCE",
                                    style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                                    color = MythosTokens.PrimaryGold,
                                    letterSpacing = 1.sp
                                )
                            }
                            Text(
                                text = alliance.name,
                                style = MythosTypography.HeroName.copy(fontSize = 20.sp),
                                color = Color.White
                            )
                            Text(
                                text = "${alliance.memberCount} / ${Alliance.MAX_MEMBERS} Champions",
                                style = MythosTypography.CardDescription.copy(fontSize = 12.sp),
                                color = MythosTokens.SecondaryGold
                            )
                        }
                    }

                    // XP Progress bar
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "ALLIANCE XP",
                                style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                                color = MythosTokens.PrimaryGold
                            )
                            Text(
                                text = if (alliance.level >= AllianceProgressionConfig.MAX_ALLIANCE_LEVEL) "MAX LEVEL"
                                else "${numberFormat.format(alliance.xp)} / ${numberFormat.format(alliance.xpRequiredForNextLevel)} XP",
                                style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                                color = Color(0xFF38BDF8)
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
                                    .fillMaxWidth(alliance.xpProgressFraction)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF0284C7), MythosTokens.PrimaryGold)
                                        )
                                    )
                            )
                        }
                    }

                    // Perk description
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MythosTokens.PanelElevated)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "⚡ " + AllianceProgressionConfig.getPerkDescription(alliance.level),
                            style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                            color = MythosTokens.SecondaryGold
                        )
                    }
                }
            }
        }

        // Quick Hub Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenMembers)
                        .border(1.dp, MythosTokens.PanelBorder, RoundedCornerShape(12.dp))
                        .testTag("open_alliance_members_button"),
                    colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = MythosTokens.PrimaryGold,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "ROSTER",
                            style = MythosTypography.HeroName.copy(fontSize = 14.sp),
                            color = Color.White
                        )
                        Text(
                            text = "${alliance.memberCount} Members",
                            style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                            color = MythosTokens.TextMuted
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenDetail)
                        .border(1.dp, MythosTokens.PanelBorder, RoundedCornerShape(12.dp))
                        .testTag("open_alliance_details_button"),
                    colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Leaderboard,
                            contentDescription = null,
                            tint = MythosTokens.DivineBlueLight,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "DETAILS",
                            style = MythosTypography.HeroName.copy(fontSize = 14.sp),
                            color = Color.White
                        )
                        Text(
                            text = "Rank & Stats",
                            style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                            color = MythosTokens.TextMuted
                        )
                    }
                }
            }
        }

        // Top Contributors Preview
        item {
            Text(
                text = "TOP ALLIANCE CONTRIBUTORS",
                style = MythosTypography.RarityLabel.copy(fontSize = 12.sp),
                color = MythosTokens.PrimaryGold
            )
        }

        items(alliance.sortedByContribution.take(4)) { member ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(0.5.dp, MythosTokens.PanelBorder, RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel)
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MythosTokens.PanelElevated)
                                .border(1.dp, MythosTokens.PrimaryGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = member.name.take(1).uppercase(),
                                style = MythosTypography.HeroName.copy(fontSize = 14.sp),
                                color = MythosTokens.PrimaryGold
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = member.name,
                                    style = MythosTypography.CardName.copy(fontSize = 13.sp),
                                    color = Color.White
                                )
                                Text(
                                    text = "[${member.role.title.uppercase()}]",
                                    style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                    color = when (member.role) {
                                        com.example.data.AllianceRole.LEADER -> MythosTokens.PrimaryGold
                                        com.example.data.AllianceRole.OFFICER -> MythosTokens.DivineBlueLight
                                        com.example.data.AllianceRole.MEMBER -> MythosTokens.TextMuted
                                    }
                                )
                            }
                            Text(
                                text = "Lvl ${member.playerLevel} • ${member.battlesWon} Victories",
                                style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                                color = MythosTokens.TextMuted
                            )
                        }
                    }

                    Text(
                        text = "${numberFormat.format(member.contributionScore)} pts",
                        style = MythosTypography.HeroName.copy(fontSize = 12.sp),
                        color = MythosTokens.SecondaryGold
                    )
                }
            }
        }

        // Leave Alliance Button
        item {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onRequestLeave,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("leave_alliance_button"),
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

@Composable
private fun AllianceLobbyView(
    alliances: List<Alliance>,
    playerGold: Int,
    numberFormat: NumberFormat,
    onOpenCreate: () -> Unit,
    onOpenJoin: () -> Unit,
    onQuickJoin: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MythosTokens.PrimaryGold, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "🏛️",
                        fontSize = 36.sp
                    )
                    Text(
                        text = "UNITE WITH FELLOW CHAMPIONS",
                        style = MythosTypography.HeroName.copy(fontSize = 18.sp),
                        color = MythosTokens.PrimaryGold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Join or establish an Alliance to earn battle gold bonuses, unlock mythic perks, and conquer upcoming World Raids together.",
                        style = MythosTypography.CardDescription.copy(fontSize = 12.sp),
                        color = Color(0xFFD4CCE6),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Action Buttons Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MythosButton(
                    text = "CREATE AN ALLIANCE",
                    subtitle = "5,000 Gold • Lead your own House",
                    onClick = onOpenCreate,
                    style = MythosButtonStyle.PRIMARY,
                    modifier = Modifier.testTag("hub_create_alliance_button")
                )

                MythosButton(
                    text = "BROWSE & SEARCH ALLIANCES",
                    subtitle = "Find an established Alliance to join",
                    onClick = onOpenJoin,
                    style = MythosButtonStyle.SECONDARY,
                    modifier = Modifier.testTag("hub_browse_alliances_button")
                )
            }
        }

        // Open Alliances Quick List
        item {
            Text(
                text = "OPEN ALLIANCES READY FOR RECRUITS",
                style = MythosTypography.RarityLabel.copy(fontSize = 12.sp),
                color = MythosTokens.PrimaryGold
            )
        }

        items(alliances.take(3)) { alliance ->
            val emblem = remember(alliance.emblem) { AllianceEmblems.find(alliance.emblem) }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(0.5.dp, MythosTokens.PanelBorder, RoundedCornerShape(12.dp))
                    .testTag("quick_alliance_card_${alliance.allianceId}"),
                colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(emblem.colorHex).copy(alpha = 0.2f))
                                    .border(1.dp, Color(emblem.colorHex), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emblem.iconEmoji, fontSize = 20.sp)
                            }

                            Column {
                                Text(
                                    text = alliance.name,
                                    style = MythosTypography.HeroName.copy(fontSize = 15.sp),
                                    color = Color.White
                                )
                                Text(
                                    text = "Lvl ${alliance.level} • ${alliance.memberCount}/${Alliance.MAX_MEMBERS} Members",
                                    style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                                    color = MythosTokens.SecondaryGold
                                )
                            }
                        }

                        Button(
                            onClick = { onQuickJoin(alliance.allianceId) },
                            enabled = !alliance.isFull,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MythosTokens.PrimaryGold,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("quick_join_btn_${alliance.allianceId}")
                        ) {
                            Text(
                                text = if (alliance.isFull) "FULL" else "JOIN",
                                style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = alliance.description,
                        style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                        color = Color(0xFFC0B8D0),
                        maxLines = 2
                    )
                }
            }
        }
    }
}
