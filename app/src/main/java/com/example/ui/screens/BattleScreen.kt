package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MythosConfig
import com.example.R
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.BattleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BattleScreen(
    viewModel: BattleViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Screen Shake effect
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(uiState.screenShakeTrigger) {
        if (uiState.screenShakeTrigger > 0) {
            shakeOffset.snapTo(12f)
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioHighBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .offset(x = shakeOffset.value.dp)
            .background(MythosTokens.Background)
    ) {
        // 1. Mythological Battlefield Background (Ancient architecture & dark dramatic contrast)
        Image(
            painter = painterResource(id = R.drawable.img_battlefield_bg),
            contentDescription = "Mythological Arena",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.35f
        )

        // Vignette gradient for dark contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.70f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // 2. Main Game Area Column (Safe Area Handled)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP SECTION: HEADER & ENEMY HERO STATUS
            Column(modifier = Modifier.fillMaxWidth()) {
                // Battle header row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("battle_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MythosTokens.PrimaryGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Turn Announcement Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (uiState.currentTurn == BattleTurn.PLAYER_TURN)
                                    MythosTokens.SecondaryGold.copy(alpha = 0.35f)
                                else
                                    Color(0x556B1119)
                            )
                            .border(
                                1.dp,
                                if (uiState.currentTurn == BattleTurn.PLAYER_TURN)
                                    MythosTokens.PrimaryGold
                                else
                                    MythosTokens.Damage,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (uiState.currentTurn == BattleTurn.PLAYER_TURN)
                                "TURN ${uiState.stats.turnsCount} • YOUR TURN"
                            else
                                "TURN ${uiState.stats.turnsCount} • ARES ACTING",
                            style = MythosTypography.RarityLabel.copy(fontSize = 10.5.sp),
                            color = if (uiState.currentTurn == BattleTurn.PLAYER_TURN)
                                MythosTokens.LightGold
                            else
                                Color(0xFFFFB3B3)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        // Section 18: DEV DEBUG BUTTON shown ONLY when DEBUG_BUILD is true
                        if (MythosConfig.DEBUG_BUILD) {
                            IconButton(
                                onClick = { viewModel.toggleDebugPanel() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("debug_panel_toggle_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = "Debug Panel",
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        // Restart Battle button
                        IconButton(
                            onClick = { viewModel.startNewBattle() },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("restart_battle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Restart",
                                tint = MythosTokens.PrimaryGold,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }

                // Reusable Enemy Hero Status Panel
                HeroStatusPanel(
                    hero = uiState.enemyHero,
                    isPlayer = false,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // CENTER BATTLEFIELD ZONE: Visual Hierarchy with Canonical Combat Arena & Played Cards Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                // Production Battlefield Hero Representation (Requirement #4B & #10)
                BattlefieldCombatArena(
                    playerHero = uiState.playerHero,
                    enemyHero = uiState.enemyHero
                )

                // Overlay Row: Played Cards Placeholder Area & Combat Event Ticker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                        .align(Alignment.Center),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Placeholder area for played cards (Requirement: placeholder area for played cards)
                    PlayedCardsZone(
                        activeCard = uiState.activePlayingCard,
                        lastPlayedCard = uiState.playerDiscardPile.lastOrNull(),
                        discardPile = uiState.playerDiscardPile
                    )

                    // Compact sequential Combat Event Banner & Combat Log Ticker
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CompactCombatEventBanner(
                            notification = uiState.activeNotification
                        )

                        CombatLogTicker(logs = uiState.combatLogs)
                    }
                }

                // Floating combat numbers (anchored close to target hero)
                FloatingNumbersOverlay(floatingTexts = uiState.floatingTexts)
            }

            // BOTTOM PLAYER SECTION: HERCULES METADATA, HEALTH, ABILITY COOLDOWNS, RESOURCES, HAND
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // Hercules Status Panel using Character Metadata and Detailed Health
                HerculesStatusPanel(
                    hero = uiState.playerHero
                )

                // Hercules Ability Cooldowns HUD (Requirement: ability cooldowns)
                HeroAbilitiesPanel(
                    hero = uiState.playerHero,
                    currentEnergy = uiState.playerEnergy,
                    currentMythPower = uiState.playerMythPower,
                    maxMythPower = uiState.maxMythPower,
                    isPlayerTurn = uiState.currentTurn == BattleTurn.PLAYER_TURN,
                    isExecutingTurn = uiState.isExecutingTurn,
                    onHeroAttackClick = { viewModel.performHeroAttack() },
                    onUltimateClick = { viewModel.activateTwelveLaborsUltimate() }
                )

                // Composite Resource Meters (Reusable EnergyMeter + Reusable MythPowerMeter + Reusable MythosButton)
                ResourceMeters(
                    currentEnergy = uiState.playerEnergy,
                    maxEnergy = uiState.maxPlayerEnergy,
                    currentMythPower = uiState.playerMythPower,
                    maxMythPower = uiState.maxMythPower,
                    isPlayerTurn = uiState.currentTurn == BattleTurn.PLAYER_TURN,
                    isExecutingTurn = uiState.isExecutingTurn,
                    isEnergyHighlighted = uiState.isEnergyHighlighted,
                    mythPowerGainNotification = uiState.mythPowerGainNotification,
                    compactEnergyWarning = uiState.compactEnergyWarning,
                    onHeroAttackClick = { viewModel.performHeroAttack() },
                    onUltimateClick = { viewModel.activateTwelveLaborsUltimate() },
                    onEndTurnClick = { viewModel.endTurn() }
                )

                // Player Hand Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CARDS IN HAND (${uiState.playerHand.size})",
                        style = MythosTypography.ResourceLabel.copy(fontSize = 9.5.sp),
                        color = MythosTokens.PrimaryGold
                    )

                    Text(
                        text = "Deck: ${uiState.playerDrawPile.size} | Discard: ${uiState.playerDiscardPile.size}",
                        style = MythosTypography.HeroTitle.copy(fontSize = 9.5.sp)
                    )
                }

                // Player Hand: Reusable CardFrame components
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(152.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.playerHand.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MythosTokens.Panel.copy(alpha = 0.5f))
                                .border(0.5.dp, MythosTokens.PanelBorder, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Hand is empty. End turn to draw cards.",
                                style = MythosTypography.CardDescription,
                                color = MythosTokens.TextMuted
                            )
                        }
                    } else {
                        uiState.playerHand.forEach { card ->
                            val canAfford = card.cost <= uiState.playerEnergy
                            val isPlayable = canAfford &&
                                    uiState.currentTurn == BattleTurn.PLAYER_TURN &&
                                    !uiState.isExecutingTurn

                            CardFrame(
                                card = card,
                                isPlayable = isPlayable,
                                canAfford = canAfford,
                                isShaking = uiState.shakingCardId == card.id,
                                modifier = Modifier
                                    .width(118.dp)
                                    .fillMaxHeight(),
                                onClick = {
                                    viewModel.playCard(card)
                                }
                            )
                        }
                    }
                }
            }
        }

        // 3. OVERLAYS & MODALS

        // Twelve Labors Cinematic Overlay
        if (uiState.isUltimateCinematicActive) {
            TwelveLaborsCinematic()
        }

        // Last Stand Alert Banner
        if (uiState.isLastStandBannerActive) {
            LastStandBanner(onDismiss = { viewModel.dismissLastStandBanner() })
        }

        // Card Detail Dialog
        uiState.inspectedCard?.let { card ->
            CardDetailDialog(
                card = card,
                hero = uiState.playerHero,
                canPlay = card.cost <= uiState.playerEnergy &&
                        uiState.currentTurn == BattleTurn.PLAYER_TURN &&
                        !uiState.isExecutingTurn,
                onPlay = { viewModel.playCard(card) },
                onDismiss = { viewModel.inspectCard(null) }
            )
        }

        // Debug Panel Modal
        if (uiState.isDebugPanelOpen) {
            DebugPanelDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.toggleDebugPanel() }
            )
        }

        // Game Over Dialog (Victory / Defeat)
        if (uiState.currentTurn == BattleTurn.VICTORY || uiState.currentTurn == BattleTurn.DEFEAT) {
            GameOverDialog(
                isVictory = uiState.currentTurn == BattleTurn.VICTORY,
                stats = uiState.stats,
                rewards = uiState.rewards,
                onBattleAgain = { viewModel.startNewBattle() },
                onGoHome = onNavigateBack
            )
        }
    }
}
