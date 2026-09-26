package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.*
import com.example.monetization.PlayerEconomyRepository
import com.example.ui.components.CardFrame
import com.example.ui.theme.MythosTokens
import com.example.ui.theme.MythosTypography
import kotlinx.coroutines.delay

/**
 * Authoritative Deck Builder Screen (Requirements #6, #7, #8, #9, #10, #11, #16, #18).
 * Enforces exact 20-card rule, max 2 duplicates per card, ownership bounds, and local persistence.
 */
@Composable
fun DeckBuilderScreen(
    onNavigateBack: () -> Unit,
    onStartBattleWithDeck: () -> Unit = {},
    initialFocusCardId: String? = null,
    modifier: Modifier = Modifier
) {
    val economyState by PlayerEconomyRepository.instance.economyState.collectAsState()

    // Local working draft of active deck card IDs
    var workingCardIds by remember(economyState.activeDeck) {
        mutableStateOf(economyState.activeDeck.cardIds.toList().toMutableList())
    }

    // Always re-sync workingCardIds from persistent activeDeck on entry/deck change
    LaunchedEffect(economyState.activeDeck) {
        workingCardIds = economyState.activeDeck.cardIds.toList().toMutableList()
    }
    val selectedHeroId by remember(economyState.selectedHeroId) {
        mutableStateOf(economyState.selectedHeroId)
    }

    var selectedFilterType by remember { mutableStateOf<CardType?>(null) }
    var userFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var highlightedCardId by remember { mutableStateOf(initialFocusCardId) }

    // Grid state for scrolling to focused card
    val gridState = rememberLazyGridState()

    // Working deck snapshot and centralized validation (Requirement #5)
    val workingDeck = remember(workingCardIds, selectedHeroId) {
        ActiveDeck(
            deckId = economyState.activeDeck.deckId,
            name = economyState.activeDeck.name,
            heroId = selectedHeroId,
            cardIds = workingCardIds.toList(),
            updatedAt = System.currentTimeMillis()
        )
    }

    val validationResult = remember(workingDeck, economyState.ownedCardCounts) {
        DeckValidator.validate(workingDeck, economyState.ownedCardCounts)
    }

    val heroDef = remember(selectedHeroId) {
        HeroCatalog.findHero(selectedHeroId) ?: HeroCatalog.HERCULES
    }

    // Frequencies in working deck
    val deckFrequencies = remember(workingCardIds) {
        workingCardIds.groupingBy { it }.eachCount()
    }

    // Candidate cards from authoritative CardCatalog
    val candidateCards = remember(selectedFilterType, economyState.ownedCardIds) {
        CardCatalog.ALL_CARDS.filter { def ->
            selectedFilterType == null || def.type == selectedFilterType
        }
    }

    // Auto-scroll to focused card if specified (Requirement #13)
    LaunchedEffect(initialFocusCardId) {
        if (initialFocusCardId != null) {
            val index = candidateCards.indexOfFirst { it.id == initialFocusCardId }
            if (index >= 0) {
                gridState.animateScrollToItem(index)
            }
        }
    }

    // Clear feedback message after delay
    LaunchedEffect(userFeedbackMessage) {
        if (userFeedbackMessage != null) {
            delay(3500)
            userFeedbackMessage = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MythosTokens.Background)
    ) {
        // Subtle battlefield background
        Image(
            painter = painterResource(id = R.drawable.img_battlefield_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.12f
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // ==========================================
            // 1. TOP APP BAR & HEADER (Requirement #6)
            // ==========================================
            Surface(
                color = MythosTokens.Panel,
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.testTag("deck_builder_back_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MythosTokens.PrimaryGold
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "DECK BUILDER",
                                        style = MythosTypography.GameTitle.copy(fontSize = 17.sp),
                                        color = MythosTokens.PrimaryGold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // VALID / INVALID Badge (Requirement #6)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (validationResult.isValid) MythosTokens.HealthGreen.copy(alpha = 0.2f)
                                                else MythosTokens.Damage.copy(alpha = 0.2f)
                                            )
                                            .border(
                                                0.8.dp,
                                                if (validationResult.isValid) MythosTokens.HealthGreen
                                                else MythosTokens.Damage,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (validationResult.isValid) "VALID" else "INVALID",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (validationResult.isValid) MythosTokens.HealthGreen else MythosTokens.Damage,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }

                                Text(
                                    text = "${heroDef.name} • ${workingCardIds.size} / 20 CARDS",
                                    style = MythosTypography.HeroTitle.copy(fontSize = 11.sp),
                                    color = if (workingCardIds.size == 20) MythosTokens.PrimaryGold else MythosTokens.TextMuted
                                )
                            }
                        }

                        // Action Buttons: RESET & SAVE DECK (Requirement #11)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    workingCardIds = HeroCatalog.HERCULES.defaultDeckCardIds.toList().toMutableList()
                                    userFeedbackMessage = "Reset to default Olympus 20-card deck."
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.8.dp, MythosTokens.PanelBorder),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("RESET", fontSize = 10.sp, color = MythosTokens.TextMuted)
                            }

                            Button(
                                onClick = {
                                    val result = PlayerEconomyRepository.instance.saveActiveDeck(workingDeck.createDefensiveCopy())
                                    if (result.isValid) {
                                        userFeedbackMessage = "Deck saved successfully!"
                                        onNavigateBack()
                                    } else {
                                        userFeedbackMessage = result.primaryErrorMessage ?: "Cannot save: Deck is invalid."
                                    }
                                },
                                enabled = validationResult.isValid && workingCardIds.size == ActiveDeck.REQUIRED_DECK_SIZE,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MythosTokens.PrimaryGold,
                                    contentColor = Color(0xFF161202),
                                    disabledContainerColor = MythosTokens.PanelBorder,
                                    disabledContentColor = MythosTokens.TextMuted
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("deck_builder_save_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "SAVE DECK",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = {
                                    val result = PlayerEconomyRepository.instance.saveActiveDeck(workingDeck.createDefensiveCopy())
                                    if (result.isValid) {
                                        userFeedbackMessage = "Deck saved! Commencing battle..."
                                        onStartBattleWithDeck()
                                    } else {
                                        userFeedbackMessage = result.primaryErrorMessage ?: "Cannot save: Deck is invalid."
                                    }
                                },
                                enabled = validationResult.isValid && workingCardIds.size == ActiveDeck.REQUIRED_DECK_SIZE,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF22C55E),
                                    contentColor = Color.Black,
                                    disabledContainerColor = MythosTokens.PanelBorder,
                                    disabledContentColor = MythosTokens.TextMuted
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("deck_builder_battle_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "BATTLE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Validation & Status Feedback Banner (Requirement #6, #10)
                    val bannerMessage = userFeedbackMessage ?: if (!validationResult.isValid) {
                        validationResult.primaryErrorMessage
                    } else null

                    AnimatedVisibility(visible = bannerMessage != null) {
                        Surface(
                            color = if (validationResult.isValid && userFeedbackMessage != null) {
                                MythosTokens.PanelElevated
                            } else {
                                MythosTokens.Damage.copy(alpha = 0.22f)
                            },
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(
                                0.8.dp,
                                if (validationResult.isValid && userFeedbackMessage != null) MythosTokens.PrimaryGold
                                else MythosTokens.Damage
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (validationResult.isValid && userFeedbackMessage != null) Icons.Default.CheckCircle
                                    else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (validationResult.isValid && userFeedbackMessage != null) MythosTokens.PrimaryGold
                                    else MythosTokens.Damage,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = bannerMessage ?: "",
                                    style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                                    color = if (validationResult.isValid && userFeedbackMessage != null) MythosTokens.PrimaryGold
                                    else MythosTokens.Damage,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 2. CURRENT DECK TRAY (Requirement #7)
            // ==========================================
            Surface(
                color = MythosTokens.PanelElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "CURRENT DECK",
                                style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                                color = MythosTokens.PrimaryGold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${workingCardIds.size} / 20)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (workingCardIds.size == 20) MythosTokens.HealthGreen else MythosTokens.Damage
                            )
                            if (workingCardIds.size < 20) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• Need ${20 - workingCardIds.size} more",
                                    fontSize = 10.sp,
                                    color = MythosTokens.TextMuted
                                )
                            }
                        }

                        Text(
                            text = "Tap to remove copy",
                            fontSize = 9.5.sp,
                            color = MythosTokens.TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Progress bar toward 20 cards
                    LinearProgressIndicator(
                        progress = { (workingCardIds.size / 20f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = if (workingCardIds.size == 20) MythosTokens.PrimaryGold else MythosTokens.Damage,
                        trackColor = MythosTokens.PanelBorder
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Horizontal tray of unique cards in current working deck
                    val uniqueDeckCardIds = remember(workingCardIds) { workingCardIds.distinct() }

                    if (uniqueDeckCardIds.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Deck is empty. Tap cards below to add (up to 20).",
                                style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                                color = MythosTokens.TextMuted
                            )
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(uniqueDeckCardIds, key = { it }) { cardId ->
                                val count = deckFrequencies[cardId] ?: 0
                                val level = economyState.cardLevels[cardId] ?: 1
                                val card = CardCatalog.getCard(cardId, level)

                                DeckCardTrayItem(
                                    card = card,
                                    count = count,
                                    onRemove = {
                                        val mutable = workingCardIds.toMutableList()
                                        mutable.remove(cardId)
                                        workingCardIds = mutable
                                        userFeedbackMessage = null
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 3. AVAILABLE CARDS POOL (Requirement #8, #9)
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                // Type Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedFilterType == null,
                        onClick = { selectedFilterType = null },
                        label = { Text("All Types", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MythosTokens.PrimaryGold,
                            selectedLabelColor = Color(0xFF13101B),
                            containerColor = MythosTokens.Panel,
                            labelColor = MythosTokens.TextPrimary
                        ),
                        modifier = Modifier.height(28.dp)
                    )

                    CardType.entries.forEach { type ->
                        val isSel = selectedFilterType == type
                        val color = MythosTokens.getCardTypeColor(type)
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedFilterType = if (isSel) null else type },
                            label = { Text(type.label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.25f),
                                selectedLabelColor = color,
                                containerColor = MythosTokens.Panel,
                                labelColor = MythosTokens.TextMuted
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Available Cards Grid
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 145.dp),
                    state = gridState,
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(candidateCards, key = { it.id }) { cardDef ->
                        val inDeckCount = deckFrequencies[cardDef.id] ?: 0
                        val ownedCount = economyState.ownedCardCounts[cardDef.id] ?: 0
                        val isOwned = ownedCount > 0
                        val level = economyState.cardLevels[cardDef.id] ?: 1
                        val card = cardDef.getCard(level)

                        val isMaxCopiesInDeck = inDeckCount >= ActiveDeck.MAX_DUPLICATES_PER_CARD
                        val isMaxOwnedInDeck = inDeckCount >= ownedCount
                        val isDeckFull = workingCardIds.size >= ActiveDeck.REQUIRED_DECK_SIZE
                        val canAdd = isOwned && !isMaxCopiesInDeck && !isMaxOwnedInDeck && !isDeckFull

                        val isHighlighted = highlightedCardId == cardDef.id

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (isHighlighted) 2.dp else 0.dp,
                                    color = if (isHighlighted) MythosTokens.PrimaryGold else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    if (canAdd) {
                                        val mutable = workingCardIds.toMutableList()
                                        mutable.add(cardDef.id)
                                        workingCardIds = mutable
                                        userFeedbackMessage = null
                                    } else {
                                        userFeedbackMessage = when {
                                            !isOwned -> "'${cardDef.name}' is locked. Obtain it from Summons or Shop."
                                            isMaxCopiesInDeck -> "Maximum ${ActiveDeck.MAX_DUPLICATES_PER_CARD} copies of '${cardDef.name}' allowed in deck."
                                            isMaxOwnedInDeck -> "All owned copies of '${cardDef.name}' are already in your deck ($ownedCount/$ownedCount)."
                                            isDeckFull -> "Deck is full (20/20). Remove a card first."
                                            else -> "Cannot add card."
                                        }
                                    }
                                }
                        ) {
                            // Standard Card Frame representation
                            CardFrame(
                                card = card,
                                isPlayable = canAdd,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(205.dp),
                                onClick = {
                                    if (canAdd) {
                                        val mutable = workingCardIds.toMutableList()
                                        mutable.add(cardDef.id)
                                        workingCardIds = mutable
                                        userFeedbackMessage = null
                                    } else {
                                        userFeedbackMessage = when {
                                            !isOwned -> "'${cardDef.name}' is locked. Obtain it from Summons or Shop."
                                            isMaxCopiesInDeck -> "Maximum ${ActiveDeck.MAX_DUPLICATES_PER_CARD} copies of '${cardDef.name}' allowed."
                                            isMaxOwnedInDeck -> "All owned copies are already in deck."
                                            isDeckFull -> "Deck is full (20/20)."
                                            else -> "Cannot add card."
                                        }
                                    }
                                }
                            )

                            // Status Header Badges (Requirement #8: Level, Owned, In Deck)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Level badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xE6161322))
                                        .border(0.5.dp, MythosTokens.PrimaryGold, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Lv.$level",
                                        style = MythosTypography.RarityLabel.copy(fontSize = 8.5.sp),
                                        color = MythosTokens.PrimaryGold
                                    )
                                }

                                // Owned & In-Deck counts (Requirement #8)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (inDeckCount > 0) MythosTokens.PrimaryGold.copy(alpha = 0.9f)
                                            else Color(0xCC2B2638)
                                        )
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Owned: $ownedCount • Deck: $inDeckCount",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (inDeckCount > 0) Color(0xFF161202) else Color(0xFFC7C2D3)
                                    )
                                }
                            }

                            // Locked Card Overlay (Requirement #9)
                            if (!isOwned) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0x8C0B0914)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            tint = MythosTokens.PrimaryGold,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "LOCKED",
                                            style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                            color = MythosTokens.PrimaryGold
                                        )
                                    }
                                }
                            }

                            // Bottom Action Strip (Requirement #9)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(
                                        if (canAdd) MythosTokens.PrimaryGold.copy(alpha = 0.92f)
                                        else Color(0xCC1A1626)
                                    )
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when {
                                        !isOwned -> "LOCKED"
                                        isMaxCopiesInDeck -> "MAX IN DECK (2/2)"
                                        isMaxOwnedInDeck -> "ALL OWNED IN DECK"
                                        isDeckFull -> "DECK FULL (20/20)"
                                        else -> "+ ADD TO DECK ($inDeckCount/2)"
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (canAdd) Color(0xFF161202) else MythosTokens.TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact horizontal badge for a card currently in the active deck tray.
 * Displays artwork, cost, name, rarity border, level, and count (Requirement #7).
 */
@Composable
private fun DeckCardTrayItem(
    card: Card,
    count: Int,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rarityColor = MythosTokens.getRarityColor(card.rarity)

    Surface(
        color = MythosTokens.Panel,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, rarityColor),
        modifier = modifier
            .width(115.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onRemove() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cost gem
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(MythosTokens.EnergyGold),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${card.cost}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF151003)
                    )
                }

                // Count in deck badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(MythosTokens.PrimaryGold)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "×$count",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF161202)
                    )
                }
            }

            // Card name
            Text(
                text = card.name,
                style = MythosTypography.CardName.copy(fontSize = 10.sp),
                color = MythosTokens.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Bottom row: Level and tap to remove
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lv.${card.level}",
                    fontSize = 8.sp,
                    color = MythosTokens.PrimaryGold,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "REMOVE",
                    fontSize = 7.5.sp,
                    color = MythosTokens.Damage,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
