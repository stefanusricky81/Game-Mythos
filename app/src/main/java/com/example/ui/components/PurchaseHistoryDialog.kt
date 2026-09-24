package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.monetization.PurchaseRecord
import com.example.monetization.PurchaseStatus
import com.example.ui.theme.MythosTokens
import com.example.ui.theme.MythosTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Receipt & Transaction History Modal (Requirement #37).
 */
@Composable
fun PurchaseHistoryDialog(
    history: List<PurchaseRecord>,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MythosTokens.PrimaryGold, RoundedCornerShape(16.dp))
                .testTag("purchase_history_dialog"),
            color = MythosTokens.Panel
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MythosTokens.PrimaryGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "TRANSACTION HISTORY",
                            style = MythosTypography.GameTitle.copy(fontSize = 16.sp),
                            color = MythosTokens.PrimaryGold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MythosTokens.TextMuted
                        )
                    }
                }

                Divider(
                    color = MythosTokens.PanelBorder,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recorded transactions yet.\nPurchases made in the Shop will appear here.",
                            style = MythosTypography.HeroTitle.copy(fontSize = 12.sp),
                            color = MythosTokens.TextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(history) { record ->
                            val statusColor = when (record.status) {
                                PurchaseStatus.PURCHASED -> MythosTokens.Success
                                PurchaseStatus.PENDING -> MythosTokens.Warning
                                PurchaseStatus.CANCELLED -> MythosTokens.TextMuted
                                PurchaseStatus.FAILED -> MythosTokens.Damage
                                PurchaseStatus.REFUNDED -> MythosTokens.Buff
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(0.5.dp, MythosTokens.PanelBorder, RoundedCornerShape(8.dp)),
                                colors = CardDefaults.cardColors(containerColor = MythosTokens.PanelElevated)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = record.rewardSummary.ifEmpty { record.productId },
                                            style = MythosTypography.CardName.copy(fontSize = 13.sp),
                                            color = MythosTokens.PrimaryGold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${record.productType} • ${dateFormat.format(Date(record.timestamp))}",
                                            style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                                            color = MythosTokens.TextMuted
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = record.priceDisplay,
                                            style = MythosTypography.HeroName.copy(fontSize = 13.sp),
                                            color = MythosTokens.TextPrimary
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(statusColor.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = record.status.name,
                                                style = MythosTypography.RarityLabel.copy(fontSize = 8.5.sp),
                                                color = statusColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                MythosButton(
                    text = "CLOSE",
                    onClick = onDismiss,
                    style = MythosButtonStyle.SECONDARY,
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                )
            }
        }
    }
}
