package com.example.ui.components

import androidx.compose.animation.*
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
import androidx.compose.runtime.*
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
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import com.example.monetization.PlayerEconomyRepository
import com.example.monetization.PlayerEconomyState
import com.example.ui.theme.MythosTokens
import com.example.ui.theme.MythosTypography
import java.text.NumberFormat
import java.util.Locale

/**
 * Commercial-quality Hero Detail & Progression Upgrade Dialog (Phase 7C Section 10).
 * Displays:
 * - Hero Portrait, Name, Title, Faction, Rarity
 * - Level Progress (Level 1-5)
 * - XP Bar and exact XP progression
 * - Dedicated Hero Shards count
 * - Current level combat stats (HP, ATK, DEF)
 * - Next level preview with stat increases
 * - Atomic Upgrade button with Gold and Hero Shard costs, or explicit missing resources
 * - Passive and Ultimate abilities
 * - Equip / Select Hero action
 */
@Composable
fun HeroDetailUpgradeDialog(
    heroDef: HeroDefinition,
    economyState: PlayerEconomyState,
    onSelectHero: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }
    val progress = economyState.getHeroProgress(heroDef.id)
    val isSelected = economyState.selectedHeroId == heroDef.id
    val isUnlocked = progress.isUnlocked
    val currentLevel = progress.level
    val isMaxLevel = progress.isMaxLevel

    // Current & Next level scaled stats from centralized configuration
    val currentStats = HeroProgressionConfig.getScaledStats(heroDef, currentLevel)
    val nextStats = if (!isMaxLevel) HeroProgressionConfig.getScaledStats(heroDef, currentLevel + 1) else null
    val upgradeCost = progress.nextLevelUpgradeCost

    // Resource validation
    val hasEnoughGold = upgradeCost != null && economyState.gold >= upgradeCost.goldCost
    val hasEnoughShards = upgradeCost != null && progress.currentShards >= upgradeCost.shardCost
    val canUpgrade = isUnlocked && !isMaxLevel && hasEnoughGold && hasEnoughShards

    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isUpgradeSuccess by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.5.dp, MythosTokens.PrimaryGold, RoundedCornerShape(20.dp))
                .testTag("hero_detail_dialog"),
            color = MythosTokens.BackgroundSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MythosTokens.PrimaryGold.copy(alpha = 0.2f))
                            .border(1.dp, MythosTokens.PrimaryGold, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = heroDef.rarity.name,
                            style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                            color = MythosTokens.PrimaryGold
                        )
                    }

                    Text(
                        text = heroDef.faction.uppercase(),
                        style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                        color = MythosTokens.TextMuted
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("close_hero_detail_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MythosTokens.TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Hero Portrait + Identity
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            2.dp,
                            if (isUnlocked) MythosTokens.PrimaryGold else MythosTokens.PanelBorder,
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    Image(
                        painter = painterResource(id = heroDef.portraitResId),
                        contentDescription = heroDef.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = if (isUnlocked) 1f else 0.45f
                    )
                    if (!isUnlocked) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = MythosTokens.TextMuted,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = heroDef.name.uppercase(),
                    style = MythosTypography.HeroName.copy(fontSize = 20.sp),
                    color = if (isUnlocked) MythosTokens.PrimaryGold else MythosTokens.TextMuted
                )
                Text(
                    text = heroDef.title,
                    style = MythosTypography.HeroTitle.copy(fontSize = 12.sp),
                    color = MythosTokens.SecondaryGold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // LEVEL & XP PROGRESSION BOX (Phase 7C Section 3, 10)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MythosTokens.Panel,
                    border = BorderStroke(1.dp, MythosTokens.PanelBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "LEVEL $currentLevel / ${HeroProgressionConfig.MAX_HERO_LEVEL}",
                                    style = MythosTypography.HeroName.copy(fontSize = 14.sp),
                                    color = MythosTokens.PrimaryGold
                                )
                                if (isMaxLevel) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MythosTokens.Success.copy(alpha = 0.2f))
                                            .border(0.5.dp, MythosTokens.Success, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "MAX LEVEL",
                                            style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                            color = MythosTokens.Success
                                        )
                                    }
                                }
                            }

                            // Hero Shards Count
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Diamond,
                                    contentDescription = null,
                                    tint = MythosTokens.SecondaryGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${progress.currentShards}${if (upgradeCost != null) " / ${upgradeCost.shardCost}" else ""} Shards",
                                    style = MythosTypography.CardName.copy(fontSize = 12.sp),
                                    color = MythosTokens.SecondaryGold
                                )
                            }
                        }

                        // XP Bar
                        if (!isMaxLevel) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "HERO XP",
                                        style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                        color = MythosTokens.TextMuted
                                    )
                                    Text(
                                        text = "${numberFormat.format(progress.currentXp)} / ${numberFormat.format(progress.xpRequiredForNextLevel)}",
                                        style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                                        color = MythosTokens.TextPrimary
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { progress.xpProgressFraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MythosTokens.PrimaryGold,
                                    trackColor = MythosTokens.BackgroundSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // STATS COMPARISON BOX (Current vs Next Level)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MythosTokens.Panel,
                    border = BorderStroke(1.dp, MythosTokens.PanelBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "COMBAT STATS",
                                style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                                color = MythosTokens.PrimaryGold
                            )
                            if (nextStats != null) {
                                Text(
                                    text = "NEXT LEVEL (LV ${currentLevel + 1})",
                                    style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                                    color = MythosTokens.Success
                                )
                            }
                        }

                        StatComparisonRow(
                            label = "Health Points (HP)",
                            currentVal = currentStats.hp,
                            nextVal = nextStats?.hp,
                            color = MythosTokens.Success
                        )
                        StatComparisonRow(
                            label = "Attack Power (ATK)",
                            currentVal = currentStats.attack,
                            nextVal = nextStats?.attack,
                            color = MythosTokens.Damage
                        )
                        StatComparisonRow(
                            label = "Defense Shield (DEF)",
                            currentVal = currentStats.defense,
                            nextVal = nextStats?.defense,
                            color = MythosTokens.Shield
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ABILITIES PREVIEW (Passive & Ultimate)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MythosTokens.Panel,
                    border = BorderStroke(1.dp, MythosTokens.PanelBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Passive
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = MythosTokens.PrimaryGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "PASSIVE — ${heroDef.passiveName}",
                                    style = MythosTypography.CardName.copy(fontSize = 11.sp),
                                    color = MythosTokens.PrimaryGold
                                )
                                Text(
                                    text = heroDef.passiveDescription,
                                    style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                                    color = MythosTokens.TextPrimary
                                )
                            }
                        }

                        HorizontalDivider(color = MythosTokens.PanelBorder, thickness = 0.5.dp)

                        // Ultimate
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = MythosTokens.MythPowerFlame,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "ULTIMATE — ${heroDef.ultimateName}",
                                    style = MythosTypography.CardName.copy(fontSize = 11.sp),
                                    color = MythosTokens.MythPowerFlame
                                )
                                Text(
                                    text = heroDef.ultimateDescription,
                                    style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                                    color = MythosTokens.TextPrimary
                                )
                            }
                        }
                    }
                }

                // Feedback Message (Success or Missing Resources)
                feedbackMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isUpgradeSuccess) MythosTokens.Success.copy(alpha = 0.15f)
                                else MythosTokens.Damage.copy(alpha = 0.15f)
                            )
                            .border(
                                1.dp,
                                if (isUpgradeSuccess) MythosTokens.Success else MythosTokens.Damage,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Text(
                            text = msg,
                            color = if (isUpgradeSuccess) MythosTokens.Success else MythosTokens.Damage,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ACTION BUTTONS (UPGRADE & SELECT)
                if (!isUnlocked) {
                    MythosButton(
                        text = "LOCKED — UNLOCK THROUGH CAMPAIGN",
                        subtitle = when (heroDef.id) {
                            "hero_achilles" -> "Clear Stage 2: Wrath of the Arena to unlock"
                            "hero_merlin" -> "Clear Stage 5: Wrath of Olympus to unlock"
                            else -> "Clear World 1 Campaign to unlock"
                        },
                        onClick = {},
                        enabled = false,
                        style = MythosButtonStyle.DISABLED,
                        icon = Icons.Default.Lock,
                        testTag = "hero_locked_dialog_button",
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // UPGRADE BUTTON SECTION (Phase 7C Section 10)
                    if (!isMaxLevel && upgradeCost != null) {
                        val missingGold = maxOf(0, upgradeCost.goldCost - economyState.gold)
                        val missingShards = maxOf(0, upgradeCost.shardCost - progress.currentShards)

                        val buttonSub = when {
                            missingGold > 0 && missingShards > 0 -> "Need ${numberFormat.format(missingGold)} Gold & $missingShards Shards"
                            missingGold > 0 -> "Need ${numberFormat.format(missingGold)} more Gold"
                            missingShards > 0 -> "Need $missingShards more ${heroDef.name} Shards"
                            else -> "Cost: ${numberFormat.format(upgradeCost.goldCost)} Gold • ${upgradeCost.shardCost} Shards"
                        }

                        MythosButton(
                            text = "UPGRADE TO LEVEL ${currentLevel + 1}",
                            subtitle = buttonSub,
                            onClick = {
                                val result = PlayerEconomyRepository.instance.upgradeHero(heroDef.id)
                                if (result.isSuccess) {
                                    isUpgradeSuccess = true
                                    feedbackMessage = "${heroDef.name} successfully ascended to Level ${currentLevel + 1}!"
                                } else {
                                    isUpgradeSuccess = false
                                    feedbackMessage = result.exceptionOrNull()?.message ?: "Upgrade failed."
                                }
                            },
                            enabled = canUpgrade,
                            style = if (canUpgrade) MythosButtonStyle.PRIMARY else MythosButtonStyle.DISABLED,
                            icon = Icons.Default.Upgrade,
                            testTag = "upgrade_hero_button",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // EQUIP / SELECT HERO BUTTON
                    if (isSelected) {
                        MythosButton(
                            text = "ACTIVE CHAMPION",
                            subtitle = "Currently equipped in Active Deck",
                            onClick = {},
                            enabled = false,
                            style = MythosButtonStyle.SECONDARY,
                            icon = Icons.Default.CheckCircle,
                            testTag = "hero_already_selected_button",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        )
                    } else {
                        MythosButton(
                            text = "SELECT AS ACTIVE CHAMPION",
                            subtitle = "Equip ${heroDef.name} and update active deck",
                            onClick = onSelectHero,
                            style = MythosButtonStyle.SECONDARY,
                            icon = Icons.Default.Check,
                            testTag = "select_hero_from_dialog_button",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatComparisonRow(
    label: String,
    currentVal: Int,
    nextVal: Int?,
    color: Color
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
            color = MythosTokens.TextPrimary
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = numberFormat.format(currentVal),
                style = MythosTypography.CardName.copy(fontSize = 12.sp),
                color = color
            )
            if (nextVal != null && nextVal > currentVal) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "→",
                    color = MythosTokens.TextMuted,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${numberFormat.format(nextVal)} (+${nextVal - currentVal})",
                    style = MythosTypography.CardName.copy(fontSize = 12.sp),
                    color = MythosTokens.Success
                )
            }
        }
    }
}
