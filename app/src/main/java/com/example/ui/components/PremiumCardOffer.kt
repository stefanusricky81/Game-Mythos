package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Card
import com.example.monetization.DirectCardPricing
import com.example.ui.theme.MythosTokens
import com.example.ui.theme.MythosTypography

/**
 * Reusable PremiumCardOffer Component (Requirement #7).
 * Renders direct card purchasing cards adhering strictly to MYTHOS visual language.
 */
@Composable
fun PremiumCardOffer(
    card: Card,
    isOwned: Boolean,
    onBuyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rarityColor = MythosTokens.getRarityColor(card.rarity)
    val priceDisplay = DirectCardPricing.getPriceDisplay(card.rarity) ?: "Not for Sale"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(rarityColor, MythosTokens.PanelBorder)
                ),
                RoundedCornerShape(12.dp)
            )
            .testTag("card_offer_${card.id}"),
        colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Miniature Card Preview Frame
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(102.dp)
                ) {
                    CardFrame(
                        card = card,
                        isPlayable = true,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Card details column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // Card Rarity & Type Tags
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(rarityColor.copy(alpha = 0.2f))
                                .border(0.5.dp, rarityColor, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = card.rarity.label.uppercase(),
                                style = MythosTypography.RarityLabel.copy(fontSize = 8.5.sp),
                                color = rarityColor
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MythosTokens.PanelElevated)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = card.type.label.uppercase(),
                                style = MythosTypography.CardTypeLabel.copy(fontSize = 8.5.sp),
                                color = MythosTokens.TextSecondary
                            )
                        }
                    }

                    // Card Name
                    Text(
                        text = card.name,
                        style = MythosTypography.CardName.copy(fontSize = 15.sp),
                        color = MythosTokens.PrimaryGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Effect Description
                    Text(
                        text = card.effectDescription,
                        style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                        color = MythosTokens.TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Base Level progression notice
                    Text(
                        text = "Base Level Only • Requires Gold & Shards to upgrade",
                        style = MythosTypography.HeroTitle.copy(fontSize = 9.sp),
                        color = MythosTokens.TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Price & Action Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MythosTokens.BackgroundSurface)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DIRECT UNLOCK",
                        style = MythosTypography.RarityLabel.copy(fontSize = 8.sp),
                        color = MythosTokens.TextMuted
                    )
                    Text(
                        text = priceDisplay,
                        style = MythosTypography.HeroName.copy(fontSize = 15.sp),
                        color = MythosTokens.PrimaryGold
                    )
                }

                if (isOwned) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MythosTokens.Success.copy(alpha = 0.2f))
                            .border(1.dp, MythosTokens.Success, RoundedCornerShape(6.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MythosTokens.Success,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "UNLOCKED",
                            style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                            color = MythosTokens.Success
                        )
                    }
                } else {
                    MythosButton(
                        text = "BUY NOW",
                        onClick = onBuyClick,
                        style = MythosButtonStyle.PRIMARY,
                        icon = Icons.Default.ShoppingCart,
                        testTag = "buy_card_${card.id}",
                        modifier = Modifier
                            .width(130.dp)
                            .height(38.dp)
                    )
                }
            }
        }
    }
}
