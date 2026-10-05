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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.Alliance
import com.example.data.AllianceMember
import com.example.data.AllianceRole
import com.example.backend.online.AllianceGateway
import com.example.ui.components.rememberDisplayEconomyState
import kotlinx.coroutines.launch
import com.example.monetization.PlayerEconomyRepository
import com.example.ui.theme.MythosTokens
import com.example.ui.theme.MythosTypography
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllianceMembersScreen(
    onNavigateBack: () -> Unit,
    onAllianceLeft: () -> Unit,
    modifier: Modifier = Modifier
) {
    val economyState by rememberDisplayEconomyState()
    val coroutineScope = rememberCoroutineScope()
    val alliance = economyState.playerAlliance
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    var actionTargetMember by remember { mutableStateOf<AllianceMember?>(null) }
    var showTransferConfirmDialog by remember { mutableStateOf(false) }
    var showKickConfirmDialog by remember { mutableStateOf(false) }
    var actionErrorMessage by remember { mutableStateOf<String?>(null) }

    if (alliance == null) {
        LaunchedEffect(Unit) {
            onNavigateBack()
        }
        return
    }

    val callerRole = economyState.playerMemberRecord?.role ?: AllianceRole.MEMBER
    val isLeader = callerRole == AllianceRole.LEADER
    val isOfficer = callerRole == AllianceRole.OFFICER

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
                            text = "MEMBER ROSTER (${alliance.memberCount}/${Alliance.MAX_MEMBERS})",
                            style = MythosTypography.GameTitle.copy(fontSize = 17.sp),
                            color = MythosTokens.PrimaryGold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("alliance_members_back_button")
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // Role permissions info banner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MythosTokens.PanelElevated),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = MythosTokens.PrimaryGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = when (callerRole) {
                                    AllianceRole.LEADER -> "You are the Alliance Leader. You can promote officers, demote, transfer leadership, and dismiss members."
                                    AllianceRole.OFFICER -> "You are an Alliance Officer. You can recruit and dismiss regular members."
                                    AllianceRole.MEMBER -> "You are an Alliance Member. Contribute in battles to raise your alliance standing."
                                },
                                style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                                color = Color(0xFFD4CCE6)
                            )
                        }
                    }
                }

                // Member items
                items(alliance.members) { member ->
                    val isSelf = member.playerId == "player_local"
                    val canManageTarget = !isSelf && (
                        (isLeader) ||
                        (isOfficer && member.role == AllianceRole.MEMBER)
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                if (isSelf) 1.dp else 0.5.dp,
                                if (isSelf) MythosTokens.PrimaryGold else MythosTokens.PanelBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .testTag("member_card_${member.playerId}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelf) MythosTokens.PanelElevated else MythosTokens.Panel
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF261F33))
                                            .border(
                                                1.dp,
                                                when (member.role) {
                                                    AllianceRole.LEADER -> MythosTokens.PrimaryGold
                                                    AllianceRole.OFFICER -> MythosTokens.DivineBlueLight
                                                    AllianceRole.MEMBER -> MythosTokens.PanelBorder
                                                },
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = member.name.take(1).uppercase(),
                                            style = MythosTypography.HeroName.copy(fontSize = 16.sp),
                                            color = when (member.role) {
                                                AllianceRole.LEADER -> MythosTokens.PrimaryGold
                                                AllianceRole.OFFICER -> MythosTokens.DivineBlueLight
                                                AllianceRole.MEMBER -> Color.White
                                            }
                                        )
                                    }

                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = if (isSelf) "${member.name} (You)" else member.name,
                                                style = MythosTypography.CardName.copy(fontSize = 14.sp),
                                                color = Color.White
                                            )
                                        }

                                        Text(
                                            text = "Level ${member.playerLevel} • ${numberFormat.format(member.combatPower)} Power",
                                            style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                                            color = MythosTokens.TextMuted
                                        )
                                    }
                                }

                                // Role Badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when (member.role) {
                                                AllianceRole.LEADER -> MythosTokens.PrimaryGold.copy(alpha = 0.2f)
                                                AllianceRole.OFFICER -> MythosTokens.DivineBlueLight.copy(alpha = 0.2f)
                                                AllianceRole.MEMBER -> Color(0xFF2A2338)
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = member.role.title.uppercase(),
                                        style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                        color = when (member.role) {
                                            AllianceRole.LEADER -> MythosTokens.PrimaryGold
                                            AllianceRole.OFFICER -> MythosTokens.DivineBlueLight
                                            AllianceRole.MEMBER -> MythosTokens.TextMuted
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Contribution stats row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1B1526))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "⚔️ ${member.battlesWon} Victories",
                                    style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                                    color = Color(0xFFD4CCE6)
                                )
                                Text(
                                    text = "⭐ ${numberFormat.format(member.contributionScore)} Contribution",
                                    style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                                    color = MythosTokens.SecondaryGold
                                )
                            }

                            // Leader & Officer actions
                            if (canManageTarget) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isLeader) {
                                        if (member.role == AllianceRole.MEMBER) {
                                            Button(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        val res = AllianceGateway.promote(member.playerId)
                                                        if (res.isFailure) actionErrorMessage = res.exceptionOrNull()?.message
                                                    }
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(30.dp)
                                                    .testTag("promote_btn_${member.playerId}"),
                                                colors = ButtonDefaults.buttonColors(containerColor = MythosTokens.DivineBlue),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text("PROMOTE", style = MythosTypography.RarityLabel.copy(fontSize = 9.sp))
                                            }
                                        } else if (member.role == AllianceRole.OFFICER) {
                                            Button(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        val res = AllianceGateway.demote(member.playerId)
                                                        if (res.isFailure) actionErrorMessage = res.exceptionOrNull()?.message
                                                    }
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(30.dp)
                                                    .testTag("demote_btn_${member.playerId}"),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A3E66)),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text("DEMOTE", style = MythosTypography.RarityLabel.copy(fontSize = 9.sp))
                                            }
                                        }

                                        // Transfer leadership button
                                        Button(
                                            onClick = {
                                                actionTargetMember = member
                                                showTransferConfirmDialog = true
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(30.dp)
                                                .testTag("transfer_btn_${member.playerId}"),
                                            colors = ButtonDefaults.buttonColors(containerColor = MythosTokens.PrimaryGold),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(
                                                "MAKE LEADER",
                                                style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                                color = Color.Black,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Kick button
                                    Button(
                                        onClick = {
                                            actionTargetMember = member
                                            showKickConfirmDialog = true
                                        },
                                        modifier = Modifier
                                            .weight(0.8f)
                                            .height(30.dp)
                                            .testTag("kick_btn_${member.playerId}"),
                                        colors = ButtonDefaults.buttonColors(containerColor = MythosTokens.Damage),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("DISMISS", style = MythosTypography.RarityLabel.copy(fontSize = 9.sp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Transfer Leadership Confirm Dialog
        if (showTransferConfirmDialog && actionTargetMember != null) {
            val target = actionTargetMember!!
            AlertDialog(
                onDismissRequest = { showTransferConfirmDialog = false },
                title = { Text("Transfer Leadership?", color = MythosTokens.PrimaryGold) },
                text = {
                    Text(
                        "Are you sure you want to transfer Alliance Leadership to ${target.name}? You will step down to an Officer role.",
                        color = Color.White
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showTransferConfirmDialog = false
                            coroutineScope.launch {
                                val res = AllianceGateway.transferLeadership(target.playerId)
                                if (res.isFailure) actionErrorMessage = res.exceptionOrNull()?.message
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MythosTokens.PrimaryGold),
                        modifier = Modifier.testTag("confirm_transfer_leadership_button")
                    ) {
                        Text("TRANSFER", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTransferConfirmDialog = false }) {
                        Text("CANCEL", color = MythosTokens.TextMuted)
                    }
                },
                containerColor = MythosTokens.Panel
            )
        }

        // Kick Confirm Dialog
        if (showKickConfirmDialog && actionTargetMember != null) {
            val target = actionTargetMember!!
            AlertDialog(
                onDismissRequest = { showKickConfirmDialog = false },
                title = { Text("Dismiss Member?", color = MythosTokens.Damage) },
                text = {
                    Text("Are you sure you want to dismiss ${target.name} from ${alliance.name}?", color = Color.White)
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showKickConfirmDialog = false
                            coroutineScope.launch {
                                val res = AllianceGateway.kick(target.playerId)
                                if (res.isFailure) actionErrorMessage = res.exceptionOrNull()?.message
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MythosTokens.Damage),
                        modifier = Modifier.testTag("confirm_kick_member_button")
                    ) {
                        Text("DISMISS")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showKickConfirmDialog = false }) {
                        Text("CANCEL", color = MythosTokens.TextMuted)
                    }
                },
                containerColor = MythosTokens.Panel
            )
        }

        actionErrorMessage?.let { msg ->
            AlertDialog(
                onDismissRequest = { actionErrorMessage = null },
                title = { Text("Notice", color = MythosTokens.Damage) },
                text = { Text(msg, color = Color.White) },
                confirmButton = {
                    Button(onClick = { actionErrorMessage = null }) { Text("OK") }
                },
                containerColor = MythosTokens.Panel
            )
        }
    }
}
