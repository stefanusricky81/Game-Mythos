package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.monetization.BundleItem
import com.example.monetization.GemProduct
import com.example.monetization.PurchaseResult
import com.example.ui.theme.MythosTokens
import com.example.ui.theme.MythosTypography

/**
 * Standard MYTHOS Bundle / Card Purchase Success Dialog (Requirement #23).
 */
@Composable
fun PurchaseSuccessDialog(
    successResult: PurchaseResult.Success,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.5.dp, MythosTokens.PrimaryGold, RoundedCornerShape(16.dp))
                .testTag("purchase_success_dialog"),
            color = MythosTokens.Panel
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Success Badge
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(MythosTokens.Success.copy(alpha = 0.2f))
                        .border(1.dp, MythosTokens.Success, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = MythosTokens.Success,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "PURCHASE COMPLETE",
                    style = MythosTypography.GameTitle.copy(fontSize = 18.sp),
                    color = MythosTokens.PrimaryGold,
                    letterSpacing = 2.sp
                )

                Text(
                    text = successResult.record.priceDisplay,
                    style = MythosTypography.HeroTitle.copy(fontSize = 12.sp),
                    color = MythosTokens.TextSecondary
                )

                Divider(
                    color = MythosTokens.PanelBorder,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Itemized rewards checklist
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MythosTokens.BackgroundSurface)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    successResult.grantedItems.forEach { item ->
                        val itemText = when (item) {
                            is BundleItem.HeroEntitlement -> "${item.name} Legendary Hero ×1"
                            is BundleItem.Gems -> "${item.amount} Myth Gems"
                            is BundleItem.Gold -> "${item.amount} Gold"
                            is BundleItem.Shards -> "${item.amount} ${item.cardName} Shards"
                            is BundleItem.SpecificCard -> item.card.name
                            is BundleItem.RandomCard -> "1x ${item.rarity.label} Card"
                            is BundleItem.Frame -> item.frameName
                            is BundleItem.Avatar -> item.avatarName
                            is BundleItem.Tokens -> "${item.count}x ${item.name}"
                            is BundleItem.Cosmetic -> item.name
                            is BundleItem.MonthlyPassEntitlement -> "Blessing of Olympus (30 Days)"
                            is BundleItem.BattlePassEntitlement -> "Mythos Battle Pass (Season 1)"
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MythosTokens.PrimaryGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = itemText,
                                style = MythosTypography.CardName.copy(fontSize = 13.sp),
                                color = MythosTokens.TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                MythosButton(
                    text = "CONTINUE",
                    onClick = onDismiss,
                    style = MythosButtonStyle.PRIMARY,
                    testTag = "continue_purchase_button",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                )
            }
        }
    }
}

/**
 * Dedicated Myth Gem Purchase Success Dialog (Requirement #24).
 * Features subtle golden radiance and live balance update.
 */
@Composable
fun GemPurchaseSuccessDialog(
    product: GemProduct,
    currentBalance: Int,
    onDismiss: () -> Unit
) {
    // Subtle pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "gem_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gem_scale"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.5.dp, MythosTokens.PrimaryGold, RoundedCornerShape(16.dp))
                .testTag("gem_purchase_success_dialog"),
            color = MythosTokens.Panel
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Subtle radiant gem circle
                Box(
                    modifier = Modifier
                        .scale(pulseScale)
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    MythosTokens.PrimaryGold.copy(alpha = 0.35f),
                                    MythosTokens.DivineBlue.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(1.5.dp, MythosTokens.PrimaryGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Diamond,
                        contentDescription = "Gems Acquired",
                        tint = MythosTokens.LightGold,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Text(
                    text = "MYTH GEMS ACQUIRED",
                    style = MythosTypography.GameTitle.copy(fontSize = 18.sp),
                    color = MythosTokens.PrimaryGold,
                    letterSpacing = 2.sp
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MythosTokens.BackgroundSurface)
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "+${product.totalGems}",
                        style = MythosTypography.GameTitle.copy(fontSize = 28.sp),
                        color = MythosTokens.LightGold
                    )
                    Text(
                        text = "MYTH GEMS",
                        style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                        color = MythosTokens.PrimaryGold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Current Balance: $currentBalance Gems",
                        style = MythosTypography.HeroTitle.copy(fontSize = 12.sp),
                        color = MythosTokens.DivineBlueLight
                    )
                }

                MythosButton(
                    text = "CONTINUE",
                    onClick = onDismiss,
                    style = MythosButtonStyle.PRIMARY,
                    testTag = "continue_gem_button",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                )
            }
        }
    }
}
