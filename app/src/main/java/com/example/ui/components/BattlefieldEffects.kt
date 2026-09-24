package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*

/**
 * Reusable visual language for combat feedback:
 * DAMAGE, HEAL, SHIELD, BUFF, DEBUFF, POISON, CRITICAL, ULTIMATE
 */
object CombatFeedbackVisuals {
    enum class FeedbackCategory {
        DAMAGE,
        HEAL,
        SHIELD,
        BUFF,
        DEBUFF,
        POISON,
        CRITICAL,
        ULTIMATE
    }

    data class FeedbackStyle(
        val textColor: Color,
        val borderColor: Color,
        val backgroundBrush: Brush
    )

    fun getStyle(category: FeedbackCategory): FeedbackStyle = when (category) {
        FeedbackCategory.DAMAGE -> FeedbackStyle(
            textColor = MythosTokens.Damage,
            borderColor = Color(0xFFB91C1C),
            backgroundBrush = Brush.linearGradient(listOf(Color(0xE62A1016), Color(0xE615080B)))
        )
        FeedbackCategory.CRITICAL -> FeedbackStyle(
            textColor = MythosTokens.Critical,
            borderColor = Color(0xFFFF0055),
            backgroundBrush = Brush.linearGradient(listOf(Color(0xE63D0C15), Color(0xE61E050A)))
        )
        FeedbackCategory.HEAL -> FeedbackStyle(
            textColor = MythosTokens.Heal,
            borderColor = Color(0xFF059669),
            backgroundBrush = Brush.linearGradient(listOf(Color(0xE60D281E), Color(0xE606140F)))
        )
        FeedbackCategory.SHIELD -> FeedbackStyle(
            textColor = MythosTokens.Shield,
            borderColor = Color(0xFF2563EB),
            backgroundBrush = Brush.linearGradient(listOf(Color(0xE60F2338), Color(0xE608121D)))
        )
        FeedbackCategory.BUFF -> FeedbackStyle(
            textColor = MythosTokens.Buff,
            borderColor = Color(0xFFD97706),
            backgroundBrush = Brush.linearGradient(listOf(Color(0xE62E1A08), Color(0xE6170D04)))
        )
        FeedbackCategory.DEBUFF -> FeedbackStyle(
            textColor = MythosTokens.Debuff,
            borderColor = Color(0xFFBE123C),
            backgroundBrush = Brush.linearGradient(listOf(Color(0xE62E0814), Color(0xE617040A)))
        )
        FeedbackCategory.POISON -> FeedbackStyle(
            textColor = MythosTokens.Poison,
            borderColor = Color(0xFF65A30D),
            backgroundBrush = Brush.linearGradient(listOf(Color(0xE619280D), Color(0xE60D1407)))
        )
        FeedbackCategory.ULTIMATE -> FeedbackStyle(
            textColor = MythosTokens.LightGold,
            borderColor = MythosTokens.PrimaryGold,
            backgroundBrush = Brush.linearGradient(listOf(Color(0xE63B2909), Color(0xE61D1404)))
        )
    }
}

@Composable
fun FloatingNumbersOverlay(
    floatingTexts: List<FloatingCombatText>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        floatingTexts.takeLast(2).forEachIndexed { index, item ->
            key(item.id) {
                FloatingItem(
                    item = item,
                    offsetY = (index * 20).dp
                )
            }
        }
    }
}

