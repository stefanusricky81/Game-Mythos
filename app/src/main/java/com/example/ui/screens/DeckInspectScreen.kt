package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
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
                // Info Summary
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MythosTokens.Panel)
                        .border(1.dp, MythosTokens.SecondaryGold.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MythosTokens.PrimaryGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Unified CardFrame system demonstrating all 6 rarity tiers and independent artwork container.",
                            style = MythosTypography.CardDescription.copy(fontSize = 10.sp, lineHeight = 13.sp),
                            color = MythosTokens.TextSecondary
                        )
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
