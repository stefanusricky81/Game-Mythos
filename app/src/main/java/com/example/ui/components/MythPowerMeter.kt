package com.example.ui.components

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
 * Reusable MythPowerMeter Component.
 * Displays:
 * - MYTH POWER X / 100
 * - Dedicated animated progress bar
 * - Glowing golden state when fully charged (100 MP)
 * - Temporary floating/tag gain indicators
 */
@Composable
fun MythPowerMeter(
    currentMythPower: Int,
    maxMythPower: Int = 100,
    modifier: Modifier = Modifier,
    gainNotification: String? = null
) {
    val progress = (currentMythPower.toFloat() / maxMythPower.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "myth_power_progress"
    )

    // Glowing animation when ultimate is fully ready
    val isFull = currentMythPower >= maxMythPower
    val infiniteTransition = rememberInfiniteTransition(label = "mp_full_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Column(
        modifier = modifier
            .testTag("myth_power_meter_container")
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF13101C))
            .border(
                1.dp,
                if (isFull) MythosTokens.PrimaryGold.copy(alpha = glowAlpha) else MythosTokens.PanelBorder,
                RoundedCornerShape(8.dp)
            )
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
                // Golden Myth Icon
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFull) MythosTokens.LightGold else MythosTokens.MythPower
                        )
                )

                Text(
                    text = "MYTH POWER",
                    style = MythosTypography.ResourceLabel,
                    color = if (isFull) MythosTokens.LightGold else MythosTokens.MythPower
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                gainNotification?.let { gainText ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x99533B05))
                            .border(0.5.dp, MythosTokens.PrimaryGold, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = gainText,
                            style = MythosTypography.ButtonSubtitle.copy(fontSize = 8.sp),
                            color = MythosTokens.LightGold
                        )
                    }
                }

                Text(
                    text = "$currentMythPower / $maxMythPower",
                    style = MythosTypography.ResourceNumber,
                    color = if (isFull) MythosTokens.LightGold else MythosTokens.TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF221C2B))
                .border(0.5.dp, Color(0xFF352C44), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = animatedProgress)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isFull) {
                            Brush.horizontalGradient(
                                listOf(
                                    MythosTokens.MythPowerFlame,
                                    MythosTokens.PrimaryGold,
                                    MythosTokens.LightGold
                                )
                            )
                        } else {
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF854D0E),
                                    MythosTokens.MythPower
                                )
                            )
                        }
                    )
            )
        }
    }
}
