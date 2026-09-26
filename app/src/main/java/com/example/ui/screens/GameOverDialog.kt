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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.BattleRewards
import com.example.data.BattleStats
import com.example.data.HerculesIdentity
import com.example.data.StageVictoryResult
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun GameOverDialog(
    isVictory: Boolean,
    stats: BattleStats,
    rewards: BattleRewards,
    campaignVictoryResult: StageVictoryResult? = null,
    onBattleAgain: () -> Unit,
    onGoHome: () -> Unit,
    onGoToCollection: (() -> Unit)? = null
) {
    val numberFormat = NumberFormat.getNumberInstance(Locale.US)
    val title = if (isVictory) "VICTORY" else "DEFEAT"
    val stageSubtitle = when {
        !isVictory -> "Ares' unrelenting fury overwhelmed Hercules in battle."
        campaignVictoryResult != null -> {
            if (campaignVictoryResult.isBoss) {
                "WRATH OF OLYMPUS CONQUERED — THE GOD OF WAR BOWS"
            } else {
                "STAGE ${campaignVictoryResult.stageNumber}: ${campaignVictoryResult.stageName.uppercase()}"
            }
        }
        else -> "Hercules has vanquished Ares in Mount Olympus!"
    }
    val primaryColor = if (isVictory) MythosGoldPrimary else MythosRed

    Dialog(onDismissRequest = {}) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
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
                // Hercules Character Pose (Victory vs Defeat)
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
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stageSubtitle,
                    color = if (campaignVictoryResult?.isBoss == true) MythosRed else Color(0xFFC7C1D4),
                    fontSize = 12.sp,
                    fontWeight = if (campaignVictoryResult?.isBoss == true) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                )

                // CAMPAIGN STAR RATING BAR (Requirement #1 & #2)
                if (isVictory && campaignVictoryResult != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1C172B))
                            .border(1.dp, MythosGoldDark, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (starNum in 1..3) {
                                val isEarned = starNum <= campaignVictoryResult.starsEarned
                                Icon(
                                    imageVector = if (isEarned) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Star $starNum",
                                    tint = if (isEarned) MythosGoldPrimary else Color(0xFF4B435C),
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val ratingLabel = when (campaignVictoryResult.starsEarned) {
                            3 -> "★★★ MASTER CLEAR"
                            2 -> "★★ HEROIC CLEAR"
                            else -> "★ TRIAL CLEAR"
                        }
                        Text(
                            text = ratingLabel,
                            color = MythosGoldPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )

                        if (campaignVictoryResult.starsEarned > campaignVictoryResult.previousStars && campaignVictoryResult.previousStars > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MythosTokens.Success.copy(alpha = 0.2f))
                                    .border(0.5.dp, MythosTokens.Success, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "✦ NEW STAR RECORD! ✦",
                                    color = MythosTokens.Success,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // PERFORMANCE CONDITIONS ACHIEVED (Requirement #2)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F0D16))
                            .border(0.5.dp, Color(0xFF2C263B), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "STAR CONDITIONS",
                            color = MythosGoldPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )

                        // Condition 1: Victory
                        StarConditionRow(
                            starIndex = 1,
                            title = "Stage Victory",
                            detail = "Defeat the opposing champion",
                            isAchieved = true
                        )

                        // Condition 2: Finish HP > 50%
                        StarConditionRow(
                            starIndex = 2,
                            title = "Heroic Resilience",
                            detail = "Finish battle with > 50% HP (${campaignVictoryResult.hpPercentage}% HP left)",
                            isAchieved = campaignVictoryResult.hpConditionMet
                        )

                        // Condition 3: Turns <= configured limit
                        StarConditionRow(
                            starIndex = 3,
                            title = "Swift Victory",
                            detail = "Complete in ≤ ${campaignVictoryResult.maxTurnsAllowed} turns (${campaignVictoryResult.turnsCount} turns taken)",
                            isAchieved = campaignVictoryResult.turnsConditionMet
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // REWARDS BOX (Requirement #2, #3: First Clear vs Replay)
                if (isVictory) {
                    val isFirstClear = campaignVictoryResult?.isFirstClear ?: true
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1B1626))
                            .border(1.dp, MythosGoldDark, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isFirstClear) "FIRST CLEAR REWARDS" else "REPLAY REWARDS",
                                color = MythosGoldPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )

                            if (isFirstClear) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MythosTokens.PrimaryGold.copy(alpha = 0.2f))
                                        .border(0.5.dp, MythosTokens.PrimaryGold, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "FIRST CLEAR",
                                        color = MythosTokens.PrimaryGold,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF2A2438))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "REPLAY BOUNTY",
                                        color = Color(0xFFAFA7BD),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Gold & XP badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val goldAmt = campaignVictoryResult?.goldAwarded ?: rewards.gold
                            val xpAmt = campaignVictoryResult?.xpAwarded ?: rewards.xp
                            RewardBadge(icon = Icons.Default.MonetizationOn, label = "+${numberFormat.format(goldAmt)} Gold", color = MythPowerGold)
                            RewardBadge(icon = Icons.Default.Star, label = "+${numberFormat.format(xpAmt)} XP", color = Color(0xFF38BDF8))
                        }

                        // Hero XP & Hero Shards badges (Phase 7C Sections 11, 12)
                        if (campaignVictoryResult != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (campaignVictoryResult.heroXpAwarded > 0) {
                                    RewardBadge(
                                        icon = Icons.Default.Shield,
                                        label = "+${numberFormat.format(campaignVictoryResult.heroXpAwarded)} Hero XP",
                                        color = MythosTokens.DivineBlueLight
                                    )
                                }
                                if (campaignVictoryResult.heroShardsAwarded > 0) {
                                    val shardName = if (campaignVictoryResult.heroShardsHeroId == HerculesIdentity.HERO_ID) "Hercules" else "Hero"
                                    RewardBadge(
                                        icon = Icons.Default.Diamond,
                                        label = "+${campaignVictoryResult.heroShardsAwarded} $shardName Shards",
                                        color = MythosTokens.PrimaryGold
                                    )
                                }
                            }
                        }

                        // HERO UNLOCKED COMPACT NOTIFICATION (Phase 7C Section 16)
                        if (campaignVictoryResult?.heroUnlocked != null) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.5.dp, MythosGoldPrimary, RoundedCornerShape(8.dp))
                                    .testTag("hero_unlocked_notification"),
                                color = Color(0xFF231C30)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MythosGoldPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "HERO UNLOCKED",
                                            color = MythosGoldPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = campaignVictoryResult.heroUnlocked.uppercase(),
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Card Reward (Only for First Clear or when card is awarded)
                        if (isFirstClear && !campaignVictoryResult?.cardNameAwarded.isNullOrBlank()) {
                            val cardName = campaignVictoryResult?.cardNameAwarded ?: rewards.cardRewardName
                            val cardRarity = campaignVictoryResult?.cardRarityAwarded ?: rewards.cardRewardRarity
                            val rarityColor = MythosTokens.getRarityColor(cardRarity)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF110E18))
                                    .border(0.5.dp, rarityColor, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = rarityColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (campaignVictoryResult?.wasDuplicateCard == true)
                                            "DUPLICATE CONVERTED TO +${campaignVictoryResult.shardsAwardedForDuplicate} SHARDS"
                                        else
                                            "CARD REWARD UNLOCKED",
                                        color = rarityColor,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = cardName,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else if (!isFirstClear && campaignVictoryResult != null) {
                            Text(
                                text = "First-clear card reward already claimed. Replay grants standard bounty.",
                                color = Color(0xFFA59EB3),
                                fontSize = 10.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Battle Statistics Report (Requirement #2)
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
                        StatRow("Total Damage Dealt", numberFormat.format(stats.totalDamageDealt))
                        StatRow("Total Damage Absorbed", numberFormat.format(stats.totalDamageTaken))
                        if (campaignVictoryResult != null) {
                            StatRow(
                                "Remaining HP",
                                "${numberFormat.format(campaignVictoryResult.playerRemainingHp)} / ${numberFormat.format(campaignVictoryResult.playerMaxHp)} (${campaignVictoryResult.hpPercentage}%)"
                            )
                        }
                        StatRow("Twelve Labors Unleashed", "${stats.ultimateUses}")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons: BATTLE AGAIN / RETRY, COLLECTION, HOME
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
                            text = if (isVictory) {
                                if (campaignVictoryResult != null) "REPLAY STAGE" else "BATTLE AGAIN"
                            } else {
                                "RETRY BATTLE"
                            },
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
                            text = if (campaignVictoryResult != null) "RETURN TO CAMPAIGN" else "HOME",
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
private fun StarConditionRow(
    starIndex: Int,
    title: String,
    detail: String,
    isAchieved: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (isAchieved) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                tint = if (isAchieved) MythosGoldPrimary else Color(0xFF5A526E),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = title,
                    color = if (isAchieved) Color.White else Color(0xFF8E869E),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = detail,
                    color = Color(0xFF756D84),
                    fontSize = 9.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (isAchieved) MythosTokens.Success.copy(alpha = 0.2f) else Color(0x33444444))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (isAchieved) "ACHIEVED" else "MISSED",
                color = if (isAchieved) MythosTokens.Success else Color(0xFF888888),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RewardBadge(icon: ImageVector, label: String, color: Color) {
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
