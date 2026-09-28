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
import androidx.compose.material.icons.filled.Check
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
import com.example.monetization.PlayerEconomyRepository
import com.example.ui.components.MythosButton
import com.example.ui.components.MythosButtonStyle
import com.example.ui.theme.MythosTokens
import com.example.ui.theme.MythosTypography
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAllianceScreen(
    onNavigateBack: () -> Unit,
    onAllianceCreated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val economyState by PlayerEconomyRepository.instance.economyState.collectAsState()
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    var allianceName by remember { mutableStateOf("") }
    var allianceDescription by remember { mutableStateOf("") }
    var selectedEmblemId by remember { mutableStateOf(AllianceEmblems.ALL_EMBLEMS[0].id) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val hasEnoughGold = economyState.gold >= Alliance.CREATE_GOLD_COST
    val isValidName = allianceName.trim().length in 3..24

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
                            text = "ESTABLISH ALLIANCE",
                            style = MythosTypography.GameTitle.copy(fontSize = 18.sp),
                            color = MythosTokens.PrimaryGold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("create_alliance_back_button")
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
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header instruction card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, MythosTokens.PrimaryGold.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "FOUND A HOUSE OF GODS & HEROES",
                            style = MythosTypography.HeroName.copy(fontSize = 15.sp),
                            color = MythosTokens.PrimaryGold
                        )
                        Text(
                            text = "Establish a sacred hall where champions gather. Up to 20 champions can rally under your divine crest.",
                            style = MythosTypography.CardDescription.copy(fontSize = 12.sp),
                            color = Color(0xFFD4CCE6)
                        )
                    }
                }

                // Alliance Name Input
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "ALLIANCE NAME",
                            style = MythosTypography.RarityLabel.copy(fontSize = 11.sp),
                            color = MythosTokens.PrimaryGold
                        )
                        OutlinedTextField(
                            value = allianceName,
                            onValueChange = { if (it.length <= 24) allianceName = it },
                            placeholder = { Text("e.g. Spartan Vanguard", color = MythosTokens.TextMuted) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_alliance_name_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MythosTokens.PrimaryGold,
                                unfocusedBorderColor = MythosTokens.PanelBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Text(
                            text = "${allianceName.trim().length} / 24 characters (minimum 3)",
                            style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                            color = if (allianceName.isNotBlank() && !isValidName) MythosTokens.Damage else MythosTokens.TextMuted
                        )
                    }
                }

                // Motto / Description Input
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "ALLIANCE MOTTO & MOTIVATION",
                            style = MythosTypography.RarityLabel.copy(fontSize = 11.sp),
                            color = MythosTokens.PrimaryGold
                        )
                        OutlinedTextField(
                            value = allianceDescription,
                            onValueChange = { if (it.length <= 120) allianceDescription = it },
                            placeholder = { Text("e.g. Glory in Tartarus, Honor on Olympus!", color = MythosTokens.TextMuted) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_alliance_desc_input"),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MythosTokens.PrimaryGold,
                                unfocusedBorderColor = MythosTokens.PanelBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Text(
                            text = "${allianceDescription.length} / 120 characters",
                            style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                            color = MythosTokens.TextMuted
                        )
                    }
                }

                // Emblem Selection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "SELECT ALLIANCE EMBLEM",
                            style = MythosTypography.RarityLabel.copy(fontSize = 11.sp),
                            color = MythosTokens.PrimaryGold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AllianceEmblems.ALL_EMBLEMS.forEach { emblemItem ->
                                val isSelected = emblemItem.id == selectedEmblemId
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) Color(emblemItem.colorHex).copy(alpha = 0.35f)
                                            else MythosTokens.PanelElevated
                                        )
                                        .border(
                                            if (isSelected) 2.dp else 1.dp,
                                            if (isSelected) MythosTokens.PrimaryGold else MythosTokens.PanelBorder,
                                            CircleShape
                                        )
                                        .clickable { selectedEmblemId = emblemItem.id }
                                        .testTag("emblem_choice_${emblemItem.id}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emblemItem.iconEmoji, fontSize = 22.sp)
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .border(2.dp, MythosTokens.PrimaryGold, CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Cost & Balance Indicator
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(0.5.dp, MythosTokens.PanelBorder, RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = MythosTokens.PanelElevated)
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
                                text = "ESTABLISHMENT COST",
                                style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                                color = MythosTokens.TextMuted
                            )
                            Text(
                                text = "🪙 ${numberFormat.format(Alliance.CREATE_GOLD_COST)} Gold",
                                style = MythosTypography.HeroName.copy(fontSize = 15.sp),
                                color = MythosTokens.PrimaryGold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "YOUR TREASURY",
                                style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                                color = MythosTokens.TextMuted
                            )
                            Text(
                                text = "🪙 ${numberFormat.format(economyState.gold)} Gold",
                                style = MythosTypography.HeroName.copy(fontSize = 15.sp),
                                color = if (hasEnoughGold) MythosTokens.Success else MythosTokens.Damage
                            )
                        }
                    }
                }

                // Submit Button
                MythosButton(
                    text = "FOUND ALLIANCE",
                    subtitle = if (!hasEnoughGold) "Insufficient Gold" else "Become Founding Leader",
                    onClick = {
                        val result = PlayerEconomyRepository.instance.createAlliance(
                            name = allianceName,
                            emblem = selectedEmblemId,
                            description = allianceDescription
                        )
                        if (result.isSuccess) {
                            onAllianceCreated()
                        } else {
                            errorMessage = result.exceptionOrNull()?.message ?: "Failed to create alliance"
                        }
                    },
                    style = MythosButtonStyle.PRIMARY,
                    enabled = isValidName && hasEnoughGold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("submit_create_alliance_button")
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        errorMessage?.let { msg ->
            AlertDialog(
                onDismissRequest = { errorMessage = null },
                title = { Text("Creation Failed", color = MythosTokens.Damage) },
                text = { Text(msg, color = Color.White) },
                confirmButton = {
                    Button(onClick = { errorMessage = null }) { Text("OK") }
                },
                containerColor = MythosTokens.Panel
            )
        }
    }
}
