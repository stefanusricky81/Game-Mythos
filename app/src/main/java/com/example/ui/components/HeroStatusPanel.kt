package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.Hero
import com.example.ui.theme.MythosTokens
import com.example.ui.theme.MythosTypography

/**
 * Reusable HeroStatusPanel Component.
 * Supports any character in MYTHOS: Hercules, Ares, Athena, Zeus, Hades, etc.
 *
 * Displays:
 * - Portrait
 * - Name & Title
 * - Attack & Defense Stats
 * - HP Bar with exact values
 * - Shield Bar (when active)
 * - Status Effects (Buffs, Debuffs, Poison, Relics)
 */
@Composable
fun HeroStatusPanel(
    hero: Hero,
    isPlayer: Boolean,
    modifier: Modifier = Modifier
) {
    val hpFraction = hero.hpPercentage
    val hpBarColor by animateColorAsState(
        targetValue = when {
            hpFraction > 0.6f -> MythosTokens.Success
            hpFraction > 0.3f -> MythosTokens.Warning
            else -> MythosTokens.Damage
        },
        label = "hero_hp_color"
    )

    // Pulsing aura when hero is in Last Stand or critical state
    val infiniteTransition = rememberInfiniteTransition(label = "hero_status_pulse")
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hero_aura"
    )

    val frameBorder = if (hero.isLastStandActive) {
        BorderStroke(2.dp, MythosTokens.PrimaryGold.copy(alpha = auraAlpha))
    } else {
        BorderStroke(1.dp, if (isPlayer) MythosTokens.SecondaryGold else Color(0xFF6B1119))
    }

    Surface(
        modifier = modifier
            .testTag(if (isPlayer) "player_hero_status" else "enemy_hero_status")
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(frameBorder, RoundedCornerShape(12.dp)),
        color = Color(0xDD121019),
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hero Portrait Container
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
                    .border(
                        BorderStroke(
                            width = if (hero.isLastStandActive) 2.dp else 1.dp,
                            color = if (hero.isLastStandActive) MythosTokens.PrimaryGold else MythosTokens.MetallicBronze
                        ),
                        RoundedCornerShape(8.dp)
                    )
            ) {
                Image(
                    painter = painterResource(id = hero.getEffectivePortraitResId()),
                    contentDescription = hero.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                if (hero.isLastStandActive) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color(0xE6E63946))
                            .padding(vertical = 1.dp)
                    ) {
                        Text(
                            text = "LAST STAND",
                            style = MythosTypography.RarityLabel.copy(fontSize = 6.5.sp),
                            color = MythosTokens.LightGold,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Details and Stats Column
            Column(modifier = Modifier.weight(1f)) {
                // Name and Title Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = hero.name,
                            style = MythosTypography.HeroName,
                            color = if (isPlayer) MythosTokens.PrimaryGold else Color(0xFFFF6B6B)
                        )
                        if (isPlayer) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MythosTokens.PrimaryGold.copy(alpha = 0.2f))
                                    .border(0.5.dp, MythosTokens.PrimaryGold, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "LV ${hero.level}",
                                    style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                    color = MythosTokens.PrimaryGold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "• ${hero.title.take(18)}",
                            style = MythosTypography.HeroTitle,
                            maxLines = 1
                        )
                    }

                    // Stat Badges (Attack & Defense)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Attack badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (hero.isLastStandActive || hero.attackBuffPercent > 0)
                                        Color(0x44EF4444)
                                    else
                                        Color(0x332A2438)
                                )
                                .border(0.5.dp, MythosTokens.Damage, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "⚔ ${hero.effectiveAttack}",
                                style = MythosTypography.StatNumber,
                                color = if (hero.isLastStandActive) MythosTokens.PrimaryGold else Color(0xFFFF8A8A)
                            )
                        }

                        // Defense badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x332A2438))
                                .border(0.5.dp, MythosTokens.DivineBlue, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "🛡 ${hero.baseDefense}",
                                style = MythosTypography.StatNumber,
                                color = MythosTokens.Shield
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // HP Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(13.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF231F2E))
                        .border(0.5.dp, MythosTokens.PanelBorder, RoundedCornerShape(6.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = hpFraction)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(hpBarColor.copy(alpha = 0.85f), hpBarColor)
                                )
                            )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HP",
                            style = MythosTypography.StatNumber.copy(fontSize = 7.5.sp),
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "${hero.currentHp} / ${hero.maxHp}",
                            style = MythosTypography.StatNumber.copy(fontSize = 8.5.sp),
                            color = Color.White
                        )
                    }
                }

                // Shield Bar (Active)
                if (hero.currentShield > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(9.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1E293B))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(
                                    fraction = (hero.currentShield.toFloat() / 5000f).coerceIn(0.1f, 1f)
                                )
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(MythosTokens.DivineBlue, MythosTokens.DivineBlueDark)
                                    )
                                )
                        )
                        Text(
                            text = "SHIELD +${hero.currentShield}",
                            style = MythosTypography.StatNumber.copy(fontSize = 6.5.sp),
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 4.dp)
                        )
                    }
                }

                // Active Buffs / Debuffs / Status Effects Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (hero.isLastStandActive) {
                        HeroStatusChip(label = "+25% ATK", color = MythosTokens.PrimaryGold)
                    }
                    if (hero.attackBuffPercent > 0) {
                        HeroStatusChip(label = "+${hero.attackBuffPercent}% (${hero.attackBuffTurns}t)", color = MythosTokens.Buff)
                    }
                    if (hero.damageReductionPercent > 0) {
                        HeroStatusChip(label = "-${hero.damageReductionPercent}% DMG", color = MythosTokens.DivineBlue)
                    }
                    if (hero.hasPhalanxGuard) {
                        HeroStatusChip(label = "Phalanx Guard", color = MythosTokens.Success)
                    }
                    if (hero.hasThunderstoneRelic) {
                        HeroStatusChip(label = "Thunderstone", color = Color(0xFFA855F7))
                    }
                    if (hero.poisonTurnsRemaining > 0) {
                        HeroStatusChip(label = "POISON ×${hero.poisonTurnsRemaining}", color = MythosTokens.Poison)
                    }
                }
            }
        }
    }
}

@Composable
fun HeroStatusChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(color.copy(alpha = 0.2f))
            .border(0.5.dp, color, RoundedCornerShape(3.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(
            text = label,
            style = MythosTypography.StatusEffect,
            color = color
        )
    }
}
