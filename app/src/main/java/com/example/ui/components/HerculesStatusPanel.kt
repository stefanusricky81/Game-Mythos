package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.CharacterFaction
import com.example.data.HerculesIdentity
import com.example.data.Hero
import com.example.ui.theme.MythosTokens
import com.example.ui.theme.MythosTypography
import java.text.NumberFormat
import java.util.Locale

/**
 * Hercules Character Metadata & Health Panel (Requirements: Character Metadata & Health Display).
 *
 * Explicitly binds to canonical metadata in [HerculesIdentity]:
 * - Hero ID: "hero_hercules"
 * - Name: "Hercules"
 * - Title: "Champion of Olympus"
 * - Faction: "Greek / Olympus"
 * - Rarity: "LEGENDARY"
 *
 * Detailed Health Display:
 * - High-contrast health bar with formatted current and max HP values
 * - 30% Last Stand Wrath activation notch indicator
 * - Dynamic shield layer
 * - Interactive metadata dialog with lore and attributes
 */
@Composable
fun HerculesStatusPanel(
    hero: Hero,
    modifier: Modifier = Modifier
) {
    var showMetadataDialog by remember { mutableStateOf(false) }

    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }
    val formattedCurrentHp = remember(hero.currentHp) { numberFormat.format(hero.currentHp) }
    val formattedMaxHp = remember(hero.maxHp) { numberFormat.format(hero.maxHp) }
    val hpFraction = hero.hpPercentage
    val hpPercentInt = (hpFraction * 100).toInt()

    val hpBarColor by animateColorAsState(
        targetValue = when {
            hero.isLastStandActive -> MythosTokens.Damage
            hpFraction > 0.6f -> MythosTokens.Success
            hpFraction > 0.3f -> MythosTokens.Warning
            else -> MythosTokens.Damage
        },
        label = "hercules_hp_bar_color"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "hercules_last_stand_pulse")
    val auraGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hercules_aura_glow"
    )

    val frameBorder = if (hero.isLastStandActive) {
        BorderStroke(2.dp, MythosTokens.PrimaryGold.copy(alpha = auraGlow))
    } else {
        BorderStroke(1.2.dp, MythosTokens.SecondaryGold.copy(alpha = 0.8f))
    }

    Surface(
        modifier = modifier
            .testTag("hercules_health_metadata_panel")
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(frameBorder, RoundedCornerShape(12.dp)),
        color = Color(0xEE120F1C),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. HERCULES PORTRAIT AVATAR (Dynamic Last Stand empowerment)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
                    .border(
                        BorderStroke(
                            width = if (hero.isLastStandActive) 2.dp else 1.2.dp,
                            color = if (hero.isLastStandActive) MythosTokens.PrimaryGold else MythosTokens.MetallicBronze
                        ),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { showMetadataDialog = true }
            ) {
                Image(
                    painter = painterResource(id = hero.getEffectivePortraitResId()),
                    contentDescription = HerculesIdentity.NAME,
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
                            text = "WRATH +25%",
                            style = MythosTypography.RarityLabel.copy(fontSize = 6.sp),
                            color = MythosTokens.LightGold,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 2. CHARACTER METADATA & HEALTH STATS
            Column(modifier = Modifier.weight(1f)) {
                // Header: Name, Faction, Rarity Badge & Metadata Info Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = HerculesIdentity.NAME.uppercase(),
                            style = MythosTypography.HeroName.copy(fontSize = 13.sp),
                            color = MythosTokens.PrimaryGold
                        )

                        // Rarity Tag from Character Metadata
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(MythosTokens.getRarityColor(HerculesIdentity.RARITY).copy(alpha = 0.2f))
                                .border(0.5.dp, MythosTokens.getRarityColor(HerculesIdentity.RARITY), RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = HerculesIdentity.RARITY.label.uppercase(),
                                style = MythosTypography.RarityLabel.copy(fontSize = 7.5.sp),
                                color = MythosTokens.getRarityColor(HerculesIdentity.RARITY)
                            )
                        }

                        // Faction Tag
                        Text(
                            text = "• ${HerculesIdentity.FACTION.displayName}",
                            style = MythosTypography.HeroTitle.copy(fontSize = 8.5.sp),
                            color = MythosTokens.TextMuted
                        )
                    }

                    // Stat Badges (Attack, Defense, and Info Dialog Trigger)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Effective Attack
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (hero.isLastStandActive || hero.attackBuffPercent > 0)
                                        Color(0x44EF4444)
                                    else
                                        Color(0x332A2438)
                                )
                                .border(0.5.dp, MythosTokens.Damage, RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "⚔ ${hero.effectiveAttack}",
                                style = MythosTypography.StatNumber.copy(fontSize = 8.5.sp),
                                color = if (hero.isLastStandActive) MythosTokens.PrimaryGold else Color(0xFFFF8A8A)
                            )
                        }

                        // Base Defense
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0x332A2438))
                                .border(0.5.dp, MythosTokens.DivineBlue, RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "🛡 ${hero.baseDefense}",
                                style = MythosTypography.StatNumber.copy(fontSize = 8.5.sp),
                                color = MythosTokens.Shield
                            )
                        }

                        // Info Icon (opens metadata dialog)
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(0x3338BDF8))
                                .border(0.5.dp, MythosTokens.DivineBlueLight, CircleShape)
                                .clickable { showMetadataDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Hero Metadata",
                                tint = MythosTokens.DivineBlueLight,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 3. DETAILED HEALTH BAR WITH 30% LAST STAND THRESHOLD INDICATOR
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(15.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF231F2E))
                        .border(
                            width = if (hero.isLastStandActive) 1.dp else 0.5.dp,
                            color = if (hero.isLastStandActive) MythosTokens.Damage.copy(alpha = auraGlow) else MythosTokens.PanelBorder,
                            shape = RoundedCornerShape(6.dp)
                        )
                ) {
                    // Filled Health Bar
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = hpFraction)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.horizontalGradient(
                                    if (hero.isLastStandActive) {
                                        listOf(Color(0xFFE63946), MythosTokens.PrimaryGold)
                                    } else {
                                        listOf(hpBarColor.copy(alpha = 0.85f), hpBarColor)
                                    }
                                )
                            )
                    )

                    // 30% LAST STAND THRESHOLD MARKER LINE
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = 0.30f),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .width(1.5.dp)
                                .fillMaxHeight()
                                .background(MythosTokens.PrimaryGold.copy(alpha = 0.8f))
                        )
                    }

                    // Numeric Health Text and Percentage
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "HP",
                                style = MythosTypography.StatNumber.copy(fontSize = 8.sp),
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$formattedCurrentHp / $formattedMaxHp",
                                style = MythosTypography.StatNumber.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        // Percentage & Status
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (hero.isLastStandActive) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = null,
                                    tint = MythosTokens.PrimaryGold,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                            Text(
                                text = "$hpPercentInt%",
                                style = MythosTypography.StatNumber.copy(fontSize = 8.5.sp),
                                color = if (hero.isLastStandActive) MythosTokens.PrimaryGold else Color.White
                            )
                        }
                    }
                }

                // 4. SHIELD BAR (WHEN SHIELD > 0)
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
                            text = "SHIELD +${numberFormat.format(hero.currentShield)}",
                            style = MythosTypography.StatNumber.copy(fontSize = 7.sp, fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 4.dp)
                        )
                    }
                }

                // 5. STATUS EFFECTS ROW
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (hero.isLastStandActive) {
                        HeroStatusChip(label = "⚡ LAST STAND (+25% ATK)", color = MythosTokens.PrimaryGold)
                    }
                    if (hero.attackBuffPercent > 0) {
                        HeroStatusChip(label = "+${hero.attackBuffPercent}% ATK", color = MythosTokens.Buff)
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

    // INTERACTIVE HERO METADATA MODAL DIALOG
    if (showMetadataDialog) {
        Dialog(onDismissRequest = { showMetadataDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.2.dp, MythosTokens.PrimaryGold, RoundedCornerShape(14.dp)),
                color = MythosTokens.BackgroundSurface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Portrait and Title
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, MythosTokens.PrimaryGold, RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            painter = painterResource(id = HerculesIdentity.assets.portrait.resolveResId()),
                            contentDescription = HerculesIdentity.NAME,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = HerculesIdentity.NAME,
                        style = MythosTypography.GameTitle.copy(fontSize = 16.sp),
                        color = MythosTokens.PrimaryGold
                    )

                    Text(
                        text = "${HerculesIdentity.TITLE} • ${HerculesIdentity.FACTION.displayName}",
                        style = MythosTypography.HeroTitle.copy(fontSize = 11.sp),
                        color = MythosTokens.TextSecondary
                    )

                    Text(
                        text = "hero_id: ${HerculesIdentity.HERO_ID} • Rarity: ${HerculesIdentity.RARITY.label} • v1.0.0",
                        style = MythosTypography.ResourceLabel.copy(fontSize = 9.sp),
                        color = MythosTokens.DivineBlueLight,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Lore Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MythosTokens.Panel)
                            .border(0.5.dp, MythosTokens.PanelBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = HerculesIdentity.definition.loreDescription,
                            style = MythosTypography.CardDescription.copy(fontSize = 10.5.sp, lineHeight = 14.sp),
                            color = MythosTokens.TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Base Attributes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AttributeColumn("HEALTH", "$formattedCurrentHp / $formattedMaxHp", MythosTokens.Success)
                        AttributeColumn("BASE ATK", "${hero.baseAttack}", MythosTokens.Damage)
                        AttributeColumn("BASE DEF", "${hero.baseDefense}", MythosTokens.DivineBlue)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    MythosButton(
                        text = "RETURN TO BATTLE",
                        onClick = { showMetadataDialog = false },
                        style = MythosButtonStyle.PRIMARY,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun AttributeColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MythosTypography.ResourceLabel.copy(fontSize = 8.sp),
            color = MythosTokens.TextMuted
        )
        Text(
            text = value,
            style = MythosTypography.StatNumber.copy(fontSize = 10.sp),
            color = color
        )
    }
}
