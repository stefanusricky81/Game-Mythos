package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Card
import com.example.ui.theme.MythosTokens
import com.example.ui.theme.MythosTypography

/**
 * Dedicated Placeholder Area for Played Cards (Battlefield Combat Drop Zone).
 *
 * Requirements:
 * - Visually establishes the designated play zone where cards resolve in combat.
 * - Displays active/resolving cards with divine particle glow and entrance animation.
 * - When idle, displays an ornate ancient mythological card slot placeholder.
 * - Provides discard history inspection dialog so players can inspect all played cards.
 */
@Composable
fun PlayedCardsZone(
    activeCard: Card?,
    lastPlayedCard: Card?,
    discardPile: List<Card>,
    modifier: Modifier = Modifier
) {
    var showHistoryDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "played_card_zone_glow")
    val borderPulse by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "zone_pulse"
    )

    Box(
        modifier = modifier
            .testTag("played_cards_placeholder_area")
            .width(132.dp)
            .height(178.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0x331E1729),
                        Color(0x55110E18),
                        Color(0x3309070D)
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    listOf(
                        MythosTokens.PrimaryGold.copy(alpha = borderPulse),
                        MythosTokens.SecondaryGold.copy(alpha = 0.3f),
                        MythosTokens.PanelBorder
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { showHistoryDialog = true },
        contentAlignment = Alignment.Center
    ) {
        if (activeCard != null) {
            // ACTIVE RESOLVING CARD IN THE PLAYED CARDS ZONE
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Tag: Active Play
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(MythosTokens.PrimaryGold.copy(alpha = 0.25f))
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚔ RESOLVING ⚔",
                        style = MythosTypography.RarityLabel.copy(fontSize = 7.5.sp),
                        color = MythosTokens.LightGold
                    )
                }

                // Render Card Frame
                CardFrame(
                    card = activeCard,
                    isPlayable = false,
                    canAfford = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(146.dp)
                        .shadow(10.dp, spotColor = MythosTokens.PrimaryGold)
                )
            }
        } else if (lastPlayedCard != null) {
            // LAST PLAYED CARD RESTING IN THE ZONE
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x44231E2F))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LAST PLAYED",
                        style = MythosTypography.RarityLabel.copy(fontSize = 7.sp),
                        color = MythosTokens.TextMuted
                    )
                    Text(
                        text = "(${discardPile.size})",
                        style = MythosTypography.StatNumber.copy(fontSize = 7.sp),
                        color = MythosTokens.PrimaryGold
                    )
                }

                CardFrame(
                    card = lastPlayedCard,
                    isPlayable = false,
                    canAfford = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(146.dp)
                )
            }
        } else {
            // IDLE PLACEHOLDER STATE (Requirement: Placeholder area for played cards)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Card Silhouette / Runic Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x332B253B))
                        .border(1.dp, MythosTokens.SecondaryGold.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Played Cards Slot",
                        tint = MythosTokens.PrimaryGold.copy(alpha = borderPulse),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "PLAYED CARDS",
                    style = MythosTypography.GameTitle.copy(fontSize = 9.sp),
                    color = MythosTokens.PrimaryGold,
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = "DROP ZONE",
                    style = MythosTypography.HeroTitle.copy(fontSize = 8.sp),
                    color = MythosTokens.TextMuted
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Discard Count Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x441F1A2A))
                        .border(0.5.dp, MythosTokens.PanelBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MythosTokens.DivineBlueLight,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "${discardPile.size} Played",
                            style = MythosTypography.ResourceLabel.copy(fontSize = 8.sp),
                            color = MythosTokens.DivineBlueLight
                        )
                    }
                }
            }
        }
    }

    // PLAYED CARDS HISTORY MODAL DIALOG
    if (showHistoryDialog) {
        Dialog(onDismissRequest = { showHistoryDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, MythosTokens.PrimaryGold, RoundedCornerShape(14.dp)),
                color = MythosTokens.BackgroundSurface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Style,
                                contentDescription = null,
                                tint = MythosTokens.PrimaryGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PLAYED CARDS ARCHIVE",
                                style = MythosTypography.GameTitle.copy(fontSize = 14.sp),
                                color = MythosTokens.PrimaryGold
                            )
                        }

                        Text(
                            text = "${discardPile.size} Total",
                            style = MythosTypography.HeroTitle.copy(fontSize = 11.sp),
                            color = MythosTokens.TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (discardPile.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No cards have been played yet in this battle.\nPlay cards from your hand to resolve effects.",
                                style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                                color = MythosTokens.TextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(185.dp)
                        ) {
                            items(discardPile.reversed()) { card ->
                                CardFrame(
                                    card = card,
                                    isPlayable = false,
                                    canAfford = true,
                                    modifier = Modifier
                                        .width(115.dp)
                                        .fillMaxHeight()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    MythosButton(
                        text = "CLOSE ARCHIVE",
                        onClick = { showHistoryDialog = false },
                        style = MythosButtonStyle.PRIMARY,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
