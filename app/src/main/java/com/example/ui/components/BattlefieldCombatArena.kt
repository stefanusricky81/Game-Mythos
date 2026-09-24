package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.data.Hero
import com.example.ui.theme.MythosTokens

/**
 * Production Battlefield Combat Arena (Requirement #4B & #10):
 * Renders the heroes in their battlefield representations:
 * - Hercules in full combat stance with divine presence and Last Stand aura
 * - Ares opposite in warlord stance
 * Strictly follows the visual rules:
 * - No text embedded in artwork
 * - No UI bars embedded in artwork
 * - Pure character presentation with atmospheric mythological lighting
 */
@Composable
fun BattlefieldCombatArena(
    playerHero: Hero,
    enemyHero: Hero,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "battle_arena_breath")

    // Subtle breathing scale animation for living heroes
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_scale"
    )

    // Last Stand divine radiance glow
    val lastStandGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "last_stand_glow"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("battlefield_combat_arena"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ENEMY COMBAT COMBATANT (Ares)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(0.85f),
                contentAlignment = Alignment.CenterStart
            ) {
                // Ground shadow ellipse
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-6).dp)
                        .size(width = 110.dp, height = 24.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                            )
                        )
                )

                // Enemy Hero Illustration
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .widthIn(max = 145.dp)
                        .aspectRatio(3f / 4f)
                        .scale(breathScale)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x55170B10))
                        .border(
                            width = 1.dp,
                            color = Color(0x66B91C1C),
                            shape = RoundedCornerShape(14.dp)
                        )
                ) {
                    Image(
                        painter = painterResource(id = enemyHero.portraitResId),
                        contentDescription = enemyHero.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Atmospheric dark red gradient vignette
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.5f)
                                    )
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // PLAYER HERO COMBATANT (Hercules - Production Battle Asset)
            Box(
                modifier = Modifier
                    .weight(1.15f)
                    .fillMaxHeight(0.95f),
                contentAlignment = Alignment.CenterEnd
            ) {
                // Divine ground shadow & golden ring
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-4).dp)
                        .size(width = 130.dp, height = 28.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                if (playerHero.isLastStandActive) {
                                    listOf(
                                        MythosTokens.PrimaryGold.copy(alpha = 0.5f * lastStandGlow),
                                        Color.Black.copy(alpha = 0.6f),
                                        Color.Transparent
                                    )
                                } else {
                                    listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                                }
                            )
                        )
                )

                // Hercules Battle Art Container
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .widthIn(max = 160.dp)
                        .aspectRatio(3f / 4f)
                        .scale(breathScale)
                        .shadow(
                            elevation = if (playerHero.isLastStandActive) 14.dp else 6.dp,
                            shape = RoundedCornerShape(16.dp),
                            spotColor = if (playerHero.isLastStandActive) MythosTokens.PrimaryGold else Color.Black
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x4414121E))
                        .border(
                            width = if (playerHero.isLastStandActive) 2.dp else 1.2.dp,
                            color = if (playerHero.isLastStandActive) {
                                MythosTokens.PrimaryGold.copy(alpha = lastStandGlow)
                            } else {
                                MythosTokens.SecondaryGold.copy(alpha = 0.7f)
                            },
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    // Production Hercules Battle Artwork
                    Image(
                        painter = painterResource(id = playerHero.getBattleResId()),
                        contentDescription = playerHero.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Last Stand celestial wrath radiant overlay
                    if (playerHero.isLastStandActive) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            MythosTokens.PrimaryGold.copy(alpha = 0.25f * lastStandGlow),
                                            Color.Transparent,
                                            Color(0x446B1119)
                                        )
                                    )
                                )
                        )
                    }

                    // Bottom ambient shadow to ground the character
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.65f)
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}
