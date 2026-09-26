package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.data.CardRarity
import com.example.data.CardType

/**
 * MYTHOS — Centralized Design Tokens
 * Reusable color constants, rarity palettes, and visual styles for the game.
 */
object MythosTokens {

    // 1. Core Atmosphere & Surfaces (Ancient Dark Stone & Dark Navy)
    val Background = Color(0xFF0D0B12)
    val BackgroundSurface = Color(0xFF14121B)
    val DarkNavy = Color(0xFF0F172A)
    val DarkNavyDeep = Color(0xFF0A0F1D)
    val Panel = Color(0xFF161420)
    val PanelElevated = Color(0xFF201D2E)
    val PanelBorder = Color(0xFF332D44)
    val PanelHighlight = Color(0xFF4A4064)

    // 2. Divine Gold Accents (Ancient Metallic & Celestial Gold)
    val PrimaryGold = Color(0xFFFFD166)
    val SecondaryGold = Color(0xFFC79218)
    val LightGold = Color(0xFFFFF2B8)
    val MetallicBronze = Color(0xFFCD7F32)

    // 3. Divine Blue & Ether
    val DivineBlue = Color(0xFF38BDF8)
    val DivineBlueLight = Color(0xFFBAE6FD)
    val DivineBlueDark = Color(0xFF0284C7)

    // 4. Typography Colors
    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFFDDD8E6)
    val TextMuted = Color(0xFFA19BAE)
    val TextDisabled = Color(0xFF6B6577)

    // 5. Combat & Status Feedback
    val Success = Color(0xFF10B981)
    val HealthGreen = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)
    val Damage = Color(0xFFEF4444)
    val Critical = Color(0xFFFF334B)
    val Shield = Color(0xFF93C5FD)
    val Heal = Color(0xFF34D399)
    val Buff = Color(0xFFF97316)
    val Debuff = Color(0xFFE11D48)
    val Poison = Color(0xFF84CC16)

    // 6. Resources
    val Energy = Color(0xFF22D3EE)
    val EnergyDark = Color(0xFF0E7490)
    val EnergyGold = Color(0xFFFFD166)
    val EnergySlotSpent = Color(0xFF1E3A4B)
    val EnergySlotLocked = Color(0xFF191722)
    val MythPower = Color(0xFFFBBF24)
    val MythPowerFlame = Color(0xFFF97316)
    val MythPowerDark = Color(0xFF4A3A17)

    // 7. Six-Tier Rarity System
    val Common = Color(0xFF94A3B8)
    val Uncommon = Color(0xFF10B981)
    val Rare = Color(0xFF38BDF8)
    val Epic = Color(0xFFA855F7)
    val Legendary = Color(0xFFFBBF24)
    val Mythic = Color(0xFFF43F5E)

    fun getRarityColor(rarity: CardRarity): Color = when (rarity) {
        CardRarity.COMMON -> Color(0xFF94A3B8)       // Ancient Stone Slate
        CardRarity.UNCOMMON -> Color(0xFF10B981)     // Spartan Bronze Jade
        CardRarity.RARE -> Color(0xFF38BDF8)         // Sapphire Divine Light
        CardRarity.EPIC -> Color(0xFFA855F7)         // Imperial Mount Olympus Amethyst
        CardRarity.LEGENDARY -> Color(0xFFFBBF24)    // Radiant Solar Amber Gold
        CardRarity.MYTHIC -> Color(0xFFF43F5E)       // Transcendent Celestial Crimson
    }

    fun getRarityBorderBrush(rarity: CardRarity, isPlayable: Boolean, shimmerAlpha: Float = 0.8f): Brush {
        val baseColor = getRarityColor(rarity)
        return if (isPlayable) {
            when (rarity) {
                CardRarity.COMMON -> Brush.linearGradient(
                    listOf(baseColor, Color(0xFFCBD5E1), baseColor)
                )
                CardRarity.UNCOMMON -> Brush.linearGradient(
                    listOf(baseColor, Color(0xFF6EE7B7), baseColor)
                )
                CardRarity.RARE -> Brush.linearGradient(
                    listOf(baseColor, DivineBlueLight.copy(alpha = shimmerAlpha), baseColor)
                )
                CardRarity.EPIC -> Brush.sweepGradient(
                    listOf(baseColor, Color(0xFFE9D5FF), baseColor)
                )
                CardRarity.LEGENDARY -> Brush.sweepGradient(
                    listOf(baseColor, LightGold.copy(alpha = shimmerAlpha), PrimaryGold, baseColor)
                )
                CardRarity.MYTHIC -> Brush.sweepGradient(
                    listOf(baseColor, PrimaryGold, Color(0xFFFF80BF), baseColor)
                )
            }
        } else {
            Brush.linearGradient(
                listOf(baseColor.copy(alpha = 0.35f), Color(0xFF221F2D))
            )
        }
    }

    // 8. Card Type Palettes
    fun getCardTypeColor(type: CardType): Color = when (type) {
        CardType.ATTACK -> Color(0xFFEF4444)
        CardType.DEFENSE -> Color(0xFF3B82F6)
        CardType.SPELL -> Color(0xFF8B5CF6)
        CardType.SUMMON -> Color(0xFF10B981)
        CardType.RELIC -> Color(0xFFF59E0B)
        CardType.TRAP -> Color(0xFFEC4899)
    }
}