@Composable
private fun FloatingItem(
    item: FloatingCombatText,
    offsetY: androidx.compose.ui.unit.Dp
) {
    val animOffsetY = remember { Animatable(0f) }
    val animAlpha = remember { Animatable(1f) }
    val animScale = remember { Animatable(0.8f) }

    LaunchedEffect(item.id) {
        animScale.animateTo(1.1f, animationSpec = tween(150, easing = FastOutSlowInEasing))
        animOffsetY.animateTo(if (item.isEnemyTarget) 20f else -20f, animationSpec = tween(900, easing = LinearOutSlowInEasing))
        animAlpha.animateTo(0f, animationSpec = tween(400))
    }

    // Positions floating numbers close to the respective Hero without covering HP bars
    val alignment = if (item.isEnemyTarget) Alignment.TopCenter else Alignment.BottomCenter
    val baseOffset = if (item.isEnemyTarget) 12.dp else (-12).dp

    val category = when {
        item.isUltimate -> CombatFeedbackVisuals.FeedbackCategory.ULTIMATE
        item.isShield -> CombatFeedbackVisuals.FeedbackCategory.SHIELD
        item.text.contains("Poison", ignoreCase = true) -> CombatFeedbackVisuals.FeedbackCategory.POISON
        item.isPositive -> CombatFeedbackVisuals.FeedbackCategory.HEAL
        else -> CombatFeedbackVisuals.FeedbackCategory.DAMAGE
    }

    val style = CombatFeedbackVisuals.getStyle(category)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .offset(y = baseOffset + (animOffsetY.value).dp + offsetY)
                .scale(animScale.value)
                .clip(RoundedCornerShape(6.dp))
                .background(style.backgroundBrush)
                .border(0.5.dp, style.borderColor.copy(alpha = animAlpha.value), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = item.text,
                color = style.textColor.copy(alpha = animAlpha.value),
                style = MythosTypography.CombatFeedback.copy(
                    fontSize = if (item.isUltimate) 16.sp else 13.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Compact Combat Event Notification (Requirements #1 and #3)
 * Replaces the large center intrusive banner with a clean, compact, non-intrusive notification strip.
 */
@Composable
fun CompactCombatEventBanner(
    notification: CombatNotification?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = notification != null,
        enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.9f),
        exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.9f),
        modifier = modifier
    ) {
        notification?.let { notif ->
            val (bgColor, borderColor, textColor) = when (notif.type) {
                NotificationType.ATTACK -> Triple(Color(0xE62A1016), MythosTokens.Damage, Color(0xFFFF9494))
                NotificationType.DEFENSE -> Triple(Color(0xE6102231), MythosTokens.Shield, Color(0xFF90D8FF))
                NotificationType.SPELL -> Triple(Color(0xE61E152F), Color(0xFFA855F7), Color(0xFFD8B4FE))
                NotificationType.CARD -> Triple(Color(0xE6171E2D), MythosTokens.Energy, Color(0xFFBAE6FD))
                NotificationType.ULTIMATE -> Triple(Color(0xE6362208), MythosTokens.MythPower, MythosTokens.LightGold)
                NotificationType.STATUS -> Triple(Color(0xE6182613), MythosTokens.Poison, Color(0xFFBEF264))
                NotificationType.INFO -> Triple(Color(0xE6171522), MythosTokens.PanelBorder, Color(0xFFDDD8E8))
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = notif.text,
                    color = textColor,
                    style = MythosTypography.CombatNotification,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun LastStandBanner(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "banner_pulse")
    val bannerGlow by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "banner_glow"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF5A141A),
                            Color(0xFF2E090D),
                            Color(0xFF1E0A0D)
                        )
                    )
                )
                .border(1.5.dp, MythosTokens.PrimaryGold.copy(alpha = bannerGlow), RoundedCornerShape(14.dp))
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        1.5.dp,
                        MythosTokens.PrimaryGold.copy(alpha = bannerGlow),
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Image(
                    painter = painterResource(id = HerculesIdentity.assets.lastStand.resolveResId()),
                    contentDescription = "Hercules Last Stand",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "⚡ LAST STAND ACTIVATED ⚡",
                color = MythosTokens.PrimaryGold,
                style = MythosTypography.GameTitle.copy(fontSize = 15.sp),
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Hercules fell below 30% HP!\nCelestial wrath triggers: +25% ATTACK power permanently active for this battle!",
                color = Color(0xFFFFECEE),
                style = MythosTypography.CardDescription.copy(fontSize = 11.sp, lineHeight = 15.sp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            MythosButton(
                text = "CONTINUE BATTLE",
                onClick = onDismiss,
                style = MythosButtonStyle.PRIMARY,
                modifier = Modifier.width(160.dp)
            )
        }
    }
}

@Composable
fun CombatLogTicker(
    logs: List<CombatLog>,
    modifier: Modifier = Modifier
) {
    val latestLog = logs.firstOrNull() ?: return

    val logColor = when (latestLog.type) {
        LogType.ATTACK -> Color(0xFFFF9595)
        LogType.DEFENSE -> Color(0xFF90D7FF)
        LogType.SPELL -> Color(0xFFCFACFF)
        LogType.PASSIVE -> MythosTokens.PrimaryGold
        LogType.ULTIMATE -> MythosTokens.MythPower
        LogType.POISON -> Color(0xFFA6E838)
        LogType.INFO -> Color(0xFFDDD8E8)
    }

    Box(
        modifier = modifier
            .fillMaxWidth(0.92f)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xCC110F18))
            .border(0.5.dp, Color(0xFF282436), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = latestLog.message,
            color = logColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}
