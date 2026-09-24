package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.example.monetization.PlayerEconomyState
import com.example.ui.components.CardFrame
import com.example.ui.components.MythosButton
import com.example.ui.components.MythosButtonStyle
import com.example.ui.theme.MythosTokens
import com.example.ui.theme.MythosTypography

@Composable
fun DeckBuilderScreen(
    onNavigateBack: () -> Unit,
    onStartBattleWithDeck: () -> Unit,
    modifier: Modifier = Modifier
) {
    val economyState by PlayerEconomyRepository.instance.economyState.collectAsState()

    // Local editing state for active deck
    var workingCardIds by remember(economyState.activeDeck) {
        mutableStateOf(economyState.activeDeck.cardIds.toMutableList())
    }
    var selectedHeroId by remember(economyState.selectedHeroId) {
        mutableStateOf(economyState.selectedHeroId)
    }

    var selectedFilterType by remember { mutableStateOf<CardType?>(null) }
    var saveStatusFeedback by remember { mutableStateOf<String?>(null) }

    // Validate in real time (Requirement #10)
    val workingDeck = remember(workingCardIds, selectedHeroId) {
        ActiveDeck(heroId = selectedHeroId, cardIds = workingCardIds.toList())
    }
    val validationResult = remember(workingDeck, economyState.ownedCardCounts) {
        DeckValidator.validate(workingDeck, economyState.ownedCardCounts)
    }

    val heroDef = remember(selectedHeroId) {
        HeroCatalog.findHero(selectedHeroId) ?: HeroCatalog.HERCULES
    }

    // Frequencies in current working deck
    val deckFrequencies = remember(workingCardIds) {
        workingCardIds.groupingBy { it }.eachCount()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MythosTokens.Background)
    ) {
        // Subtle background
        Image(
            painter = painterResource(id = R.drawable.img_battlefield_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.15f
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // TOP BAR
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
                                Text(
                                    text = "DECK BUILDER",
                                    style = MythosTypography.GameTitle.copy(fontSize = 18.sp),
                                    color = MythosTokens.PrimaryGold
                                )
                                Text(
                                    text = "${heroDef.name} • ${workingCardIds.size} / 20 CARDS",
                                    style = MythosTypography.HeroTitle.copy(fontSize = 11.sp),
                                    color = if (workingCardIds.size == 20) MythosTokens.PrimaryGold else MythosTokens.TextMuted
                                )
                            }
                        }

                        // Save Button
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Reset to default
                            OutlinedButton(
                                onClick = {
                                    workingCardIds = HeroCatalog.HERCULES.defaultDeckCardIds.toMutableList()
                                    saveStatusFeedback = "Reset to default Olympus deck."
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.5.dp, MythosTokens.PanelBorder),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("RESET", fontSize = 10.sp, color = MythosTokens.TextMuted)
                            }

                            Button(
                                onClick = {
                                    val result = PlayerEconomyRepository.instance.saveActiveDeck(workingDeck)
                                    if (result.isValid) {
                                        saveStatusFeedback = "Deck saved successfully!"
                                    } else {
                                        saveStatusFeedback = result.primaryErrorMessage ?: "Validation error."
                                    }
                                },
                                enabled = validationResult.isValid,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MythosTokens.PrimaryGold,
                                    contentColor = Color(0xFF161202),
                                    disabledContainerColor = MythosTokens.PanelBorder,
                                    disabledContentColor = MythosTokens.TextMuted
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
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
                        }
                    }

                    // Validation banner (Requirement #10)
                    AnimatedVisibility(visible = !validationResult.isValid || saveStatusFeedback != null) {
                        Surface(
                            color = if (validationResult.isValid) MythosTokens.PanelElevated else MythosTokens.Damage.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(
                                0.5.dp,
                                if (validationResult.isValid) MythosTokens.PrimaryGold else MythosTokens.Damage
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
                                    imageVector = if (validationResult.isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (validationResult.isValid) MythosTokens.PrimaryGold else MythosTokens.Damage,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = saveStatusFeedback ?: validationResult.primaryErrorMessage ?: "",
                                    style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                                    color = if (validationResult.isValid) MythosTokens.PrimaryGold else MythosTokens.Damage,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 1: ACTIVE DECK TRAY (Cards currently in deck)
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
                        }

                        Text(
                            text = "Tap card to remove",
                            fontSize = 10.sp,
                            color = MythosTokens.TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Horizontal list of unique cards in deck
                    val uniqueDeckCardIds = remember(workingCardIds) { workingCardIds.distinct() }

                    if (uniqueDeckCardIds.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Deck is empty. Add cards from the collection below.",
                                style = MythosTypography.CardDescription,
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

                                DeckCardCompactBadge(
                                    card = card,
                                    count = count,
                                    onRemove = {
                                        val mutable = workingCardIds.toMutableList()
                                        mutable.remove(cardId)
                                        workingCardIds = mutable
                                        saveStatusFeedback = null
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 2: CARD COLLECTION POOL
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Filter row for collection pool
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

                    CardType.values().forEach { type ->
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

                Spacer(modifier = Modifier.height(8.dp))

                // Owned cards eligible for deck
                val candidateCards = remember(selectedFilterType, economyState.ownedCardIds) {
                    CardCatalog.ALL_CARDS.filter { def ->
                        economyState.ownedCardIds.contains(def.id) &&
                                (selectedFilterType == null || def.type == selectedFilterType)
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 145.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(candidateCards, key = { it.id }) { cardDef ->
                        val inDeckCount = deckFrequencies[cardDef.id] ?: 0
                        val ownedCount = economyState.ownedCardCounts[cardDef.id] ?: 0
                        val level = economyState.cardLevels[cardDef.id] ?: 1
                        val card = cardDef.getCard(level)

                        val isMaxCopiesInDeck = inDeckCount >= ActiveDeck.MAX_DUPLICATES_PER_CARD
                        val isMaxOwnedInDeck = inDeckCount >= ownedCount
                        val isDeckFull = workingCardIds.size >= ActiveDeck.REQUIRED_DECK_SIZE
                        val canAdd = !isMaxCopiesInDeck && !isMaxOwnedInDeck && !isDeckFull

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = canAdd) {
                                    val mutable = workingCardIds.toMutableList()
                                    mutable.add(cardDef.id)
                                    workingCardIds = mutable
                                    saveStatusFeedback = null
                                }
                        ) {
                            CardFrame(
                                card = card,
                                isPlayable = canAdd,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                onClick = {
                                    if (canAdd) {
                                        val mutable = workingCardIds.toMutableList()
                                        mutable.add(cardDef.id)
                                        workingCardIds = mutable
                                        saveStatusFeedback = null
                                    }
                                }
                            )

                            // Status badge on top
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
                                        text = "DECK: $inDeckCount/$ownedCount",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (inDeckCount > 0) Color(0xFF161202) else Color(0xFFC7C2D3)
                                    )
                                }
                            }

                            // Add Button Overlay at bottom
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(
                                        if (canAdd) MythosTokens.PrimaryGold.copy(alpha = 0.9f)
                                        else Color(0xCC1A1626)
                                    )
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when {
                                        isMaxCopiesInDeck -> "MAX COPIES (2/2)"
                                        isMaxOwnedInDeck -> "ALL OWNED IN DECK"
                                        isDeckFull -> "DECK FULL (20/20)"
                                        else -> "+ ADD TO DECK"
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
 */
@Composable
private fun DeckCardCompactBadge(
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
            .width(110.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onRemove() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
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

            Text(
                text = card.name,
                style = MythosTypography.CardName.copy(fontSize = 10.sp),
                color = MythosTokens.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

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
                    text = "TAP TO REMOVE",
                    fontSize = 7.5.sp,
                    color = MythosTokens.Damage,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
