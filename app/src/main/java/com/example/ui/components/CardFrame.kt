package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Card
import com.example.data.CardRarity
import com.example.ui.theme.MythosTokens
import com.example.ui.theme.MythosTypography

/**
 * ONE Reusable CardFrame Component supporting all 6 Rarity Tiers:
 * 1. COMMON
 * 2. UNCOMMON
 * 3. RARE
 * 4. EPIC
 * 5. LEGENDARY
 * 6. MYTHIC
 *
 * Strict layout hierarchy:
 * - TOP: Cost Gem & Card Artwork
 * - MIDDLE: Card Name & Type
 * - BOTTOM: Effect Description, Rarity Indicator, PLAY Interaction
 */
@Composable
fun CardFrame(
    card: Card,
    isPlayable: Boolean,
    modifier: Modifier = Modifier,
    canAfford: Boolean = true,
    isShaking: Boolean = false,
    onClick: () -> Unit = {}
) {
    val rarityColor = MythosTokens.getRarityColor(card.rarity)
    val typeColor = MythosTokens.getCardTypeColor(card.type)

    // Shaking animation on invalid action
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(isShaking) {
        if (isShaking) {
            shakeOffset.snapTo(6f)
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioHighBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    // Subtle rarity pulse & glow
    val infiniteTransition = rememberInfiniteTransition(label = "frame_glow")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    val borderBrush = when {
        isShaking -> Brush.linearGradient(listOf(MythosTokens.Damage, Color(0xFFB91C1C)))
        else -> MythosTokens.getRarityBorderBrush(card.rarity, isPlayable, shimmerAlpha)
    }

    val cardAlpha = if (isPlayable) 1f else 0.72f

    Card(
        modifier = modifier
            .testTag("card_${card.id}")
            .offset(x = shakeOffset.value.dp)
            .alpha(cardAlpha)
            .shadow(
                elevation = when {
                    card.rarity == CardRarity.MYTHIC && isPlayable -> 10.dp
                    card.rarity == CardRarity.LEGENDARY && isPlayable -> 8.dp
                    isPlayable -> 5.dp
                    else -> 1.dp
                },
                shape = RoundedCornerShape(10.dp),
                ambientColor = if (isPlayable) rarityColor else Color.Black,
                spotColor = if (isPlayable) rarityColor else Color.Transparent
            )
            .border(
                BorderStroke(
                    width = if (isPlayable || isShaking) 1.5.dp else 1.dp,
                    brush = borderBrush
                ),
                shape = RoundedCornerShape(10.dp)
            )
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlayable) MythosTokens.Panel else MythosTokens.BackgroundSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ==========================================
            // TOP SECTION: Cost Gem & Artwork Container
            // ==========================================
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Cost Gem (Cyan if affordable, crimson/amber if insufficient)
                    Box(
                        modifier = Modifier
                            .size(19.dp)
                            .clip(CircleShape)
                            .background(
                                if (canAfford)
                                    Brush.radialGradient(listOf(MythosTokens.Energy, MythosTokens.EnergyDark))
                                else
                                    Brush.radialGradient(listOf(Color(0xFF991B1B), Color(0xFF450A0A)))
                            )
                            .border(
                                0.8.dp,
                                if (canAfford) Color.White.copy(alpha = 0.9f) else MythosTokens.Damage,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = card.cost.toString(),
                            style = MythosTypography.ResourceNumber.copy(fontSize = 11.sp),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(3.dp))

                    // Rarity Pill Indicator (Top-right)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(rarityColor.copy(alpha = 0.25f))
                            .border(0.5.dp, rarityColor.copy(alpha = 0.7f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 0.5.dp)
                    ) {
                        Text(
                            text = if (card.level > 1) "${card.rarity.name.take(3)} • L${card.level}" else card.rarity.name.take(3),
                            style = MythosTypography.RarityLabel,
                            color = rarityColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Dedicated, Independent Artwork Area
                CardArtwork(
                    card = card,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                )
            }

            // ==========================================
            // MIDDLE SECTION: Card Name & Type
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = card.name,
                    style = MythosTypography.CardName,
                    color = if (isPlayable) MythosTokens.TextPrimary else MythosTokens.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(typeColor.copy(alpha = 0.2f))
                        .padding(horizontal = 3.dp, vertical = 0.5.dp)
                ) {
                    Text(
                        text = card.type.label.uppercase(),
                        style = MythosTypography.CardTypeLabel,
                        color = typeColor
                    )
                }
            }

            // ==========================================
            // BOTTOM SECTION: Effect & Play Interaction
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF09080F))
                    .border(0.5.dp, Color(0xFF221F2D), RoundedCornerShape(4.dp))
                    .padding(horizontal = 3.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = card.effectDescription,
                    style = MythosTypography.CardDescription,
                    color = if (isPlayable) MythosTokens.TextSecondary else MythosTokens.TextMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Play Interaction Status Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = card.faction.uppercase(),
                    style = MythosTypography.RarityLabel.copy(fontSize = 6.5.sp),
                    color = MythosTokens.TextDisabled
                )

                if (isPlayable) {
                    Text(
                        text = "PLAY",
                        style = MythosTypography.ButtonText.copy(fontSize = 8.sp),
                        color = MythosTokens.PrimaryGold
                    )
                } else if (!canAfford) {
                    Text(
                        text = "NEED ${card.cost}E",
                        style = MythosTypography.ButtonText.copy(fontSize = 7.sp),
                        color = MythosTokens.Damage
                    )
                }
            }
        }
    }
}
