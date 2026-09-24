package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.monetization.SummonedCardResult
import com.example.ui.theme.MythosTokens
import com.example.ui.theme.MythosTypography

/**
 * Dialog displaying cards drawn from the Summon Altar (Requirement #9, #10).
 */
@Composable
fun SummonResultDialog(
    results: List<SummonedCardResult>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.5.dp, MythosTokens.PrimaryGold, RoundedCornerShape(16.dp))
                .testTag("summon_result_dialog"),
            color = MythosTokens.Panel
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MythosTokens.PrimaryGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "DIVINE SUMMON COMPLETE",
                            style = MythosTypography.GameTitle.copy(fontSize = 17.sp),
                            color = MythosTokens.PrimaryGold,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        text = "Cards acquired or converted to shards",
                        style = MythosTypography.HeroTitle.copy(fontSize = 11.sp),
                        color = MythosTokens.TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Cards Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (results.size > 1) 2 else 1),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(results) { pull ->
                        val rarityColor = MythosTokens.getRarityColor(pull.card.rarity)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, rarityColor, RoundedCornerShape(10.dp)),
                            colors = CardDefaults.cardColors(containerColor = MythosTokens.PanelElevated)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Miniature Card Frame Preview
                                Box(
                                    modifier = Modifier
                                        .width(70.dp)
                                        .height(100.dp)
                                ) {
                                    CardFrame(
                                        card = pull.card,
                                        isPlayable = true,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Text(
                                    text = pull.card.name,
                                    style = MythosTypography.CardName.copy(fontSize = 12.sp),
                                    color = MythosTokens.PrimaryGold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = pull.card.rarity.label.uppercase(),
                                    style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                    color = rarityColor
                                )

                                if (pull.isDuplicate) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MythosTokens.Buff.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "DUPLICATE: +${pull.shardsConverted} SHARDS",
                                            style = MythosTypography.RarityLabel.copy(fontSize = 8.sp),
                                            color = MythosTokens.Buff,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MythosTokens.Success.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "NEW CARD UNLOCKED!",
                                            style = MythosTypography.RarityLabel.copy(fontSize = 8.sp),
                                            color = MythosTokens.Success,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                pull.triggeredPity?.let { pityText ->
                                    Text(
                                        text = pityText,
                                        style = MythosTypography.RarityLabel.copy(fontSize = 7.5.sp),
                                        color = MythosTokens.MythPowerFlame
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                MythosButton(
                    text = "COLLECT ALL",
                    onClick = onDismiss,
                    style = MythosButtonStyle.PRIMARY,
                    testTag = "dismiss_summon_result_btn",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                )
            }
        }
    }
}
