package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MythosTokens
import com.example.ui.theme.MythosTypography

/**
 * Reusable EnergyMeter Component.
 * Displays:
 * - ENERGY X / MAX
 * - Ten distinct crystal indicators (Available, Spent/Unlocked, Locked)
 * - Compact warning badge when player lacks sufficient energy
 */
@Composable
fun EnergyMeter(
    currentEnergy: Int,
    maxEnergy: Int,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    warningText: String? = null
) {
    // Pulse animation on insufficient energy highlight
    val infiniteTransition = rememberInfiniteTransition(label = "energy_highlight")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val meterBorderColor by animateColorAsState(
        targetValue = when {
            isHighlighted -> MythosTokens.Damage.copy(alpha = pulseAlpha)
            else -> MythosTokens.PanelBorder
        },
        label = "energy_border_color"
    )

    Column(
        modifier = modifier
            .testTag("energy_meter_container")
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF13101C))
            .border(1.dp, meterBorderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Glowing cyan energy icon
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(MythosTokens.Energy)
                )

                Text(
                    text = "ENERGY",
                    style = MythosTypography.ResourceLabel,
                    color = MythosTokens.Energy
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Compact "Need X Energy" tag (Section 11 & User constraint)
                warningText?.let { warning ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xCC7F1D1D))
                            .border(0.5.dp, MythosTokens.Damage, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = warning,
                            style = MythosTypography.ButtonSubtitle.copy(fontSize = 8.5.sp),
                            color = Color(0xFFFFCCCC)
                        )
                    }
                }

                // Numeric readout: AVAILABLE / MAX
                Text(
                    text = "$currentEnergy / $maxEnergy",
                    style = MythosTypography.ResourceNumber,
                    color = if (currentEnergy > 0) MythosTokens.TextPrimary else MythosTokens.TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 10 Distinct Crystal Indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..10) {
                val isAvailable = i <= currentEnergy
                val isUnlocked = i <= maxEnergy

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(
                            when {
                                isAvailable -> Brush.verticalGradient(
                                    listOf(MythosTokens.DivineBlueLight, MythosTokens.Energy, MythosTokens.EnergyDark)
                                )
                                isUnlocked -> Brush.verticalGradient(
                                    listOf(MythosTokens.EnergySlotSpent, Color(0xFF132230))
                                )
                                else -> Brush.verticalGradient(
                                    listOf(Color(0xFF181523), Color(0xFF0F0E17))
                                )
                            }
                        )
                        .border(
                            width = 0.5.dp,
                            color = when {
                                isAvailable -> MythosTokens.Energy
                                isUnlocked -> Color(0xFF2B4D63)
                                else -> Color(0xFF262135)
                            },
                            shape = RoundedCornerShape(2.5.dp)
                        )
                )
            }
        }
    }
}
