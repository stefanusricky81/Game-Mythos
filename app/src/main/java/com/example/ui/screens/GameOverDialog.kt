package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.BattleRewards
import com.example.data.BattleStats
import com.example.data.HerculesIdentity
import com.example.ui.theme.*

@Composable
fun GameOverDialog(
    isVictory: Boolean,
    stats: BattleStats,
    rewards: BattleRewards,
    onBattleAgain: () -> Unit,
    onGoHome: () -> Unit,
    onGoToCollection: (() -> Unit)? = null
) {
    val title = if (isVictory) "VICTORY" else "DEFEAT"
    val subtitle = if (isVictory) "Hercules has vanquished Ares in Mount Olympus!" else "Ares' unrelenting fury overwhelmed Hercules."
    val primaryColor = if (isVictory) MythosGoldPrimary else MythosRed

    Dialog(onDismissRequest = {}) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(20.dp))
                .border(2.dp, primaryColor, RoundedCornerShape(20.dp)),
            color = Color(0xFF13101B)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hercules Character Pose (Victory vs Defeat - Section 4H/4I)
                val heroPoseResId = if (isVictory) {
                    HerculesIdentity.assets.victory.resolveResId()
                } else {
                    HerculesIdentity.assets.defeat.resolveResId()
                }

                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.5.dp, primaryColor, RoundedCornerShape(12.dp))
                ) {
                    Image(
                        painter = painterResource(id = heroPoseResId),
                        contentDescription = HerculesIdentity.NAME,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Title
                Text(
                    text = title,
                    color = primaryColor,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = subtitle,
                    color = Color(0xFFC7C1D4),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )

                // VICTORY REWARDS BOX (Section 12)
                if (isVictory) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1B1626))
                            .border(1.dp, MythosGoldDark, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "BATTLE REWARDS EARNED",
                            color = MythosGoldPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RewardBadge(icon = Icons.Default.MonetizationOn, label = "+${rewards.gold} Gold", color = MythPowerGold)
                            RewardBadge(icon = Icons.Default.Star, label = "+${rewards.xp} XP", color = Color(0xFF38BDF8))
                        }

                        // Card Reward
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF110E18))
                                .border(0.5.dp, RarityEpic, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = RarityEpic,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "CARD REWARD UNLOCKED",
                                    color = RarityEpic,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = rewards.cardRewardName,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Battle Report
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0C0A12))
                        .border(0.5.dp, Color(0xFF2C263B), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "BATTLE STATISTICS",
                            color = Color(0xFFA59EB3),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )

                        StatRow("Turns Taken", "${stats.turnsCount}")
                        StatRow("Cards Played", "${stats.cardsPlayedCount}")
                        StatRow("Total Damage Dealt", "${stats.totalDamageDealt}")
                        StatRow("Total Damage Absorbed", "${stats.totalDamageTaken}")
                        StatRow("Twelve Labors Unleashed", "${stats.ultimateUses}")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons: BATTLE AGAIN / RETRY and HOME
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onBattleAgain,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag(if (isVictory) "battle_again_button" else "retry_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = if (isVictory) Color(0xFF1F1500) else Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isVictory) "BATTLE AGAIN" else "RETRY",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    if (isVictory && onGoToCollection != null) {
                        Button(
                            onClick = onGoToCollection,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("game_over_collection_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2A233D),
                                contentColor = MythosGoldPrimary
                            ),
                            border = BorderStroke(1.dp, MythosGoldPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Layers, contentDescription = null, tint = MythosGoldPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "VIEW REWARDS IN COLLECTION",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onGoHome,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("game_over_home_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFBBB5C7)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF383248)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Home, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "HOME",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RewardBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.5.dp, color, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, color = color, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFFA19AB6),
            fontSize = 11.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
