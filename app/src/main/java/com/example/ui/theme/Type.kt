package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * MYTHOS — Centralized Typography System
 * Prioritizes high legibility on mobile screens while maintaining
 * an ancient mythological, premium card battle aesthetic.
 */
object MythosTypography {

    val GameTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = 1.5.sp,
        color = MythosTokens.PrimaryGold
    )

    val GameSubtitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.sp,
        color = MythosTokens.DivineBlueLight
    )

    val HeroName = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp,
        color = MythosTokens.TextPrimary
    )

    val HeroTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        color = MythosTokens.TextMuted
    )

    val CardName = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.2.sp,
        color = MythosTokens.TextPrimary
    )

    val CardDescription = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 8.5.sp,
        lineHeight = 11.5.sp,
        color = MythosTokens.TextSecondary
    )

    val StatNumber = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        color = MythosTokens.TextPrimary
    )

    val ResourceNumber = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 11.5.sp,
        lineHeight = 14.sp,
        color = MythosTokens.TextPrimary
    )

    val ResourceLabel = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.5.sp
    )

    val ButtonText = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )

    val ButtonSubtitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 7.5.sp,
        lineHeight = 9.sp
    )

    val RarityLabel = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 7.sp,
        lineHeight = 9.sp,
        letterSpacing = 0.5.sp
    )

    val CardTypeLabel = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 7.5.sp,
        lineHeight = 9.sp,
        letterSpacing = 0.4.sp
    )

    val StatusEffect = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 7.5.sp,
        lineHeight = 9.sp
    )

    val CombatFeedback = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 14.sp,
        lineHeight = 16.sp
    )

    val CombatNotification = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
}

// Material 3 Typography mapping
val Typography = Typography(
    titleLarge = MythosTypography.GameTitle,
    titleMedium = MythosTypography.HeroName,
    bodyLarge = MythosTypography.CardName,
    bodyMedium = MythosTypography.CardDescription,
    labelLarge = MythosTypography.ButtonText,
    labelSmall = MythosTypography.RarityLabel
)
