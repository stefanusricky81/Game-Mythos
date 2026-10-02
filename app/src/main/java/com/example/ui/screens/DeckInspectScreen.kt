package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
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
import com.example.data.*
import com.example.monetization.PlayerEconomyRepository
import com.example.ui.components.CardDetailDialog
import com.example.ui.components.CardFrame
import com.example.ui.theme.MythosTokens
import com.example.ui.theme.MythosTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckInspectScreen(
    onNavigateBack: () -> Unit,
    onOpenDeckBuilder: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val economyState by PlayerEconomyRepository.instance.economyState.collectAsState()
    val activeDeck = economyState.activeDeck
    val fullDeck = remember(activeDeck.cardIds, economyState.cardLevels) {
        activeDeck.cardIds.map { cardId ->
            val level = economyState.cardLevels[cardId] ?: 1
            CardCatalog.getCard(cardId, level)
        }
    }
    val heroDef = remember(activeDeck.heroId) {
        HeroCatalog.findHero(activeDeck.heroId) ?: HeroCatalog.HERCULES
    }
    val deckAnalysis = remember(fullDeck, heroDef) {
        SynergyCatalog.analyzeDeck(fullDeck, heroDef)
    }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedTypeFilter by remember { mutableStateOf<CardType?>(null) }
    var selectedCardForDetail by remember { mutableStateOf<Card?>(null) }

    val filteredCards = remember(selectedTypeFilter, fullDeck) {
        if (selectedTypeFilter == null) fullDeck else fullDeck.filter { it.type == selectedTypeFilter }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MYTHOS ARCHIVES",
                            style = MythosTypography.GameTitle.copy(fontSize = 15.sp),
                            color = MythosTokens.PrimaryGold
                        )
                        Text(
                            text = if (selectedTab == 0) "${fullDeck.size}/20 Active Deck Cards • ${activeDeck.name}" else "Hercules Canonical Character Pipeline",
                            style = MythosTypography.HeroTitle.copy(fontSize = 11.sp),
                            color = MythosTokens.TextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MythosTokens.PrimaryGold
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = onOpenDeckBuilder,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MythosTokens.PrimaryGold,
                            contentColor = Color(0xFF161202)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .height(32.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("EDIT DECK", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MythosTokens.BackgroundSurface
                )
            )
        },
        containerColor = MythosTokens.Background
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp)
        ) {
            // Screen Tabs: Cards vs Hero Collection Pipeline
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MythosTokens.Panel,
                contentColor = MythosTokens.PrimaryGold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "DECK CARDS (${fullDeck.size})",
                            style = MythosTypography.ButtonText.copy(fontSize = 11.sp)
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "HERO PIPELINE (Hercules)",
                            style = MythosTypography.ButtonText.copy(fontSize = 11.sp)
                        )
                    }
                )
            }

            if (selectedTab == 0) {
                // DECK ANALYSIS SECTION (Requirement #9, #10)
                Surface(
                    color = MythosTokens.Panel,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MythosTokens.SecondaryGold.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("deck_inspect_analysis_card")
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DECK ANALYSIS",
                                style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                                color = MythosTokens.PrimaryGold,
                                letterSpacing = 1.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Deck Power: ${java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(deckAnalysis.deckPower)}",
                                    style = MythosTypography.StatNumber.copy(fontSize = 11.sp),
                                    color = MythosTokens.PrimaryGold
                                )
                                Text(
                                    text = "Avg Energy: ${deckAnalysis.averageEnergyCost}",
                                    style = MythosTypography.StatNumber.copy(fontSize = 11.sp),
                                    color = MythosTokens.DivineBlueLight
                                )
                            }
                        }

                        // Distribution
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CardType.values().forEach { type ->
                                val count = deckAnalysis.typeDistribution[type] ?: 0
                                if (count > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MythosTokens.getCardTypeColor(type).copy(alpha = 0.2f),
                                        border = BorderStroke(0.5.dp, MythosTokens.getCardTypeColor(type))
                                    ) {
                                        Text(
                                            text = "${type.label}: $count",
                                            style = MythosTypography.CardDescription.copy(fontSize = 9.5.sp),
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Synergies
                        if (deckAnalysis.activeSynergies.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                deckAnalysis.activeSynergies.forEach { syn ->
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MythosTokens.PrimaryGold.copy(alpha = 0.18f),
                                        border = BorderStroke(0.5.dp, MythosTokens.PrimaryGold)
                                    ) {
                                        Text(
                                            text = "${syn.iconSymbol} ${syn.name}",
                                            style = MythosTypography.CardDescription.copy(fontSize = 9.sp),
                                            color = MythosTokens.PrimaryGold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedTypeFilter == null,
                        onClick = { selectedTypeFilter = null },
                        label = { Text("All (${fullDeck.size})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MythosTokens.PrimaryGold,
                            selectedLabelColor = Color.Black
                        )
                    )

                    CardType.entries.forEach { type ->
                        val count = fullDeck.count { it.type == type }
                        if (count > 0) {
                            FilterChip(
                                selected = selectedTypeFilter == type,
                                onClick = {
                                    selectedTypeFilter = if (selectedTypeFilter == type) null else type
                                },
                                label = { Text("${type.label} ($count)", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MythosTokens.PrimaryGold,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Card Grid using reusable CardFrame
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(filteredCards, key = { index, card -> "${card.id}_$index" }) { _, card ->
                        CardFrame(
                            card = card,
                            isPlayable = false,
                            canAfford = true,
                            modifier = Modifier
                                .height(180.dp)
                                .testTag("deck_inspect_${card.id}"),
                            onClick = { selectedCardForDetail = card }
                        )
                    }
                }
            } else {
                // HERO PIPELINE & ASSET REGISTRY TAB (Requirement #2, #3, #7)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Canonical Character Identity Header
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MythosTokens.Panel)
                                .border(1.5.dp, MythosTokens.PrimaryGold, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, MythosTokens.PrimaryGold, RoundedCornerShape(10.dp))
                                ) {
                                    Image(
                                        painter = painterResource(id = HerculesIdentity.assets.portrait.resolveResId()),
                                        contentDescription = HerculesIdentity.NAME,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = HerculesIdentity.NAME,
                                            style = MythosTypography.HeroName.copy(fontSize = 18.sp),
                                            color = MythosTokens.PrimaryGold
                                        )
                                        val rarityColor = MythosTokens.getRarityColor(HerculesIdentity.RARITY)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(rarityColor.copy(alpha = 0.2f))
                                                .border(0.5.dp, rarityColor, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = HerculesIdentity.RARITY.label.uppercase(),
                                                style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                                color = rarityColor
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${HerculesIdentity.TITLE} • ${HerculesIdentity.FACTION.displayName}",
                                        style = MythosTypography.HeroTitle.copy(fontSize = 11.sp),
                                        color = MythosTokens.TextSecondary
                                    )

                                    Text(
                                        text = "hero_id: ${HerculesIdentity.HERO_ID}",
                                        style = MythosTypography.ResourceLabel.copy(fontSize = 10.sp),
                                        color = MythosTokens.DivineBlueLight,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )

                                    Text(
                                        text = HerculesIdentity.definition.loreDescription,
                                        style = MythosTypography.CardDescription.copy(fontSize = 10.sp, lineHeight = 13.sp),
                                        color = MythosTokens.TextMuted,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Section Title
                    item {
                        Text(
                            text = "CANONICAL ASSET REGISTRY (HerculesAssets)",
                            style = MythosTypography.ButtonText.copy(fontSize = 12.sp),
                            color = MythosTokens.PrimaryGold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Asset registry items
                    items(HerculesAssets.registry.allAssets, key = { it.assetKey }) { asset ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MythosTokens.PanelElevated)
                                .border(0.5.dp, MythosTokens.PanelBorder, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .border(1.dp, MythosTokens.PanelHighlight, RoundedCornerShape(6.dp))
                                ) {
                                    Image(
                                        painter = painterResource(id = asset.resolveResId()),
                                        contentDescription = asset.assetKey,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = asset.assetKey,
                                            style = MythosTypography.ButtonText.copy(fontSize = 12.sp),
                                            color = MythosTokens.TextPrimary
                                        )

                                        val isProd = asset.status == AssetStatus.PRODUCTION
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (isProd) Color(0x3310B981) else Color(0x33F59E0B)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isProd) Icons.Default.CheckCircle else Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = if (isProd) MythosTokens.Success else MythosTokens.Warning,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = asset.status.label.uppercase(),
                                                style = MythosTypography.RarityLabel.copy(fontSize = 8.5.sp),
                                                color = if (isProd) MythosTokens.Success else MythosTokens.Warning
                                            )
                                        }
                                    }

                                    Text(
                                        text = "assets/${asset.assetPath}",
                                        style = MythosTypography.ResourceLabel.copy(fontSize = 9.sp),
                                        color = MythosTokens.DivineBlueLight,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Inspection Dialog
        selectedCardForDetail?.let { card ->
            CardDetailDialog(
                card = card,
                hero = Hero.createHercules(),
                canPlay = false,
                onPlay = {},
                onDismiss = { selectedCardForDetail = null }
            )
        }
    }
}
