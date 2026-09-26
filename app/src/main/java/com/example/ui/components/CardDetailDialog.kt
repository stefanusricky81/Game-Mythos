package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Card
import com.example.data.CardRarity
import com.example.data.Hero
import com.example.ui.theme.*

@Composable
fun CardDetailDialog(
    card: Card,
    hero: Hero,
    canPlay: Boolean,
    onPlay: () -> Unit,
    onDismiss: () -> Unit
) {
    val rarityColor = MythosTokens.getRarityColor(card.rarity)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, rarityColor, RoundedCornerShape(16.dp)),
            color = Color(0xFF13111C)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(rarityColor.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${card.rarity.label.uppercase()} • ${card.type.label.uppercase()} • LV.${card.level}",
                            color = rarityColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Card Art Showcase
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(rarityColor.copy(alpha = 0.3f), Color(0xFF0C0A12))
                            )
                        )
                        .border(1.dp, rarityColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    CardArtwork(
                        card = card,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Name & Cost
                Text(
                    text = card.name,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = "Cost: ${card.cost} Energy • Level ${card.level}/${card.maxLevel}",
                    color = EnergyCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Effect Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0B0A11))
                        .border(0.5.dp, Color(0xFF2C273D), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "CARD EFFECT:",
                            color = MythosGoldPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = card.effectDescription,
                            color = Color(0xFFEBE6F3),
                            fontSize = 13.sp,
                            lineHeight = 17.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Lore Quote Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = "\"${card.loreQuote}\"",
                        color = Color(0xFFA19BAE),
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFBBB5C7)),
                        border = BorderStroke(1.dp, Color(0xFF3B354C)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("BACK")
                    }

                    Button(
                        onClick = {
                            onPlay()
                            onDismiss()
                        },
                        enabled = canPlay,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MythosGoldPrimary,
                            contentColor = Color(0xFF1B1300),
                            disabledContainerColor = Color(0xFF2B2636),
                            disabledContentColor = Color(0xFF6E687A)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (canPlay) "PLAY CARD" else "NEED ENERGY")
                    }
                }
            }
        }
    }
}
