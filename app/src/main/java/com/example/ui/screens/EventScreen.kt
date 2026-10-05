package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun EventScreen(
    onNavigateBack: () -> Unit,
    onParticipateEvent: (MythosEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val economyState by rememberDisplayEconomyState()
    val events = remember(economyState.activeEvents) {
        if (economyState.activeEvents.isNotEmpty()) economyState.activeEvents else EventCatalog.getDefaultEvents()
    }
    var currentMainTab by remember { mutableStateOf("EVENTS") } // "EVENTS" or "SHOP"
    var selectedFilter by remember { mutableStateOf("ALL") }
    var shopFeedbackMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val isOnline = MythosBackend.isOnline
    LaunchedEffect(Unit) { if (isOnline) MythosBackend.online.refreshAll() }
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

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
                            text = if (currentMainTab == "EVENTS") "LIMITED-TIME EVENTS" else "EVENT REWARD SHOP",
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
                    actions = {
                        // Event Tokens Balance Display (Phase 10 Section 11)
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
                                text = "${numberFormat.format(economyState.eventTokens)} TOKENS",
                                style = MythosTypography.RarityLabel.copy(fontSize = 11.sp),
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OnlineStatusBanner(onRetry = { coroutineScope.launch { MythosBackend.online.refreshAll() } })

                // Main Switcher: Events vs Reward Shop
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { currentMainTab = "EVENTS" },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("tab_events_list"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentMainTab == "EVENTS") MythosTokens.PrimaryGold else MythosTokens.PanelElevated,
                            contentColor = if (currentMainTab == "EVENTS") Color.Black else MythosTokens.TextMuted
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "LIVE EVENTS",
                            style = MythosTypography.RarityLabel,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { currentMainTab = "SHOP" },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("tab_event_reward_shop"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentMainTab == "SHOP") MythosTokens.PrimaryGold else MythosTokens.PanelElevated,
                            contentColor = if (currentMainTab == "SHOP") Color.Black else MythosTokens.TextMuted
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "REWARD SHOP",
                            style = MythosTypography.RarityLabel,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (currentMainTab == "EVENTS") {
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
                                    .height(34.dp)
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
                                    style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
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
                } else {
                    // Event Reward Shop (Phase 10 Section 12)
                    shopFeedbackMessage?.let { msg: String ->
                        Text(
                            text = msg,
                            style = MythosTypography.CardDescription,
                            color = MythosTokens.PrimaryGold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "EXCHANGE EVENT TOKENS FOR EXCLUSIVE LOOT",
                                style = MythosTypography.RarityLabel,
                                color = MythosTokens.PrimaryGold
                            )
                        }

                        items(EndgameCatalog.DEFAULT_EVENT_SHOP_ITEMS) { item ->
                            val purchased = economyState.eventShopPurchases[item.itemId] ?: 0
                            val canAfford = economyState.eventTokens >= item.tokenPrice
                            val isSoldOut = purchased >= item.purchaseLimit

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, MythosTokens.PanelBorder, RoundedCornerShape(12.dp))
                                    .testTag("event_shop_item_${item.itemId}"),
                                colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name.uppercase(),
                                            style = MythosTypography.CardName.copy(fontSize = 13.sp),
                                            color = if (isSoldOut) MythosTokens.TextMuted else MythosTokens.PrimaryGold
                                        )
                                        Text(
                                            text = item.description,
                                            style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                                            color = MythosTokens.TextMuted
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Cost: ${item.tokenPrice} Tokens • Stock: $purchased/${item.purchaseLimit}",
                                            style = MythosTypography.HeroTitle.copy(fontSize = 11.sp),
                                            color = if (canAfford) MythosTokens.DivineBlueLight else MythosTokens.Error
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Button(
                                        onClick = {
                                            if (isOnline) {
                                                // The server holds the token wallet, checks the event window and the
                                                // per-item purchase limit, and rejects duplicate claims.
                                                val eventId = MythosBackend.online.snapshot.value.endgame?.activeEventIds?.firstOrNull()
                                                if (eventId == null) {
                                                    shopFeedbackMessage = "No event is currently active."
                                                } else {
                                                    coroutineScope.launch {
                                                        MythosBackend.online.claimEventReward(eventId, item.itemId).fold(
                                                            onSuccess = { shopFeedbackMessage = "Successfully claimed ${item.name}!" },
                                                            onFailure = { e ->
                                                                shopFeedbackMessage = (e as? BackendException)?.let(OnlineBackend::userMessage) ?: e.message
                                                            }
                                                        )
                                                    }
                                                }
                                            } else {
                                                val result = PlayerEconomyRepository.instance.purchaseEventShopItem(item.itemId)
                                                shopFeedbackMessage = if (result.isSuccess) {
                                                    "Successfully claimed ${item.name}!"
                                                } else {
                                                    result.exceptionOrNull()?.message
                                                }
                                            }
                                        },
                                        enabled = canAfford && !isSoldOut,
                                        modifier = Modifier.testTag("buy_event_item_${item.itemId}"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MythosTokens.PrimaryGold,
                                            disabledContainerColor = MythosTokens.PanelBorder
                                        ),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (isSoldOut) "MAX" else "CLAIM",
                                            style = MythosTypography.RarityLabel.copy(fontSize = 11.sp),
                                            color = if (canAfford && !isSoldOut) Color.Black else MythosTokens.TextMuted
                                        )
                                    }
                                }
                            }
                        }
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
                            .background(if (isActive) MythosTokens.Success.copy(alpha = 0.9f) else MythosTokens.PanelBorder)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isActive) "● LIVE NOW" else "○ UPCOMING",
                            style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MythosTokens.PanelElevated)
                            .border(1.dp, MythosTokens.PanelBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = event.eventType.displayTag,
                            style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                            color = MythosTokens.DivineBlueLight
                        )
                    }
                }
            }

            // Event Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = event.title,
                    style = MythosTypography.HeroName.copy(fontSize = 18.sp),
                    color = MythosTokens.PrimaryGold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = event.description,
                    style = MythosTypography.CardDescription,
                    color = MythosTokens.TextMuted
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Date & Requirements
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📅 ", fontSize = 12.sp)
                        Text(
                            text = "${event.startDate} — ${event.endDate}",
                            style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                            color = MythosTokens.DivineBlueLight
                        )
                    }
                    Text(
                        text = event.participationRequirement,
                        style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                        color = MythosTokens.TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Rewards pill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MythosTokens.PanelElevated)
                        .border(0.5.dp, MythosTokens.PrimaryGold.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🎁 ", fontSize = 12.sp)
                        Text(
                            text = "Rewards: ${event.rewardSummary}",
                            style = MythosTypography.CardName.copy(fontSize = 11.sp),
                            color = MythosTokens.PrimaryGold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Participation CTA Button
                Button(
                    onClick = onParticipate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("event_participate_${event.eventId}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) MythosTokens.PrimaryGold else MythosTokens.PanelElevated,
                        contentColor = if (isActive) Color.Black else MythosTokens.TextMuted
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isActive) Icons.Default.PlayArrow else Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isActive) "ENTER EVENT STAGES" else "LOCKED (STARTS ${event.startDate})",
                            style = MythosTypography.RarityLabel.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
