package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Search
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
import com.example.data.AllianceEmblems
import com.example.backend.online.AllianceGateway
import com.example.ui.components.rememberDisplayEconomyState
import kotlinx.coroutines.launch
import com.example.backend.MythosBackend
import com.example.monetization.PlayerEconomyRepository
import com.example.ui.theme.MythosTokens
import com.example.ui.theme.MythosTypography
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinAllianceScreen(
    onNavigateBack: () -> Unit,
    onAllianceJoined: () -> Unit,
    modifier: Modifier = Modifier
) {
    val economyState by rememberDisplayEconomyState()
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) { if (MythosBackend.isOnline) MythosBackend.online.refreshAlliance() }
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }
    var searchQuery by remember { mutableStateOf("") }
    var actionErrorMessage by remember { mutableStateOf<String?>(null) }

    val filteredAlliances = remember(economyState.alliances, searchQuery) {
        val query = searchQuery.trim().lowercase()
        if (query.isEmpty()) {
            economyState.alliances
        } else {
            economyState.alliances.filter { it.name.lowercase().contains(query) || it.description.lowercase().contains(query) }
        }
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
                        Text(
                            text = "JOIN AN ALLIANCE",
                            style = MythosTypography.GameTitle.copy(fontSize = 18.sp),
                            color = MythosTokens.PrimaryGold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("join_alliance_back_button")
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name or keyword...", color = MythosTokens.TextMuted) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MythosTokens.PrimaryGold)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_alliance_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MythosTokens.PrimaryGold,
                        unfocusedBorderColor = MythosTokens.PanelBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Text(
                    text = "AVAILABLE ALLIANCES (${filteredAlliances.size})",
                    style = MythosTypography.RarityLabel.copy(fontSize = 11.sp),
                    color = MythosTokens.PrimaryGold
                )

                if (filteredAlliances.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No alliances found matching \"$searchQuery\"",
                            style = MythosTypography.CardDescription,
                            color = MythosTokens.TextMuted
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredAlliances) { alliance ->
                            val emblem = remember(alliance.emblem) { AllianceEmblems.find(alliance.emblem) }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(0.5.dp, MythosTokens.PanelBorder, RoundedCornerShape(12.dp))
                                    .testTag("alliance_row_${alliance.allianceId}"),
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
                                                    .size(42.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(emblem.colorHex).copy(alpha = 0.2f))
                                                    .border(1.dp, Color(emblem.colorHex), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = emblem.iconEmoji, fontSize = 22.sp)
                                            }

                                            Column {
                                                Text(
                                                    text = alliance.name,
                                                    style = MythosTypography.HeroName.copy(fontSize = 15.sp),
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "Lvl ${alliance.level} • ${alliance.memberCount}/${Alliance.MAX_MEMBERS} Members • ${numberFormat.format(alliance.totalPower)} Power",
                                                    style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                                                    color = MythosTokens.SecondaryGold
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    val result = AllianceGateway.join(alliance.allianceId)
                                                    if (result.isSuccess) {
                                                        onAllianceJoined()
                                                    } else {
                                                        actionErrorMessage = result.exceptionOrNull()?.message ?: "Failed to join alliance"
                                                    }
                                                }
                                            },
                                            enabled = !alliance.isFull,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MythosTokens.PrimaryGold,
                                                contentColor = Color.Black
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.testTag("join_alliance_btn_${alliance.allianceId}")
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
            }
        }

        actionErrorMessage?.let { msg ->
            AlertDialog(
                onDismissRequest = { actionErrorMessage = null },
                title = { Text("Alliance Notice", color = MythosTokens.PrimaryGold) },
                text = { Text(msg, color = Color.White) },
                confirmButton = {
                    Button(onClick = { actionErrorMessage = null }) { Text("OK") }
                },
                containerColor = MythosTokens.Panel
            )
        }
    }
}
