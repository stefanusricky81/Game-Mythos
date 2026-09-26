package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.*
import com.example.monetization.PlayerEconomyRepository
import com.example.monetization.PlayerEconomyState
import com.example.ui.components.CardArtwork
import com.example.ui.components.CardFrame
import com.example.ui.components.HeroDetailUpgradeDialog
import com.example.ui.components.MythosButton
import com.example.ui.components.MythosButtonStyle
import com.example.ui.theme.MythosTokens
import com.example.ui.theme.MythosTypography
import java.text.NumberFormat
import java.util.Locale

enum class CollectionTab(val label: String) {
    HEROES("HEROES"),
    CARDS("CARDS")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    onNavigateBack: () -> Unit,
    onOpenDeckBuilder: (String?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val economyState by PlayerEconomyRepository.instance.economyState.collectAsState()
    var selectedTab by remember { mutableStateOf(CollectionTab.CARDS) }

    // Filters for cards (Requirement #5)
    var selectedRarityFilter by remember { mutableStateOf<CardRarity?>(null) }
    var selectedTypeFilter by remember { mutableStateOf<CardType?>(null) }
    var selectedOwnershipFilter by remember { mutableStateOf<Boolean?>(null) } // null = All, true = Owned, false = Unowned

    // Detail dialog state
    var inspectingCardDef by remember { mutableStateOf<CardDefinition?>(null) }
    var inspectingHeroDef by remember { mutableStateOf<HeroDefinition?>(null) }
    var upgradeFeedbackMessage by remember { mutableStateOf<String?>(null) }

    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

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
            // 1. TOP APP BAR
            Surface(
                color = MythosTokens.Panel,
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.testTag("collection_back_button")
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
                                    text = "COLLECTION",
                                    style = MythosTypography.GameTitle.copy(fontSize = 18.sp),
                                    color = MythosTokens.PrimaryGold
                                )
                                Text(
                                    text = "${economyState.ownedCardIds.size} / ${CardCatalog.ALL_CARDS.size} Cards Owned",
                                    style = MythosTypography.HeroTitle.copy(fontSize = 10.sp),
                                    color = MythosTokens.SecondaryGold
                                )
                            }
                        }

                        // Currencies & Deck Builder action
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Gold
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MythosTokens.PanelElevated)
                                    .border(0.5.dp, MythosTokens.PanelBorder, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "🪙", fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = numberFormat.format(economyState.gold),
                                    style = MythosTypography.HeroName.copy(fontSize = 11.sp),
                                    color = MythosTokens.PrimaryGold
                                )
                            }

                            // Deck Builder Button
                            Button(
                                onClick = { onOpenDeckBuilder(null) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MythosTokens.PanelElevated,
                                    contentColor = MythosTokens.PrimaryGold
                                ),
                                border = BorderStroke(1.dp, MythosTokens.PrimaryGold),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp).testTag("collection_deck_builder_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "DECK (${economyState.activeDeck.totalCards}/20)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // TAB SELECTOR: [ HEROES ] [ CARDS ]
                    TabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        containerColor = MythosTokens.Panel,
                        contentColor = MythosTokens.PrimaryGold,
                        indicator = { tabPositions ->
                            if (selectedTab.ordinal < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                                    height = 2.5.dp,
                                    color = MythosTokens.PrimaryGold
                                )
                            }
                        },
                        divider = {
                            HorizontalDivider(color = MythosTokens.PanelBorder, thickness = 1.dp)
                        }
                    ) {
                        CollectionTab.values().forEach { tab ->
                            Tab(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                text = {
                                    Text(
                                        text = tab.label,
                                        style = MythosTypography.RarityLabel.copy(
                                            fontSize = 12.sp,
                                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (selectedTab == tab) MythosTokens.PrimaryGold else MythosTokens.TextMuted
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // 2. CONTENT AREA
            when (selectedTab) {
                CollectionTab.HEROES -> {
                    HeroesCollectionContent(
                        economyState = economyState,
                        onSelectHero = { heroDef ->
                            PlayerEconomyRepository.instance.selectHero(heroDef.id)
                        },
                        onInspectHero = { heroDef ->
                            inspectingHeroDef = heroDef
                        }
                    )
                }

                CollectionTab.CARDS -> {
                    CardsCollectionContent(
                        economyState = economyState,
                        selectedRarity = selectedRarityFilter,
                        onSelectRarity = { selectedRarityFilter = it },
                        selectedType = selectedTypeFilter,
                        onSelectType = { selectedTypeFilter = it },
                        selectedOwnership = selectedOwnershipFilter,
                        onSelectOwnership = { selectedOwnershipFilter = it },
                        onCardClicked = { cardDef ->
                            inspectingCardDef = cardDef
                        }
                    )
                }
            }
        }

        // 3. CARD DETAIL & UPGRADE MODAL (Requirement #6)
        inspectingCardDef?.let { cardDef ->
            CardDetailUpgradeDialog(
                cardDef = cardDef,
                economyState = economyState,
                onUpgrade = {
                    val result = PlayerEconomyRepository.instance.upgradeCard(cardDef.id)
                    if (result.isSuccess) {
                        upgradeFeedbackMessage = "Successfully upgraded ${cardDef.name}!"
                    } else {
                        upgradeFeedbackMessage = result.exceptionOrNull()?.message ?: "Upgrade failed."
                    }
                },
                onOpenDeckBuilder = {
                    val targetCardId = cardDef.id
                    inspectingCardDef = null
                    onOpenDeckBuilder(targetCardId)
                },
                onDismiss = {
                    inspectingCardDef = null
                    upgradeFeedbackMessage = null
                },
                feedbackMessage = upgradeFeedbackMessage
            )
        }

        // 4. HERO DETAIL & UPGRADE MODAL (Phase 7C Section 10 & 17)
        inspectingHeroDef?.let { heroDef ->
            HeroDetailUpgradeDialog(
                heroDef = heroDef,
                economyState = economyState,
                onSelectHero = {
                    PlayerEconomyRepository.instance.selectHero(heroDef.id)
                    inspectingHeroDef = null
                },
                onDismiss = { inspectingHeroDef = null }
            )
        }
    }
}

/**
 * Tab 1: Heroes Collection (Requirement #4, #17, #18).
 */
@Composable
private fun HeroesCollectionContent(
    economyState: PlayerEconomyState,
    onSelectHero: (HeroDefinition) -> Unit,
    onInspectHero: (HeroDefinition) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "ACTIVE CHAMPION",
                style = MythosTypography.RarityLabel.copy(fontSize = 11.sp),
                color = MythosTokens.PrimaryGold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Roster: 1 Playable • ${HeroCatalog.UPCOMING_HEROES.size} Archive",
                style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                color = MythosTokens.TextMuted
            )
        }

        // Canonical Hercules Featured Showcase
        val hercules = HeroCatalog.HERCULES
        val isHerculesSelected = economyState.selectedHeroId == hercules.id
        val herculesShards = economyState.heroShards[hercules.id] ?: 20
        val herculesLevel = economyState.heroProgression[hercules.id] ?: 1

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(
                    1.5.dp,
                    if (isHerculesSelected) MythosTokens.PrimaryGold else MythosTokens.PanelBorder,
                    RoundedCornerShape(16.dp)
                )
                .clickable { onInspectHero(hercules) },
            colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, MythosTokens.PrimaryGold, RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            painter = painterResource(id = hercules.portraitResId),
                            contentDescription = hercules.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = hercules.name,
                                style = MythosTypography.HeroName.copy(fontSize = 18.sp),
                                color = MythosTokens.PrimaryGold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MythosTokens.Legendary.copy(alpha = 0.2f))
                                    .border(0.5.dp, MythosTokens.Legendary, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "LEGENDARY",
                                    style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                    color = MythosTokens.Legendary
                                )
                            }
                        }

                        Text(
                            text = hercules.title,
                            style = MythosTypography.HeroTitle.copy(fontSize = 12.sp),
                            color = MythosTokens.TextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Level $herculesLevel",
                                style = MythosTypography.CardName.copy(fontSize = 11.sp),
                                color = MythosTokens.DivineBlueLight
                            )
                            Text(
                                text = "•",
                                color = MythosTokens.TextMuted,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "$herculesShards / 50 Shards",
                                style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                                color = MythosTokens.SecondaryGold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Combat identity badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MythosTokens.PanelElevated)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = hercules.combatIdentity,
                        style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                        color = Color(0xFFDDD8E8)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isHerculesSelected) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MythosTokens.PrimaryGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ACTIVE HERO IN DECK",
                                style = MythosTypography.RarityLabel.copy(fontSize = 11.sp),
                                color = MythosTokens.PrimaryGold
                            )
                        }
                    } else {
                        MythosButton(
                            text = "SELECT HERO",
                            onClick = { onSelectHero(hercules) },
                            style = MythosButtonStyle.PRIMARY,
                            modifier = Modifier.height(38.dp).width(140.dp)
                        )
                    }

                    OutlinedButton(
                        onClick = { onInspectHero(hercules) },
                        border = BorderStroke(0.5.dp, MythosTokens.PanelBorder),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "HERO DETAILS",
                            fontSize = 11.sp,
                            color = MythosTokens.TextPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ARCHIVE & UPCOMING HEROES (Section 3 Extensible Architecture)
        Text(
            text = "OLYMPIAN ARCHIVE • UPCOMING HEROES",
            style = MythosTypography.RarityLabel.copy(fontSize = 11.sp),
            color = MythosTokens.TextMuted,
            letterSpacing = 1.sp
        )

        HeroCatalog.UPCOMING_HEROES.forEach { heroDef ->
            val heroProgress = economyState.getHeroProgress(heroDef.id)
            val isUnlocked = heroProgress.isUnlocked
            val isSelected = economyState.selectedHeroId == heroDef.id

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        if (isSelected) 1.5.dp else 0.5.dp,
                        if (isSelected) MythosTokens.PrimaryGold else if (isUnlocked) MythosTokens.Success.copy(alpha = 0.6f) else MythosTokens.PanelBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onInspectHero(heroDef) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isUnlocked) MythosTokens.Panel else MythosTokens.Panel.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                1.dp,
                                if (isUnlocked) MythosTokens.getRarityColor(heroDef.rarity)
                                else MythosTokens.getRarityColor(heroDef.rarity).copy(alpha = 0.5f),
                                RoundedCornerShape(10.dp)
                            )
                    ) {
                        Image(
                            painter = painterResource(id = heroDef.portraitResId),
                            contentDescription = heroDef.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = if (isUnlocked) 1.0f else 0.45f
                        )
                        if (!isUnlocked) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = MythosTokens.TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = heroDef.name,
                                style = MythosTypography.HeroName.copy(fontSize = 15.sp),
                                color = if (isUnlocked) MythosTokens.TextPrimary else MythosTokens.TextMuted
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MythosTokens.getRarityColor(heroDef.rarity).copy(alpha = 0.15f))
                                    .border(0.5.dp, MythosTokens.getRarityColor(heroDef.rarity).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = heroDef.rarity.label.uppercase(),
                                    style = MythosTypography.RarityLabel.copy(fontSize = 8.sp),
                                    color = MythosTokens.getRarityColor(heroDef.rarity)
                                )
                            }
                        }

                        Text(
                            text = if (isUnlocked) "Level ${heroProgress.level} • ${heroProgress.currentShards} Shards"
                            else heroDef.title,
                            style = MythosTypography.HeroTitle.copy(fontSize = 11.sp),
                            color = if (isUnlocked) MythosTokens.SecondaryGold else MythosTokens.TextMuted
                        )

                        Text(
                            text = heroDef.combatIdentity,
                            style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                            color = Color(0xFFAAA5B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Status Badge (Phase 7C Section 17)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when {
                                    isSelected -> MythosTokens.PrimaryGold.copy(alpha = 0.2f)
                                    isUnlocked -> MythosTokens.Success.copy(alpha = 0.2f)
                                    else -> MythosTokens.PanelElevated
                                }
                            )
                            .border(
                                0.5.dp,
                                when {
                                    isSelected -> MythosTokens.PrimaryGold
                                    isUnlocked -> MythosTokens.Success
                                    else -> MythosTokens.PanelBorder
                                },
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = when {
                                isSelected -> "ACTIVE"
                                isUnlocked -> "LV ${heroProgress.level}"
                                heroDef.id == "hero_achilles" -> "STAGE 2"
                                heroDef.id == "hero_merlin" -> "STAGE 5"
                                else -> "ARCHIVED"
                            },
                            style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                            color = when {
                                isSelected -> MythosTokens.PrimaryGold
                                isUnlocked -> MythosTokens.Success
                                else -> MythosTokens.TextMuted
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tab 2: Cards Collection Grid with Filters (Requirements #4, #5, #6).
 */
@Composable
private fun CardsCollectionContent(
    economyState: PlayerEconomyState,
    selectedRarity: CardRarity?,
    onSelectRarity: (CardRarity?) -> Unit,
    selectedType: CardType?,
    onSelectType: (CardType?) -> Unit,
    selectedOwnership: Boolean?,
    onSelectOwnership: (Boolean?) -> Unit,
    onCardClicked: (CardDefinition) -> Unit,
    modifier: Modifier = Modifier
) {
    // Filtered card list
    val filteredCards = remember(selectedRarity, selectedType, selectedOwnership, economyState) {
        CardCatalog.ALL_CARDS.filter { def ->
            val matchesRarity = selectedRarity == null || def.rarity == selectedRarity
            val matchesType = selectedType == null || def.type == selectedType
            val isOwned = economyState.ownedCardIds.contains(def.id)
            val matchesOwnership = when (selectedOwnership) {
                true -> isOwned
                false -> !isOwned
                null -> true
            }
            matchesRarity && matchesType && matchesOwnership
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // COMPACT FILTER BAR (Requirement #5)
        Surface(
            color = MythosTokens.PanelElevated,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                // Rarity filters
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedRarity == null,
                        onClick = { onSelectRarity(null) },
                        label = { Text("ALL", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MythosTokens.PrimaryGold,
                            selectedLabelColor = Color(0xFF13101B),
                            containerColor = MythosTokens.Panel,
                            labelColor = MythosTokens.TextPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedRarity == null,
                            borderColor = MythosTokens.PanelBorder,
                            selectedBorderColor = MythosTokens.PrimaryGold
                        ),
                        modifier = Modifier.height(28.dp)
                    )

                    CardRarity.values().forEach { rarity ->
                        val isSel = selectedRarity == rarity
                        val color = MythosTokens.getRarityColor(rarity)
                        FilterChip(
                            selected = isSel,
                            onClick = { onSelectRarity(if (isSel) null else rarity) },
                            label = { Text(rarity.name, fontSize = 10.5.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.25f),
                                selectedLabelColor = color,
                                containerColor = MythosTokens.Panel,
                                labelColor = MythosTokens.TextMuted
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSel,
                                borderColor = MythosTokens.PanelBorder,
                                selectedBorderColor = color
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Type & Ownership Filters
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ownership quick chips
                    FilterChip(
                        selected = selectedOwnership == null,
                        onClick = { onSelectOwnership(null) },
                        label = { Text("All Status", fontSize = 10.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MythosTokens.DivineBlueLight.copy(alpha = 0.3f),
                            selectedLabelColor = MythosTokens.DivineBlueLight,
                            containerColor = MythosTokens.Panel,
                            labelColor = MythosTokens.TextMuted
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                    FilterChip(
                        selected = selectedOwnership == true,
                        onClick = { onSelectOwnership(if (selectedOwnership == true) null else true) },
                        label = { Text("Owned", fontSize = 10.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MythosTokens.PrimaryGold.copy(alpha = 0.25f),
                            selectedLabelColor = MythosTokens.PrimaryGold,
                            containerColor = MythosTokens.Panel,
                            labelColor = MythosTokens.TextMuted
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                    FilterChip(
                        selected = selectedOwnership == false,
                        onClick = { onSelectOwnership(if (selectedOwnership == false) null else false) },
                        label = { Text("Unowned", fontSize = 10.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6B7280).copy(alpha = 0.3f),
                            selectedLabelColor = Color(0xFFE5E7EB),
                            containerColor = MythosTokens.Panel,
                            labelColor = MythosTokens.TextMuted
                        ),
                        modifier = Modifier.height(28.dp)
                    )

                    VerticalDivider(modifier = Modifier.height(18.dp), color = MythosTokens.PanelBorder)

                    // Type chips
                    CardType.values().forEach { type ->
                        val isSel = selectedType == type
                        val color = MythosTokens.getCardTypeColor(type)
                        FilterChip(
                            selected = isSel,
                            onClick = { onSelectType(if (isSel) null else type) },
                            label = { Text(type.label, fontSize = 10.5.sp) },
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
            }
        }

        // CARDS GRID (Requirement #4)
        if (filteredCards.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No cards match the active filters.",
                    style = MythosTypography.CardDescription,
                    color = MythosTokens.TextMuted
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredCards, key = { it.id }) { cardDef ->
                    val isOwned = economyState.ownedCardIds.contains(cardDef.id)
                    val ownedCount = economyState.ownedCardCounts[cardDef.id] ?: 0
                    val currentLevel = economyState.cardLevels[cardDef.id] ?: 1
                    val cardInstance = cardDef.getCard(currentLevel)
                    val shards = economyState.cardShards[cardDef.id] ?: 0

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onCardClicked(cardDef) }
                    ) {
                        CardFrame(
                            card = cardInstance,
                            isPlayable = isOwned,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp),
                            onClick = { onCardClicked(cardDef) }
                        )

                        // Top Badges Overlay: Level & Ownership
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Level Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xE6161322))
                                    .border(0.5.dp, MythosTokens.PrimaryGold, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Lv.$currentLevel",
                                    style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                    color = MythosTokens.PrimaryGold
                                )
                            }

                            // Ownership Badge (Requirement #4)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isOwned) MythosTokens.DivineBlueLight.copy(alpha = 0.85f)
                                        else Color(0xCC2B2638)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isOwned) "×$ownedCount" else "LOCKED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isOwned) Color(0xFF0D1B2A) else Color(0xFFE2D6A5)
                                )
                            }
                        }

                        // Locked Card Visual Overlay (Requirement #5)
                        if (!isOwned) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x800B0914)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked Card",
                                        tint = MythosTokens.PrimaryGold,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "LOCKED",
                                        style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                        color = MythosTokens.PrimaryGold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }

                        // Shards Progress Indicator at Bottom
                        if (isOwned) {
                            val upgradeCost = cardDef.getUpgradeCost(currentLevel)
                            val reqShards = upgradeCost?.second ?: 0
                            if (reqShards > 0) {
                                val progress = (shards.toFloat() / reqShards).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .background(Color(0xCC100D1A))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "💎 $shards/$reqShards",
                                            fontSize = 8.5.sp,
                                            color = if (shards >= reqShards) MythosTokens.PrimaryGold else MythosTokens.TextMuted,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (shards >= reqShards) {
                                            Text(
                                                text = "UPGRADE READY",
                                                fontSize = 8.sp,
                                                color = MythosTokens.PrimaryGold,
                                                fontWeight = FontWeight.Black
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
}

/**
 * Card Detail & Upgrade Dialog (Requirements #6, #7, #8).
 */
@Composable
private fun CardDetailUpgradeDialog(
    cardDef: CardDefinition,
    economyState: PlayerEconomyState,
    onUpgrade: () -> Unit,
    onOpenDeckBuilder: () -> Unit,
    onDismiss: () -> Unit,
    feedbackMessage: String?
) {
    val isOwned = economyState.ownedCardIds.contains(cardDef.id)
    val ownedCount = economyState.ownedCardCounts[cardDef.id] ?: 0
    val currentLevel = economyState.cardLevels[cardDef.id] ?: 1
    val currentCard = cardDef.getCard(currentLevel)
    val shards = economyState.cardShards[cardDef.id] ?: 0

    val upgradeCost = cardDef.getUpgradeCost(currentLevel)
    val isMaxLevel = currentLevel >= cardDef.maxLevel
    val goldCost = upgradeCost?.first ?: 0
    val shardCost = upgradeCost?.second ?: 0

    val hasEnoughGold = economyState.gold >= goldCost
    val hasEnoughShards = shards >= shardCost
    val canUpgrade = isOwned && !isMaxLevel && hasEnoughGold && hasEnoughShards

    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(18.dp))
                .border(1.5.dp, MythosTokens.getRarityColor(cardDef.rarity), RoundedCornerShape(18.dp)),
            color = MythosTokens.Panel
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MythosTokens.getRarityColor(cardDef.rarity).copy(alpha = 0.2f))
                            .border(0.5.dp, MythosTokens.getRarityColor(cardDef.rarity), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${cardDef.rarity.label.uppercase()} • ${cardDef.type.label.uppercase()}",
                            style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                            color = MythosTokens.getRarityColor(cardDef.rarity)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MythosTokens.TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Card Preview Frame
                Box(modifier = Modifier.width(170.dp).height(240.dp)) {
                    CardFrame(
                        card = currentCard,
                        isPlayable = isOwned,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Card Name & Level
                Text(
                    text = cardDef.name.uppercase(),
                    style = MythosTypography.GameTitle.copy(fontSize = 20.sp),
                    color = MythosTokens.PrimaryGold,
                    textAlign = TextAlign.Center
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LEVEL $currentLevel / ${cardDef.maxLevel}",
                        style = MythosTypography.HeroTitle.copy(fontSize = 12.sp),
                        color = MythosTokens.DivineBlueLight
                    )
                    Text(
                        text = "•",
                        color = MythosTokens.TextMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "⚡ ${currentCard.cost} Energy",
                        style = MythosTypography.HeroTitle.copy(fontSize = 12.sp),
                        color = MythosTokens.DivineBlue
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Current Effect & Lore
                Surface(
                    color = MythosTokens.PanelElevated,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "CURRENT EFFECT (LV. $currentLevel)",
                            style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                            color = MythosTokens.SecondaryGold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentCard.effectDescription,
                            style = MythosTypography.CardDescription.copy(fontSize = 12.sp),
                            color = MythosTokens.TextPrimary
                        )

                        // Next level preview
                        if (!isMaxLevel && cardDef.progression[currentLevel + 1] != null) {
                            val nextStep = cardDef.progression[currentLevel + 1]!!
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MythosTokens.PanelBorder, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "NEXT LEVEL (LV. ${currentLevel + 1}):",
                                style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                color = MythosTokens.DivineBlueLight
                            )
                            Text(
                                text = nextStep.effectDescription,
                                style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                                color = Color(0xFFC2BED0)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Owned Copies & Shards (Requirement #6)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Copies Owned: $ownedCount",
                        style = MythosTypography.HeroName.copy(fontSize = 12.sp),
                        color = MythosTokens.TextPrimary
                    )

                    Text(
                        text = "Shards: $shards / $shardCost",
                        style = MythosTypography.HeroName.copy(fontSize = 12.sp),
                        color = if (hasEnoughShards) MythosTokens.PrimaryGold else MythosTokens.TextMuted
                    )
                }

                // Shards Progress Bar
                if (!isMaxLevel && shardCost > 0) {
                    val progress = (shards.toFloat() / shardCost).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MythosTokens.PrimaryGold,
                        trackColor = MythosTokens.PanelElevated
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // UPGRADE SECTION
                if (isMaxLevel) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MythosTokens.PanelElevated)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "★ MAXIMUM LEVEL REACHED ★",
                            style = MythosTypography.RarityLabel.copy(fontSize = 12.sp),
                            color = MythosTokens.PrimaryGold
                        )
                    }
                } else {
                    // Upgrade Cost display
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MythosTokens.PanelElevated)
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Upgrade:\n${numberFormat.format(goldCost)} Gold + $shardCost Shards",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = if (canUpgrade) MythosTokens.PrimaryGold else MythosTokens.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🪙 ${numberFormat.format(goldCost)} Gold",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasEnoughGold) MythosTokens.PrimaryGold else MythosTokens.Damage
                            )
                            Text(
                                text = "💎 $shardCost Shards",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasEnoughShards) MythosTokens.DivineBlueLight else MythosTokens.Damage
                            )
                        }

                        // Resource shortfall hint if unable to upgrade (Requirement #6)
                        if (!canUpgrade && isOwned) {
                            Spacer(modifier = Modifier.height(4.dp))
                            val neededGold = (goldCost - economyState.gold).coerceAtLeast(0)
                            val neededShards = (shardCost - shards).coerceAtLeast(0)
                            val neededText = buildString {
                                append("NEED: ")
                                if (neededGold > 0) append("${numberFormat.format(neededGold)} Gold ")
                                if (neededShards > 0) append("$neededShards Shards")
                            }
                            Text(
                                text = neededText,
                                style = MythosTypography.CardDescription.copy(fontSize = 10.5.sp),
                                color = MythosTokens.Damage,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Feedback toast message if upgrade clicked
                    feedbackMessage?.let { msg ->
                        Text(
                            text = msg,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (msg.contains("Success", ignoreCase = true)) MythosTokens.PrimaryGold else MythosTokens.Damage,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    // UPGRADE BUTTON
                    Button(
                        onClick = onUpgrade,
                        enabled = canUpgrade,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("card_upgrade_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MythosTokens.PrimaryGold,
                            contentColor = Color(0xFF161202),
                            disabledContainerColor = MythosTokens.PanelBorder,
                            disabledContentColor = MythosTokens.TextMuted
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Upgrade, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "UPGRADE TO LEVEL ${currentLevel + 1}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action buttons: Add to Deck / Deck Builder
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenDeckBuilder,
                        modifier = Modifier.weight(1f).height(40.dp),
                        border = BorderStroke(1.dp, MythosTokens.PanelBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("DECK BUILDER", fontSize = 11.sp, color = MythosTokens.TextPrimary)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(40.dp),
                        border = BorderStroke(1.dp, MythosTokens.PanelBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("CLOSE", fontSize = 11.sp, color = MythosTokens.TextMuted)
                    }
                }
            }
        }
    }
}

/**
 * Hero Detail Dialog (Requirement #17).
 */
@Composable
private fun HeroDetailDialog(
    heroDef: HeroDefinition,
    isSelected: Boolean,
    isOwned: Boolean,
    economyState: PlayerEconomyState,
    onSelectHero: () -> Unit,
    onDismiss: () -> Unit
) {
    val level = economyState.heroProgression[heroDef.id] ?: 1
    val shards = economyState.heroShards[heroDef.id] ?: 20

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(18.dp))
                .border(1.5.dp, MythosTokens.getRarityColor(heroDef.rarity), RoundedCornerShape(18.dp)),
            color = MythosTokens.Panel
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MythosTokens.getRarityColor(heroDef.rarity).copy(alpha = 0.2f))
                            .border(0.5.dp, MythosTokens.getRarityColor(heroDef.rarity), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = heroDef.rarity.label.uppercase(),
                            style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                            color = MythosTokens.getRarityColor(heroDef.rarity)
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MythosTokens.TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Canonical Hero Portrait (Requirement #17)
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(2.dp, MythosTokens.PrimaryGold, RoundedCornerShape(14.dp))
                ) {
                    Image(
                        painter = painterResource(id = heroDef.portraitResId),
                        contentDescription = heroDef.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = heroDef.name.uppercase(),
                    style = MythosTypography.GameTitle.copy(fontSize = 22.sp),
                    color = MythosTokens.PrimaryGold
                )

                Text(
                    text = heroDef.title,
                    style = MythosTypography.HeroTitle.copy(fontSize = 13.sp),
                    color = MythosTokens.TextPrimary
                )

                Text(
                    text = heroDef.faction,
                    style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                    color = MythosTokens.TextMuted
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Progression Info (Requirement #17)
                Surface(
                    color = MythosTokens.PanelElevated,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Hero Level", style = MythosTypography.CardDescription, color = MythosTokens.TextMuted)
                            Text(text = "Level $level", style = MythosTypography.HeroName.copy(fontSize = 12.sp), color = MythosTokens.DivineBlueLight)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Hero Shards", style = MythosTypography.CardDescription, color = MythosTokens.TextMuted)
                            Text(text = "$shards / 50", style = MythosTypography.HeroName.copy(fontSize = 12.sp), color = MythosTokens.SecondaryGold)
                        }

                        LinearProgressIndicator(
                            progress = { (shards.toFloat() / 50).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = MythosTokens.PrimaryGold,
                            trackColor = MythosTokens.PanelBorder
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Combat Identity (Requirement #17)
                Surface(
                    color = MythosTokens.PanelElevated,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "COMBAT IDENTITY",
                            style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                            color = MythosTokens.SecondaryGold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = heroDef.combatIdentity,
                            style = MythosTypography.CardDescription.copy(fontSize = 11.5.sp),
                            color = MythosTokens.TextPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MythosTokens.PanelBorder, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "HP", fontSize = 10.sp, color = MythosTokens.TextMuted)
                                Text(text = "${heroDef.baseHp}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MythosTokens.HealthGreen)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "ATK", fontSize = 10.sp, color = MythosTokens.TextMuted)
                                Text(text = "${heroDef.baseAttack}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MythosTokens.Damage)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "DEF", fontSize = 10.sp, color = MythosTokens.TextMuted)
                                Text(text = "${heroDef.baseDefense}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MythosTokens.DivineBlueLight)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Selection button (Requirement #18)
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MythosTokens.PanelElevated)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✓ CURRENTLY SELECTED HERO",
                            style = MythosTypography.RarityLabel.copy(fontSize = 11.sp),
                            color = MythosTokens.PrimaryGold
                        )
                    }
                } else if (isOwned && heroDef.isPlayableInPrototype) {
                    MythosButton(
                        text = "SELECT HERO",
                        onClick = onSelectHero,
                        style = MythosButtonStyle.PRIMARY,
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MythosTokens.PanelElevated)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "COMING SOON IN FUTURE EXPANSION",
                            style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                            color = MythosTokens.TextMuted
                        )
                    }
                }
            }
        }
    }
}
