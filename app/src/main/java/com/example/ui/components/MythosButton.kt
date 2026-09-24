package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MythosTokens
import com.example.ui.theme.MythosTypography

enum class MythosButtonStyle {
    PRIMARY,
    SECONDARY,
    DANGER,
    DISABLED,
    ULTIMATE
}

/**
 * Reusable Button System for MYTHOS.
 * Supports: PRIMARY, SECONDARY, DANGER, DISABLED, ULTIMATE styles.
 * States: normal, pressed, disabled, highlighted.
 * Accessibility: Minimum 44-48dp touch target height.
 */
@Composable
fun MythosButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: MythosButtonStyle = MythosButtonStyle.PRIMARY,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isHighlighted: Boolean = false,
    testTag: String = "mythos_button"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Smooth press scale animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "btn_press_scale"
    )

    // Glowing pulse for ULTIMATE or HIGHLIGHTED state
    val infiniteTransition = rememberInfiniteTransition(label = "btn_glow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "btn_pulse"
    )

    val effectiveStyle = if (!enabled) MythosButtonStyle.DISABLED else style

    val (bgBrush, borderColor, textColor, glowColor) = when (effectiveStyle) {
        MythosButtonStyle.PRIMARY -> Quadruple(
            Brush.verticalGradient(listOf(MythosTokens.PrimaryGold, MythosTokens.SecondaryGold)),
            MythosTokens.LightGold,
            Color(0xFF1B1405),
            MythosTokens.PrimaryGold
        )
        MythosButtonStyle.SECONDARY -> Quadruple(
            Brush.verticalGradient(listOf(Color(0xFF262035), Color(0xFF191624))),
            if (isHighlighted) MythosTokens.PrimaryGold.copy(alpha = pulseAlpha) else MythosTokens.PanelBorder,
            MythosTokens.TextPrimary,
            Color.Transparent
        )
        MythosButtonStyle.DANGER -> Quadruple(
            Brush.verticalGradient(listOf(MythosTokens.Damage, Color(0xFF991B1B))),
            Color(0xFFFCA5A5),
            Color.White,
            MythosTokens.Damage
        )
        MythosButtonStyle.ULTIMATE -> Quadruple(
            Brush.verticalGradient(
                listOf(
                    MythosTokens.LightGold.copy(alpha = pulseAlpha),
                    MythosTokens.PrimaryGold,
                    MythosTokens.MythPowerFlame
                )
            ),
            Color.White,
            Color(0xFF261803),
            MythosTokens.PrimaryGold
        )
        MythosButtonStyle.DISABLED -> Quadruple(
            Brush.verticalGradient(listOf(Color(0xFF1B1824), Color(0xFF13111A))),
            Color(0xFF2D273D),
            MythosTokens.TextDisabled,
            Color.Transparent
        )
    }

    Box(
        modifier = modifier
            .testTag(testTag)
            .scale(scale)
            .defaultMinSize(minHeight = 44.dp)
            .shadow(
                elevation = if (effectiveStyle == MythosButtonStyle.ULTIMATE && enabled) 8.dp else 2.dp,
                shape = RoundedCornerShape(8.dp),
                ambientColor = glowColor,
                spotColor = glowColor
            )
            .clip(RoundedCornerShape(8.dp))
            .background(bgBrush)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Text(
                    text = text,
                    style = MythosTypography.ButtonText,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
            }

            subtitle?.let {
                Text(
                    text = it,
                    style = MythosTypography.ButtonSubtitle,
                    color = textColor.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
