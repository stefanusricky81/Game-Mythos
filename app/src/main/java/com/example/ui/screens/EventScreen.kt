package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.EventCatalog
import com.example.data.EventType
import com.example.data.MythosDateUtil
import com.example.data.MythosEvent
import com.example.monetization.PlayerEconomyRepository
import com.example.ui.theme.MythosTokens
import com.example.ui.theme.MythosTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventScreen(
    onNavigateBack: () -> Unit,
    onParticipateEvent: (MythosEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val economyState by PlayerEconomyRepository.instance.economyState.collectAsState()
    val events = remember(economyState.activeEvents) {
        if (economyState.activeEvents.isNotEmpty()) economyState.activeEvents else EventCatalog.getDefaultEvents()
    }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredEvents = remember(events, selectedFilter) {
        when (selectedFilter) {
            "ACTIVE" -> events.filter { it.isCurrentlyActive }
            "UPCOMING" -> events.filter { !it.isCurrentlyActive }
            else -> events
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
                            text = "LIMITED-TIME EVENTS",
                            style = MythosTypography.GameTitle.copy(fontSize = 18.sp),
                            color = MythosTokens.PrimaryGold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("events_back_button")
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
                // Filter Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALL", "ACTIVE", "UPCOMING").forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Button(
                            onClick = { selectedFilter = filter },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("event_filter_${filter.lowercase()}"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MythosTokens.PrimaryGold else MythosTokens.PanelElevated,
                                contentColor = if (isSelected) Color.Black else MythosTokens.TextMuted
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = filter,
                                style = MythosTypography.RarityLabel.copy(fontSize = 11.sp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Event List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredEvents) { event ->
                        EventCard(
                            event = event,
                            onParticipate = { onParticipateEvent(event) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventCard(
    event: MythosEvent,
    onParticipate: () -> Unit
) {
    val isActive = event.isCurrentlyActive

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                if (isActive) 1.dp else 0.5.dp,
                if (isActive) MythosTokens.PrimaryGold else MythosTokens.PanelBorder,
                RoundedCornerShape(14.dp)
            )
            .testTag("event_card_${event.eventId}"),
        colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Event Banner Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                Image(
                    painter = painterResource(id = event.bannerResId),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Dark gradient scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xCC130F1A), Color(0xFF130F1A))
                            )
                        )
                )

                // Status pill & Type tag
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (event.eventType) {
                                    EventType.RAID_BOSS -> MythosTokens.Damage.copy(alpha = 0.85f)
                                    EventType.DOUBLE_GOLD -> MythosTokens.PrimaryGold.copy(alpha = 0.85f)
                                    EventType.OLYMPUS_TRIAL -> MythosTokens.DivineBlue.copy(alpha = 0.85f)
                                    EventType.SEASON_EXPEDITION -> MythosTokens.Legendary.copy(alpha = 0.85f)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = event.eventType.displayTag,
                            style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                            color = if (event.eventType == EventType.DOUBLE_GOLD) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isActive) MythosTokens.Success.copy(alpha = 0.25f)
                                else Color(0xFF261F33)
                            )
                            .border(
                                1.dp,
                                if (isActive) MythosTokens.Success else MythosTokens.PanelBorder,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isActive) "● LIVE NOW" else "UPCOMING",
                            style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                            color = if (isActive) MythosTokens.Success else MythosTokens.TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Event Details Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = event.title,
                    style = MythosTypography.HeroName.copy(fontSize = 17.sp),
                    color = Color.White
                )

                Text(
                    text = event.description,
                    style = MythosTypography.CardDescription.copy(fontSize = 12.sp),
                    color = Color(0xFFD4CCE6)
                )

                // Date & Requirements
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "📅 ${event.startDate} to ${event.endDate}",
                        style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                        color = MythosTokens.SecondaryGold
                    )
                    Text(
                        text = "🎯 ${event.participationRequirement}",
                        style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                        color = MythosTokens.TextMuted
                    )
                }

                // Rewards Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MythosTokens.PanelElevated)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "🎁", fontSize = 16.sp)
                        Column {
                            Text(
                                text = "EVENT REWARDS",
                                style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                color = MythosTokens.PrimaryGold
                            )
                            Text(
                                text = event.rewardSummary,
                                style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                                color = Color.White
                            )
                        }
                    }
                }

                // CTA Button
                Button(
                    onClick = onParticipate,
                    enabled = isActive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("participate_event_${event.eventId}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MythosTokens.PrimaryGold,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isActive) "ENTER EVENT" else "STARTS SOON",
                        style = MythosTypography.RarityLabel.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
