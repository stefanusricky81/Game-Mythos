package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MythosTokens

/**
 * Composite ResourceMeters component combining:
 * - Reusable EnergyMeter
 * - Reusable MythPowerMeter
 * - Reusable MythosButton system (Attack, Twelve Labors, End Turn)
 */
@Composable
fun ResourceMeters(
    currentEnergy: Int,
    maxEnergy: Int,
    currentMythPower: Int,
    maxMythPower: Int,
    isPlayerTurn: Boolean,
    isExecutingTurn: Boolean,
    isEnergyHighlighted: Boolean = false,
    mythPowerGainNotification: Int? = null,
    compactEnergyWarning: String? = null,
    onHeroAttackClick: () -> Unit,
    onUltimateClick: () -> Unit,
    onEndTurnClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canAttack = isPlayerTurn && !isExecutingTurn && currentEnergy >= 1
    val isUltimateReady = currentMythPower >= maxMythPower && isPlayerTurn && !isExecutingTurn
    val canEndTurn = isPlayerTurn && !isExecutingTurn

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Upper row: Reusable Energy Meter & Reusable Myth Power Meter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EnergyMeter(
                currentEnergy = currentEnergy,
                maxEnergy = maxEnergy,
                isHighlighted = isEnergyHighlighted,
                warningText = compactEnergyWarning,
                modifier = Modifier.weight(1f)
            )

            MythPowerMeter(
                currentMythPower = currentMythPower,
                maxMythPower = maxMythPower,
                gainNotification = mythPowerGainNotification?.let { "+$it MP" },
                modifier = Modifier.weight(1f)
            )
        }

        // Lower row: Action Buttons via reusable MythosButton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // HERO ATTACK (1 Energy)
            MythosButton(
                text = "⚔ ATTACK",
                subtitle = "1 Energy • 2.8k",
                onClick = onHeroAttackClick,
                style = if (canAttack) MythosButtonStyle.DANGER else MythosButtonStyle.DISABLED,
                enabled = canAttack,
                testTag = "hero_attack_button",
                modifier = Modifier.weight(1f)
            )

            // TWELVE LABORS ULTIMATE
            MythosButton(
                text = "TWELVE LABORS",
                subtitle = if (isUltimateReady) "READY (4.5k DMG)" else "$currentMythPower/100 MP",
                onClick = onUltimateClick,
                style = if (isUltimateReady) MythosButtonStyle.ULTIMATE else MythosButtonStyle.DISABLED,
                enabled = isUltimateReady,
                icon = if (isUltimateReady) Icons.Default.Bolt else Icons.Default.Lock,
                testTag = "twelve_labors_button",
                modifier = Modifier.weight(1.3f)
            )

            // END TURN
            MythosButton(
                text = if (isPlayerTurn) "END TURN" else "ARES...",
                subtitle = if (isPlayerTurn) "Refill Hand" else null,
                onClick = onEndTurnClick,
                style = if (canEndTurn) MythosButtonStyle.SECONDARY else MythosButtonStyle.DISABLED,
                enabled = canEndTurn,
                testTag = "end_turn_button",
                modifier = Modifier.weight(0.9f)
            )
        }
    }
}
