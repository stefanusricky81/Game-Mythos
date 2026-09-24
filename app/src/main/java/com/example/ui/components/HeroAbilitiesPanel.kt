package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsKabaddi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Hero
import com.example.ui.theme.MythosTokens
import com.example.ui.theme.MythosTypography

/**
 * Hero Abilities & Cooldowns Panel (Requirement: Ability Cooldowns).
 *
 * Displays Hercules' canonical mythological combat abilities:
 * 1. Twelve Labors (Ultimate Ability): Myth Power charge meter (X / 100 MP cooldown)
 * 2. Olympian Strike (Hero Active Skill): 1 Energy cooldown / turn readiness
 * 3. Last Stand (Passive Wrath): < 30% HP activation threshold cooldown
 * 4. Nemean Hide / Phalanx Guard (Defensive Stance): Damage reduction & counter readiness
 */
@Composable
fun HeroAbilitiesPanel(
    hero: Hero,
    currentEnergy: Int,
    currentMythPower: Int,
    maxMythPower: Int,
    isPlayerTurn: Boolean,
    isExecutingTurn: Boolean,
    onHeroAttackClick: () -> Unit,
    onUltimateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUltimateReady = currentMythPower >= maxMythPower && isPlayerTurn && !isExecutingTurn
    val isStrikeReady = currentEnergy >= 1 && isPlayerTurn && !isExecutingTurn

    val infiniteTransition = rememberInfiniteTransition(label = "ability_glow_transition")
    val ultGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ult_glow"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hercules_abilities_panel")
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xEE120F1B))
            .border(1.dp, MythosTokens.PanelBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Section Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "HERCULES COMBAT ABILITIES",
                    style = MythosTypography.ResourceLabel.copy(fontSize = 8.5.sp),
                    color = MythosTokens.PrimaryGold,
                    letterSpacing = 0.5.sp
                )
            }

            Text(
                text = "COOLDOWNS & READINESS",
                style = MythosTypography.HeroTitle.copy(fontSize = 7.5.sp),
                color = MythosTokens.TextMuted
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 4-Column Ability Grid / Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. TWELVE LABORS ULTIMATE
            AbilityCooldownSlot(
                name = "Twelve Labors",
                type = "ULTIMATE",
                icon = Icons.Default.AutoAwesome,
                isReady = isUltimateReady,
                glowAlpha = if (isUltimateReady) ultGlow else 0f,
                accentColor = MythosTokens.PrimaryGold,
                costOrCooldown = if (isUltimateReady) "READY" else "$currentMythPower/$maxMythPower MP",
                subText = if (isUltimateReady) "4,500 DMG" else "${maxMythPower - currentMythPower} to ready",
                modifier = Modifier.weight(1.15f),
                onClick = { if (isUltimateReady) onUltimateClick() }
            )

            // 2. OLYMPIAN STRIKE
            AbilityCooldownSlot(
                name = "Olympian Strike",
                type = "STRIKE",
                icon = Icons.Default.SportsKabaddi,
                isReady = isStrikeReady,
                glowAlpha = 0f,
                accentColor = MythosTokens.Energy,
                costOrCooldown = if (isStrikeReady) "READY" else if (currentEnergy < 1) "0/1 ENERGY" else "WAIT TURN",
                subText = "2,800 DMG",
                modifier = Modifier.weight(1f),
                onClick = { if (isStrikeReady) onHeroAttackClick() }
            )

            // 3. LAST STAND PASSIVE
            val lastStandStatus = when {
                hero.isLastStandActive -> "ACTIVE (+25%)"
                hero.hasLastStandTriggered -> "EXHAUSTED"
                else -> "READY (<30% HP)"
            }
            val lastStandColor = when {
                hero.isLastStandActive -> MythosTokens.Damage
                hero.hasLastStandTriggered -> MythosTokens.TextDisabled
                else -> MythosTokens.Warning
            }
            AbilityCooldownSlot(
                name = "Last Stand",
                type = "PASSIVE",
                icon = Icons.Default.FlashOn,
                isReady = hero.isLastStandActive,
                glowAlpha = if (hero.isLastStandActive) ultGlow else 0f,
                accentColor = lastStandColor,
                costOrCooldown = lastStandStatus,
                subText = "+25% ATK bonus",
                modifier = Modifier.weight(1f),
                onClick = {}
            )

            // 4. NEMEAN GUARD
            val defenseStatus = when {
                hero.currentShield > 0 -> "+${hero.currentShield} SHIELD"
                hero.damageReductionPercent > 0 -> "-${hero.damageReductionPercent}% DMG"
                hero.hasPhalanxGuard -> "PHALANX"
                else -> "STANDBY"
            }
            val defenseColor = if (hero.currentShield > 0 || hero.damageReductionPercent > 0) {
                MythosTokens.Shield
            } else {
                MythosTokens.TextMuted
            }
            AbilityCooldownSlot(
                name = "Nemean Guard",
                type = "STANCE",
                icon = Icons.Default.Shield,
                isReady = hero.currentShield > 0 || hero.damageReductionPercent > 0,
                glowAlpha = 0f,
                accentColor = defenseColor,
                costOrCooldown = defenseStatus,
                subText = "DEF 2,600",
                modifier = Modifier.weight(1f),
                onClick = {}
            )
        }
    }
}

@Composable
private fun AbilityCooldownSlot(
    name: String,
    type: String,
    icon: ImageVector,
    isReady: Boolean,
    glowAlpha: Float,
    accentColor: Color,
    costOrCooldown: String,
    subText: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isReady && glowAlpha > 0f) {
                    Brush.verticalGradient(
                        listOf(
                            accentColor.copy(alpha = 0.25f * glowAlpha),
                            Color(0x441F1A2A)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(Color(0x33282236), Color(0x22171322))
                    )
                }
            )
            .border(
                width = if (isReady) 1.2.dp else 0.5.dp,
                color = if (isReady) accentColor.copy(alpha = if (glowAlpha > 0f) glowAlpha else 0.8f) else MythosTokens.PanelBorder,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(enabled = isReady) { onClick() }
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = name,
                    tint = accentColor,
                    modifier = Modifier.size(13.dp)
                )

                Text(
                    text = type,
                    style = MythosTypography.RarityLabel.copy(fontSize = 6.5.sp),
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = name,
                style = MythosTypography.HeroName.copy(fontSize = 8.5.sp),
                color = Color.White,
                maxLines = 1
            )

            // Cooldown / Readiness Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accentColor.copy(alpha = 0.18f))
                    .padding(vertical = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = costOrCooldown,
                    style = MythosTypography.StatNumber.copy(fontSize = 7.sp),
                    color = accentColor,
                    maxLines = 1
                )
            }

            Text(
                text = subText,
                style = MythosTypography.CardDescription.copy(fontSize = 6.5.sp),
                color = MythosTokens.TextMuted,
                maxLines = 1,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
    }
}
