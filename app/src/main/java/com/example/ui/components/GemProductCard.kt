package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.monetization.GemProduct
import com.example.ui.theme.MythosTokens
import com.example.ui.theme.MythosTypography

/**
 * Polished Myth Gem Product Card for the Top-Up Store (Requirement #5).
 */
@Composable
fun GemProductCard(
    product: GemProduct,
    onBuyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFeatured = product.featured
    val borderColor = if (isFeatured) MythosTokens.PrimaryGold else MythosTokens.PanelBorder
    val containerColor = if (isFeatured) MythosTokens.PanelElevated else MythosTokens.Panel

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isFeatured) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .testTag("gem_product_${product.productId}"),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top Gem Icon with Radiant Circle Background
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    MythosTokens.DivineBlue.copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(1.dp, MythosTokens.DivineBlueLight.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Diamond,
                        contentDescription = "Myth Gems",
                        tint = MythosTokens.DivineBlueLight,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Gem Amount Header
                val numberFormat = java.text.NumberFormat.getIntegerInstance(java.util.Locale.US)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = numberFormat.format(product.gemAmount),
                        style = MythosTypography.GameTitle.copy(fontSize = 21.sp),
                        color = MythosTokens.PrimaryGold
                    )
                    Text(
                        text = "MYTH GEMS",
                        style = MythosTypography.RarityLabel.copy(fontSize = 9.sp, letterSpacing = 1.sp),
                        color = MythosTokens.TextSecondary
                    )
                }

                // Bonus Tag & Total Gems Clarity
                if (product.bonusGems > 0) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MythosTokens.Success.copy(alpha = 0.2f))
                                .border(0.5.dp, MythosTokens.Success, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "+${numberFormat.format(product.bonusGems)} BONUS",
                                style = MythosTypography.RarityLabel.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = MythosTokens.Success
                            )
                        }
                        Text(
                            text = "TOTAL ${numberFormat.format(product.totalGems)}",
                            style = MythosTypography.HeroName.copy(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold),
                            color = MythosTokens.LightGold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(34.dp))
                }

                // Price display & Buy Button
                MythosButton(
                    text = product.priceDisplay,
                    onClick = onBuyClick,
                    style = if (isFeatured) MythosButtonStyle.PRIMARY else MythosButtonStyle.SECONDARY,
                    testTag = "buy_btn_${product.productId}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                )
            }

            // Promotional Badge (e.g. BEST VALUE, POPULAR)
            product.badgeText?.let { badge ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(bottomStart = 8.dp, topEnd = 12.dp))
                        .background(
                            if (badge == "BEST VALUE") MythosTokens.MythPowerFlame
                            else MythosTokens.PrimaryGold
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badge,
                        style = MythosTypography.RarityLabel.copy(fontSize = 8.sp),
                        color = Color.Black,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
