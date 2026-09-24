package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.MythosConfig
import com.example.R
import com.example.data.CardRarity
import com.example.data.HerculesIdentity
import com.example.monetization.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.ShopTab
import com.example.viewmodel.ShopViewModel
import java.text.NumberFormat
import java.util.Locale

/**
 * Production-Ready MYTHOS Shop & Top-Up Screen.
 * Implements centralized monetization, data-driven catalogs, mock billing,
 * summon altar, direct card purchases, and visual-only cosmetics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShopViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val economyState by viewModel.economyState.collectAsState()
    var showDebugPanel by remember { mutableStateOf(false) }

    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MythosTokens.Background)
    ) {
        // Background Artwork
        Image(
            painter = painterResource(id = R.drawable.img_battlefield_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.18f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // 1. TOP SHOP HEADER (Requirement #4)
            ShopHeader(
                goldBalance = numberFormat.format(economyState.gold),
                gemBalance = numberFormat.format(economyState.mythGems),
                onBackClick = onNavigateBack,
                onHistoryClick = { viewModel.showHistory(true) },
                onToggleDebug = { showDebugPanel = !showDebugPanel }
            )

            // Status / Error Notification Banner
            AnimatedVisibility(
                visible = uiState.statusNotification != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                uiState.statusNotification?.let { msg ->
                    val bannerBg = if (uiState.isErrorNotification) MythosTokens.Damage else MythosTokens.PanelElevated
                    val bannerBorder = if (uiState.isErrorNotification) MythosTokens.Damage else MythosTokens.PrimaryGold

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bannerBg.copy(alpha = 0.9f))
                            .border(1.dp, bannerBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = msg,
                            style = MythosTypography.HeroTitle.copy(fontSize = 12.sp),
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearNotification() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 2. NAVIGATION TABS (Requirement #17 & Polish #5)
            ScrollableTabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = MythosTokens.Panel,
                contentColor = MythosTokens.PrimaryGold,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    if (uiState.selectedTab.ordinal < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab.ordinal]),
                            height = 3.dp,
                            color = MythosTokens.PrimaryGold
                        )
                    }
                },
                divider = {
                    Divider(color = MythosTokens.PanelBorder, thickness = 1.dp)
                }
            ) {
                ShopTab.values().forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = {
                            Text(
                                text = tab.label.uppercase(),
                                style = MythosTypography.RarityLabel.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = if (uiState.selectedTab == tab) FontWeight.Bold else FontWeight.Medium
                                ),
                                letterSpacing = 1.sp,
                                maxLines = 1,
                                softWrap = false,
                                color = if (uiState.selectedTab == tab) MythosTokens.PrimaryGold else MythosTokens.TextMuted
                            )
                        },
                        icon = {
                            val icon = when (tab) {
                                ShopTab.FEATURED -> Icons.Default.AutoAwesome
                                ShopTab.GEMS -> Icons.Default.Diamond
                                ShopTab.CARDS -> Icons.Default.Layers
                                ShopTab.SUMMON -> Icons.Default.Bolt
                                ShopTab.COSMETICS -> Icons.Default.Palette
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier
                            .testTag("shop_tab_${tab.name.lowercase()}")
                            .padding(horizontal = 4.dp)
                    )
                }
            }

            // 3. TAB CONTENT
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (uiState.selectedTab) {
                    ShopTab.FEATURED -> FeaturedTab(
                        bundles = MonetizationCatalog.BUNDLES,
                        ownedBundleIds = economyState.ownedBundleIds,
                        onBuyBundle = { viewModel.buyBundle(it) },
                        onNavigateToGems = { viewModel.selectTab(ShopTab.GEMS) }
                    )
                    ShopTab.GEMS -> GemsTab(
                        products = MonetizationCatalog.GEM_PRODUCTS,
                        onBuyProduct = { viewModel.buyGemProduct(it) }
                    )
                    ShopTab.CARDS -> CardsTab(
                        cards = MonetizationCatalog.getDirectCardOffers(),
                        ownedCardIds = economyState.ownedCardIds,
                        onBuyCard = { viewModel.buyDirectCard(it) }
                    )
                    ShopTab.SUMMON -> SummonTab(
                        pityState = economyState.pityState,
                        gemBalance = economyState.mythGems,
                        onSingleSummon = { viewModel.executeSummon(1) },
                        onTenSummon = { viewModel.executeSummon(10) },
                        onShowRates = { viewModel.showSummonRates(true) },
                        onTopUpGems = { viewModel.selectTab(ShopTab.GEMS) }
                    )
                    ShopTab.COSMETICS -> CosmeticsTab(
                        cosmetics = MonetizationCatalog.COSMETICS,
                        ownedCosmeticIds = economyState.ownedCosmeticIds,
                        gemBalance = economyState.mythGems,
                        onBuyCosmetic = { viewModel.buyCosmetic(it) }
                    )
                }

                // Loading overlay
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MythosTokens.PrimaryGold,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        }

        // 4. DIALOGS & OVERLAYS
        // Bundle Purchase Success Dialog
        uiState.successPurchase?.let { success ->
            PurchaseSuccessDialog(
                successResult = success,
                onDismiss = { viewModel.dismissSuccessDialog() }
            )
        }

        // Gem Purchase Success Dialog
        uiState.gemSuccessProduct?.let { product ->
            GemPurchaseSuccessDialog(
                product = product,
                currentBalance = economyState.mythGems,
                onDismiss = { viewModel.dismissSuccessDialog() }
            )
        }

        // Summon Results Dialog
        uiState.summonResults?.let { pulls ->
            SummonResultDialog(
                results = pulls,
                onDismiss = { viewModel.dismissSummonDialog() }
            )
        }

        // Transaction History Dialog
        if (uiState.showHistoryDialog) {
            PurchaseHistoryDialog(
                history = economyState.purchaseHistory,
                onDismiss = { viewModel.showHistory(false) }
            )
        }

        // Summon Rates Disclosure Modal
        if (uiState.showSummonRatesDialog) {
            SummonRatesDialog(
                onDismiss = { viewModel.showSummonRates(false) }
            )
        }

        // Debug / Cheat Controls (Requirement #36)
        if (showDebugPanel && MythosConfig.DEBUG_BUILD) {
            DebugControlsDialog(
                currentMode = uiState.simulationMode,
                onSetMode = { viewModel.setSimulationMode(it) },
                onAddGems = { viewModel.debugAddGems(1000) },
                onAddGold = { viewModel.debugAddGold(50000) },
                onResetEconomy = { viewModel.debugResetEconomy() },
                onDismiss = { showDebugPanel = false }
            )
        }
    }
}

// -------------------------------------------------------------------------
// SECTION: HEADER
// -------------------------------------------------------------------------

@Composable
private fun ShopHeader(
    goldBalance: String,
    gemBalance: String,
    onBackClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onToggleDebug: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MythosTokens.BackgroundSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Back button & Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("shop_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MythosTokens.PrimaryGold
                    )
                }
                Column {
                    Text(
                        text = "MYTHOS SHOP",
                        style = MythosTypography.GameTitle.copy(fontSize = 17.sp),
                        color = MythosTokens.PrimaryGold
                    )
                    Text(
                        text = "Treasures of Mount Olympus",
                        style = MythosTypography.HeroTitle.copy(fontSize = 9.5.sp),
                        color = MythosTokens.TextMuted
                    )
                }
            }

            // Right: Currencies & Quick Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Gold Balance
                CurrencyPill(
                    symbol = "🪙",
                    amount = goldBalance,
                    color = MythosTokens.PrimaryGold
                )

                // Myth Gem Balance
                CurrencyPill(
                    symbol = "💎",
                    amount = gemBalance,
                    color = MythosTokens.DivineBlueLight
                )

                // History Icon
                IconButton(
                    onClick = onHistoryClick,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("transaction_history_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = "History",
                        tint = MythosTokens.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Debug Button (Only in debug build)
                if (MythosConfig.DEBUG_BUILD) {
                    IconButton(
                        onClick = onToggleDebug,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("debug_controls_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Debug",
                            tint = MythosTokens.MythPowerFlame,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencyPill(symbol: String, amount: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MythosTokens.PanelElevated)
            .border(0.5.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = symbol, fontSize = 11.sp)
        Text(
            text = amount,
            style = MythosTypography.HeroName.copy(fontSize = 11.5.sp),
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

// -------------------------------------------------------------------------
// SECTION: TAB 1 — FEATURED
// -------------------------------------------------------------------------

@Composable
private fun FeaturedTab(
    bundles: List<PremiumBundle>,
    ownedBundleIds: Set<String>,
    onBuyBundle: (PremiumBundle) -> Unit,
    onNavigateToGems: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Hero Featured Banner (Requirement #4 & #12): Hercules Legendary Hero Bundle
        val herculesBundle = bundles.find { it.bundleId == "bundle_legendary_hercules" }
        if (herculesBundle != null) {
            item {
                FeaturedHeroBundleCard(
                    bundle = herculesBundle,
                    onBuyClick = { onBuyBundle(herculesBundle) }
                )
            }
        }

        // 2. Starter Pack (Requirement #11): One-Time Purchase
        val starterPack = bundles.find { it.bundleId == "bundle_starter_pack" }
        if (starterPack != null) {
            val isOwned = ownedBundleIds.contains(starterPack.bundleId)
            item {
                StarterPackBanner(
                    bundle = starterPack,
                    isOwned = isOwned,
                    onBuyClick = { onBuyBundle(starterPack) }
                )
            }
        }

        // 3. Mythic Bundle & Passes
        val otherBundles = bundles.filter {
            it.bundleId != "bundle_legendary_hercules" && it.bundleId != "bundle_starter_pack"
        }

        items(otherBundles) { bundle ->
            val isOwned = bundle.isOneTime && ownedBundleIds.contains(bundle.bundleId)
            StandardBundleCard(
                bundle = bundle,
                isOwned = isOwned,
                onBuyClick = { onBuyBundle(bundle) }
            )
        }
    }
}

@Composable
private fun FeaturedHeroBundleCard(
    bundle: PremiumBundle,
    onBuyClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.5.dp,
                Brush.horizontalGradient(
                    listOf(MythosTokens.PrimaryGold, MythosTokens.MythPowerFlame, MythosTokens.PrimaryGold)
                ),
                RoundedCornerShape(16.dp)
            )
            .testTag("featured_bundle_${bundle.bundleId}"),
        colors = CardDefaults.cardColors(containerColor = MythosTokens.PanelElevated)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Image with Hercules Canonical Production Artwork (Requirement #2 & #3)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Image(
                    painter = painterResource(id = HerculesIdentity.assets.fullBody.resolveResId()),
                    contentDescription = HerculesIdentity.NAME,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Dark gradient overlay for smooth visual transition
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.25f),
                                    Color.Transparent,
                                    MythosTokens.PanelElevated.copy(alpha = 0.95f),
                                    MythosTokens.PanelElevated
                                )
                            )
                        )
                )

                // Top Badge: GUARANTEED LEGENDARY
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(MythosTokens.MythPowerFlame, MythosTokens.PrimaryGold)
                            )
                        )
                        .border(1.dp, MythosTokens.LightGold, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "GUARANTEED LEGENDARY",
                        style = MythosTypography.RarityLabel.copy(fontSize = 9.5.sp),
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Bundle Details Hierarchy (Requirement #3)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = bundle.title,
                    style = MythosTypography.GameTitle.copy(fontSize = 18.sp),
                    color = MythosTokens.PrimaryGold
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = HerculesIdentity.NAME,
                        style = MythosTypography.HeroName.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = HerculesIdentity.TITLE,
                        style = MythosTypography.HeroTitle.copy(fontSize = 12.sp),
                        color = MythosTokens.TextSecondary
                    )
                }

                // Reward Contents Pills (Hercules x1, 1,000 Gems, 50 Hercules Shards, 30K Gold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MiniContentTag("Hercules ×1", MythosTokens.PrimaryGold)
                    MiniContentTag("1,000 Gems", MythosTokens.DivineBlueLight)
                    MiniContentTag("50 Hercules Shards", MythosTokens.Buff)
                    MiniContentTag("30K Gold", MythosTokens.LightGold)
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PREMIUM HERO PACK",
                            style = MythosTypography.RarityLabel.copy(fontSize = 8.5.sp),
                            color = MythosTokens.TextMuted
                        )
                        Text(
                            text = bundle.priceDisplay,
                            style = MythosTypography.GameTitle.copy(fontSize = 19.sp),
                            color = MythosTokens.LightGold
                        )
                    }

                    MythosButton(
                        text = "GET BUNDLE",
                        onClick = onBuyClick,
                        style = MythosButtonStyle.PRIMARY,
                        icon = Icons.Default.ShoppingCart,
                        testTag = "buy_featured_bundle",
                        modifier = Modifier
                            .width(145.dp)
                            .height(44.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StarterPackBanner(
    bundle: PremiumBundle,
    isOwned: Boolean,
    onBuyClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (isOwned) MythosTokens.PanelBorder else MythosTokens.PrimaryGold,
                RoundedCornerShape(12.dp)
            )
            .testTag("starter_pack_banner"),
        colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = bundle.title,
                        style = MythosTypography.GameTitle.copy(fontSize = 15.sp),
                        color = MythosTokens.PrimaryGold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MythosTokens.Buff.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isOwned) "OWNED" else "ONE TIME",
                            style = MythosTypography.RarityLabel.copy(fontSize = 8.sp),
                            color = if (isOwned) MythosTokens.Success else MythosTokens.Buff
                        )
                    }
                }

                Text(
                    text = "300 Gems • 1 Rare • 1 Epic • 10k Gold • Frame & Avatar",
                    style = MythosTypography.CardDescription.copy(fontSize = 10.5.sp),
                    color = MythosTokens.TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Text(
                    text = bundle.priceDisplay,
                    style = MythosTypography.HeroName.copy(fontSize = 14.sp),
                    color = MythosTokens.LightGold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isOwned) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MythosTokens.Success.copy(alpha = 0.15f))
                        .border(1.dp, MythosTokens.Success, RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MythosTokens.Success,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "OWNED",
                        style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                        color = MythosTokens.Success
                    )
                }
            } else {
                MythosButton(
                    text = "CLAIM",
                    onClick = onBuyClick,
                    style = MythosButtonStyle.PRIMARY,
                    testTag = "buy_starter_pack_btn",
                    modifier = Modifier
                        .width(96.dp)
                        .height(38.dp)
                )
            }
        }
    }
}

@Composable
private fun StandardBundleCard(
    bundle: PremiumBundle,
    isOwned: Boolean,
    onBuyClick: () -> Unit
) {
    val rarityColor = MythosTokens.getRarityColor(bundle.rarity)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, rarityColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .testTag("bundle_${bundle.bundleId}"),
        colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = bundle.title,
                        style = MythosTypography.GameTitle.copy(fontSize = 15.sp),
                        color = MythosTokens.PrimaryGold
                    )
                    Text(
                        text = bundle.subtitle,
                        style = MythosTypography.HeroTitle.copy(fontSize = 11.sp),
                        color = MythosTokens.TextSecondary
                    )
                }

                bundle.badgeText?.let { badge ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(rarityColor.copy(alpha = 0.2f))
                            .border(0.5.dp, rarityColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            style = MythosTypography.RarityLabel.copy(fontSize = 8.5.sp),
                            color = rarityColor
                        )
                    }
                }
            }

            Text(
                text = bundle.description,
                style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                color = MythosTokens.TextMuted
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MythosTokens.BackgroundSurface)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = bundle.priceDisplay,
                    style = MythosTypography.HeroName.copy(fontSize = 14.sp),
                    color = MythosTokens.LightGold
                )

                if (isOwned) {
                    Text(
                        text = "OWNED",
                        style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                        color = MythosTokens.Success
                    )
                } else {
                    MythosButton(
                        text = "UNLOCK",
                        onClick = onBuyClick,
                        style = MythosButtonStyle.SECONDARY,
                        modifier = Modifier
                            .width(110.dp)
                            .height(36.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniContentTag(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.5.dp, color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MythosTypography.RarityLabel.copy(fontSize = 8.5.sp),
            color = color
        )
    }
}

// -------------------------------------------------------------------------
// SECTION: TAB 2 — GEMS (Requirement #5)
// -------------------------------------------------------------------------

@Composable
private fun GemsTab(
    products: List<GemProduct>,
    onBuyProduct: (GemProduct) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(products) { product ->
            GemProductCard(
                product = product,
                onBuyClick = { onBuyProduct(product) }
            )
        }
    }
}

// -------------------------------------------------------------------------
// SECTION: TAB 3 — CARDS (Requirement #6 & #7)
// -------------------------------------------------------------------------

@Composable
private fun CardsTab(
    cards: List<com.example.data.Card>,
    ownedCardIds: Set<String>,
    onBuyCard: (com.example.data.Card) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Base Level Disclosure Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MythosTokens.PrimaryGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = MythosTokens.PanelElevated)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MythosTokens.PrimaryGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "DIRECT PURCHASES ARE BASE LEVEL ONLY — Upgrades require gameplay progression, Gold, and Card Shards.",
                        style = MythosTypography.HeroTitle.copy(fontSize = 10.sp),
                        color = MythosTokens.TextSecondary
                    )
                }
            }
        }

        items(cards) { card ->
            val isOwned = ownedCardIds.contains(card.id)
            PremiumCardOffer(
                card = card,
                isOwned = isOwned,
                onBuyClick = { onBuyCard(card) }
            )
        }
    }
}

// -------------------------------------------------------------------------
// SECTION: TAB 4 — SUMMON (Requirement #9 & #10)
// -------------------------------------------------------------------------

@Composable
private fun SummonTab(
    pityState: SummonPityState,
    gemBalance: Int,
    onSingleSummon: () -> Unit,
    onTenSummon: () -> Unit,
    onShowRates: () -> Unit,
    onTopUpGems: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Altar Chamber Showcase
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.5.dp, MythosTokens.PrimaryGold, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MythosTokens.PanelElevated)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Altar Icon & Visual Aura
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    MythosTokens.PrimaryGold.copy(alpha = 0.35f),
                                    MythosTokens.DivineBlue.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(1.dp, MythosTokens.PrimaryGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Summon Altar",
                        tint = MythosTokens.PrimaryGold,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Text(
                    text = "ALTAR OF OLYMPUS",
                    style = MythosTypography.GameTitle.copy(fontSize = 18.sp),
                    color = MythosTokens.PrimaryGold,
                    letterSpacing = 2.sp
                )

                Text(
                    text = "Invoke the favor of ancient gods to summon celestial cards.\nDuplicates automatically convert into Card Shards.",
                    style = MythosTypography.CardDescription.copy(fontSize = 11.5.sp),
                    color = MythosTokens.TextSecondary,
                    textAlign = TextAlign.Center
                )

                TextButton(onClick = onShowRates) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = MythosTokens.DivineBlueLight,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "View Summon Probabilities",
                        style = MythosTypography.HeroTitle.copy(fontSize = 11.sp),
                        color = MythosTokens.DivineBlueLight
                    )
                }
            }
        }

        // Pity Counter Panel (Requirement #10)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MythosTokens.PanelBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PITY GUARANTEE PROGRESS",
                        style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                        color = MythosTokens.PrimaryGold
                    )
                    Text(
                        text = "Total Summons: ${pityState.totalSummons}",
                        style = MythosTypography.HeroTitle.copy(fontSize = 9.sp),
                        color = MythosTokens.TextMuted
                    )
                }

                PityProgressBar(
                    title = "Epic Pity (Min. Epic Guarantee)",
                    current = pityState.epicPity,
                    threshold = SummonPityState.EPIC_THRESHOLD,
                    color = MythosTokens.getRarityColor(CardRarity.EPIC)
                )

                PityProgressBar(
                    title = "Legendary Pity (Guaranteed Legendary)",
                    current = pityState.legendaryPity,
                    threshold = SummonPityState.LEGENDARY_THRESHOLD,
                    color = MythosTokens.getRarityColor(CardRarity.LEGENDARY)
                )

                PityProgressBar(
                    title = "Mythic Pity (Guaranteed Mythic)",
                    current = pityState.mythicPity,
                    threshold = SummonPityState.MYTHIC_THRESHOLD,
                    color = MythosTokens.getRarityColor(CardRarity.MYTHIC)
                )
            }
        }

        // Summon Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1x Summon
            MythosButton(
                text = "SUMMON ×1",
                subtitle = "100 Myth Gems",
                onClick = onSingleSummon,
                style = MythosButtonStyle.SECONDARY,
                icon = Icons.Default.Bolt,
                testTag = "summon_1x_btn",
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
            )

            // 10x Summon
            MythosButton(
                text = "SUMMON ×10",
                subtitle = "900 Myth Gems (10% OFF)",
                onClick = onTenSummon,
                style = MythosButtonStyle.PRIMARY,
                icon = Icons.Default.AutoAwesome,
                testTag = "summon_10x_btn",
                modifier = Modifier
                    .weight(1.2f)
                    .height(54.dp)
            )
        }

        if (gemBalance < 100) {
            TextButton(onClick = onTopUpGems) {
                Text(
                    text = "Low on Myth Gems? Tap to Visit Top-Up Vault",
                    style = MythosTypography.HeroTitle.copy(fontSize = 11.sp),
                    color = MythosTokens.PrimaryGold
                )
            }
        }
    }
}

@Composable
private fun PityProgressBar(
    title: String,
    current: Int,
    threshold: Int,
    color: Color
) {
    val progress = (current.toFloat() / threshold).coerceIn(0f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MythosTypography.HeroTitle.copy(fontSize = 10.sp),
                color = MythosTokens.TextSecondary
            )
            Text(
                text = "$current / $threshold",
                style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                color = color
            )
        }
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = MythosTokens.BackgroundSurface
        )
    }
}

// -------------------------------------------------------------------------
// SECTION: TAB 5 — COSMETICS (Requirement #16)
// -------------------------------------------------------------------------

@Composable
private fun CosmeticsTab(
    cosmetics: List<CosmeticItem>,
    ownedCosmeticIds: Set<String>,
    gemBalance: Int,
    onBuyCosmetic: (CosmeticItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Visual-Only Notice
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MythosTokens.PanelBorder, RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = MythosTokens.PanelElevated)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MythosTokens.DivineBlueLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Cosmetics are purely aesthetic and visual. They do not alter combat statistics, hero attack, or health values.",
                        style = MythosTypography.HeroTitle.copy(fontSize = 10.sp),
                        color = MythosTokens.TextSecondary
                    )
                }
            }
        }

        items(cosmetics) { cosmetic ->
            val isOwned = ownedCosmeticIds.contains(cosmetic.id)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MythosTokens.PanelBorder, RoundedCornerShape(12.dp))
                    .testTag("cosmetic_${cosmetic.id}"),
                colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = cosmetic.name,
                                style = MythosTypography.CardName.copy(fontSize = 14.sp),
                                color = MythosTokens.PrimaryGold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MythosTokens.PanelElevated)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = cosmetic.type.name,
                                    style = MythosTypography.RarityLabel.copy(fontSize = 8.sp),
                                    color = MythosTokens.DivineBlueLight
                                )
                            }
                        }

                        Text(
                            text = cosmetic.description,
                            style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                            color = MythosTokens.TextMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Text(
                            text = cosmetic.priceDisplay,
                            style = MythosTypography.HeroName.copy(fontSize = 13.sp),
                            color = MythosTokens.LightGold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (isOwned) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MythosTokens.Success.copy(alpha = 0.2f))
                                .border(1.dp, MythosTokens.Success, RoundedCornerShape(6.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "UNLOCKED",
                                style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                color = MythosTokens.Success
                            )
                        }
                    } else {
                        MythosButton(
                            text = "UNLOCK",
                            onClick = { onBuyCosmetic(cosmetic) },
                            style = MythosButtonStyle.PRIMARY,
                            modifier = Modifier
                                .width(100.dp)
                                .height(38.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// SECTION: MODALS & DISCLOSURES
// -------------------------------------------------------------------------

@Composable
private fun SummonRatesDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MythosTokens.PrimaryGold, RoundedCornerShape(16.dp)),
            color = MythosTokens.Panel
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "SUMMON PROBABILITIES",
                    style = MythosTypography.GameTitle.copy(fontSize = 16.sp),
                    color = MythosTokens.PrimaryGold
                )

                Text(
                    text = "Altar of Olympus Drop Rates per Draw",
                    style = MythosTypography.HeroTitle.copy(fontSize = 11.sp),
                    color = MythosTokens.TextSecondary
                )

                Divider(color = MythosTokens.PanelBorder)

                SummonRates.RATE_ENTRIES.forEach { (rarity, rate) ->
                    val color = MythosTokens.getRarityColor(rarity)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MythosTokens.BackgroundSurface)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = rarity.label.uppercase(),
                            style = MythosTypography.RarityLabel.copy(fontSize = 11.sp),
                            color = color
                        )
                        Text(
                            text = rate,
                            style = MythosTypography.HeroName.copy(fontSize = 12.sp),
                            color = MythosTokens.TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Duplicate Shard Conversion:\nCommon: 5 • Uncommon: 8 • Rare: 12 • Epic: 16 • Legendary: 20 • Mythic: 30",
                    style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                    color = MythosTokens.TextMuted,
                    textAlign = TextAlign.Center
                )

                MythosButton(
                    text = "CLOSE",
                    onClick = onDismiss,
                    style = MythosButtonStyle.PRIMARY,
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                )
            }
        }
    }
}

@Composable
private fun DebugControlsDialog(
    currentMode: MockBillingResult,
    onSetMode: (MockBillingResult) -> Unit,
    onAddGems: () -> Unit,
    onAddGold: () -> Unit,
    onResetEconomy: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MythosTokens.MythPowerFlame, RoundedCornerShape(16.dp))
                .testTag("debug_controls_dialog"),
            color = MythosTokens.Panel
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "DEV / MONETIZATION CONTROLS",
                    style = MythosTypography.GameTitle.copy(fontSize = 15.sp),
                    color = MythosTokens.MythPowerFlame
                )
                Text(
                    text = "Simulate payment outcomes and adjust sandbox balances.",
                    style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                    color = MythosTokens.TextMuted
                )

                Divider(color = MythosTokens.PanelBorder)

                Text(
                    text = "Mock Payment Outcome:",
                    style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                    color = MythosTokens.PrimaryGold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MockBillingResult.values().forEach { mode ->
                        val isSelected = currentMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) MythosTokens.MythPowerFlame
                                    else MythosTokens.PanelElevated
                                )
                                .clickable { onSetMode(mode) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.name.take(4),
                                style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                color = if (isSelected) Color.Black else MythosTokens.TextSecondary
                            )
                        }
                    }
                }

                Divider(color = MythosTokens.PanelBorder)

                Text(
                    text = "Sandbox Economy Cheats:",
                    style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                    color = MythosTokens.PrimaryGold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MythosButton(
                        text = "+1,000 Gems",
                        onClick = onAddGems,
                        style = MythosButtonStyle.SECONDARY,
                        modifier = Modifier.weight(1f).height(38.dp)
                    )
                    MythosButton(
                        text = "+50k Gold",
                        onClick = onAddGold,
                        style = MythosButtonStyle.SECONDARY,
                        modifier = Modifier.weight(1f).height(38.dp)
                    )
                }

                MythosButton(
                    text = "RESET ALL PURCHASES & ECONOMY",
                    onClick = onResetEconomy,
                    style = MythosButtonStyle.DANGER,
                    modifier = Modifier.fillMaxWidth().height(38.dp)
                )

                MythosButton(
                    text = "DONE",
                    onClick = onDismiss,
                    style = MythosButtonStyle.PRIMARY,
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                )
            }
        }
    }
}
