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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CardCatalog
import com.example.data.CardRarity
import com.example.data.HeroCatalog
import com.example.data.PlayerProgressionConfig
import com.example.monetization.PlayerEconomyRepository
import com.example.ui.components.MythosButton
import com.example.ui.components.MythosButtonStyle
import com.example.ui.theme.MythosTokens
import com.example.ui.theme.MythosTypography
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProfileScreen(
    onNavigateBack: () -> Unit,
    onOpenAlliance: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val economyState by PlayerEconomyRepository.instance.economyState.collectAsState()
    val progress = economyState.playerProgress
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }
    val selectedHero = remember(economyState.selectedHeroId) {
        HeroCatalog.findHero(economyState.selectedHeroId) ?: HeroCatalog.HERCULES
    }

    val highestRarity = remember(economyState.ownedCardIds) {
        val ownedCards = economyState.ownedCardIds.mapNotNull { CardCatalog.findCard(it) }
        when {
            ownedCards.any { it.rarity == CardRarity.MYTHIC } -> CardRarity.MYTHIC
            ownedCards.any { it.rarity == CardRarity.EPIC } -> CardRarity.EPIC
            ownedCards.any { it.rarity == CardRarity.RARE } -> CardRarity.RARE
            ownedCards.any { it.rarity == CardRarity.UNCOMMON } -> CardRarity.UNCOMMON
            else -> CardRarity.COMMON
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MythosTokens.Background)
    ) {
        // Background art overlay
        Image(
            painter = painterResource(id = R.drawable.img_battlefield_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.2f
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "PLAYER PROFILE",
                            style = MythosTypography.GameTitle.copy(fontSize = 18.sp),
                            color = MythosTokens.PrimaryGold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("profile_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MythosTokens.PrimaryGold
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MythosTokens.Panel.copy(alpha = 0.95f)
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Banner: Champion Avatar + Player Level & XP
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MythosTokens.PrimaryGold, RoundedCornerShape(16.dp))
                        .testTag("player_profile_card"),
                    colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Avatar Box
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, MythosTokens.PrimaryGold, CircleShape)
                                    .background(MythosTokens.BackgroundSurface)
                            ) {
                                Image(
                                    painter = painterResource(id = selectedHero.portraitResId),
                                    contentDescription = selectedHero.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "CHAMPION OF OLYMPUS",
                                        style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                                        color = MythosTokens.PrimaryGold,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Text(
                                    text = "Player Level ${progress.playerLevel}",
                                    style = MythosTypography.HeroName.copy(fontSize = 20.sp),
                                    color = Color.White
                                )

                                Text(
                                    text = "Active Hero: ${selectedHero.name}",
                                    style = MythosTypography.HeroTitle.copy(fontSize = 12.sp)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Login Streak Indicator (Phase 8 Section 6)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF261A10))
                                        .border(0.5.dp, MythosTokens.PrimaryGold, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(text = "🔥", fontSize = 11.sp)
                                    Text(
                                        text = "${progress.loginStreak}-Day Streak (Best: ${progress.highestLoginStreak})",
                                        style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                        color = MythosTokens.PrimaryGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // XP Bar & Next Level Target (Phase 7D Section 4, 7)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PLAYER XP",
                                    style = MythosTypography.RarityLabel.copy(fontSize = 11.sp),
                                    color = MythosTokens.PrimaryGold
                                )
                                Text(
                                    text = if (progress.isMaxLevel) "MAX LEVEL"
                                    else "${numberFormat.format(progress.playerXp)} / ${numberFormat.format(progress.xpRequiredForNextLevel)} XP",
                                    style = MythosTypography.CardDescription.copy(fontSize = 12.sp),
                                    color = Color(0xFF38BDF8)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E182A))
                                    .border(1.dp, MythosTokens.PanelBorder, RoundedCornerShape(6.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(progress.xpProgressFraction)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFF0284C7), Color(0xFF38BDF8), MythosTokens.PrimaryGold)
                                            )
                                        )
                                )
                            }
                        }
                    }
                }

                // Inventory & Account Overview (Phase 7D Section 7)
                Text(
                    text = "RESOURCES & ASSETS",
                    style = MythosTypography.RarityLabel.copy(fontSize = 12.sp),
                    color = MythosTokens.PrimaryGold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ResourceCard(
                        modifier = Modifier.weight(1f),
                        icon = "🪙",
                        label = "GOLD",
                        value = numberFormat.format(economyState.gold),
                        color = MythosTokens.PrimaryGold
                    )
                    ResourceCard(
                        modifier = Modifier.weight(1f),
                        icon = "💎",
                        label = "MYTH GEMS",
                        value = numberFormat.format(economyState.mythGems),
                        color = MythosTokens.DivineBlueLight
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ResourceCard(
                        modifier = Modifier.weight(1f),
                        icon = "🛡️",
                        label = "HEROES",
                        value = "${economyState.ownedHeroIds.size} / ${HeroCatalog.ALL_HEROES.size}",
                        color = Color(0xFFA78BFA)
                    )
                    ResourceCard(
                        modifier = Modifier.weight(1f),
                        icon = "🃏",
                        label = "CARDS",
                        value = "${economyState.ownedCardIds.size} / ${CardCatalog.ALL_CARDS.size}",
                        color = MythosTokens.Epic
                    )
                    ResourceCard(
                        modifier = Modifier.weight(1f),
                        icon = "✨",
                        label = "TOP RARITY",
                        value = highestRarity.label,
                        color = when (highestRarity) {
                            CardRarity.MYTHIC -> MythosTokens.Legendary
                            CardRarity.EPIC -> MythosTokens.Epic
                            CardRarity.RARE -> MythosTokens.DivineBlueLight
                            CardRarity.UNCOMMON -> MythosTokens.Success
                            CardRarity.COMMON -> MythosTokens.TextMuted
                        }
                    )
                }

                // Alliance Membership & Contribution Card (Phase 8 Section 6)
                Text(
                    text = "ALLIANCE AFFILIATION",
                    style = MythosTypography.RarityLabel.copy(fontSize = 12.sp),
                    color = MythosTokens.PrimaryGold
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MythosTokens.PanelBorder, RoundedCornerShape(12.dp))
                        .testTag("profile_alliance_card"),
                    colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (progress.allianceId != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(text = "🏛️", fontSize = 24.sp)
                                    Column {
                                        Text(
                                            text = progress.allianceName ?: "Olympus Alliance",
                                            style = MythosTypography.HeroName.copy(fontSize = 15.sp),
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Role: ${progress.allianceRole ?: "Member"}",
                                            style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                                            color = MythosTokens.PrimaryGold
                                        )
                                    }
                                }

                                Button(
                                    onClick = onOpenAlliance,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MythosTokens.PanelElevated,
                                        contentColor = MythosTokens.PrimaryGold
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, MythosTokens.PrimaryGold),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("profile_view_alliance_btn")
                                ) {
                                    Text("VIEW", style = MythosTypography.RarityLabel.copy(fontSize = 10.sp))
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MythosTokens.PanelElevated)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "All-Time Contribution",
                                    style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                                    color = MythosTokens.TextMuted
                                )
                                Text(
                                    text = "⭐ ${numberFormat.format(progress.allianceContribution)} pts",
                                    style = MythosTypography.HeroName.copy(fontSize = 12.sp),
                                    color = MythosTokens.SecondaryGold
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "No Alliance Joined",
                                        style = MythosTypography.HeroName.copy(fontSize = 14.sp),
                                        color = MythosTokens.TextMuted
                                    )
                                    Text(
                                        text = "Join or found an Alliance to earn bonuses and raid together.",
                                        style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                                        color = Color(0xFFC0B8D0)
                                    )
                                }

                                Button(
                                    onClick = onOpenAlliance,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MythosTokens.PrimaryGold,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("profile_join_alliance_btn")
                                ) {
                                    Text("JOIN", style = MythosTypography.RarityLabel.copy(fontSize = 10.sp), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Real persistent Statistics Section (Phase 7D Section 7, 24)
                Text(
                    text = "LIFETIME STATISTICS",
                    style = MythosTypography.RarityLabel.copy(fontSize = 12.sp),
                    color = MythosTokens.PrimaryGold
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MythosTokens.PanelBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatRow(
                            icon = Icons.Default.SportsMartialArts,
                            title = "Total Battles",
                            value = numberFormat.format(progress.totalBattles)
                        )
                        HorizontalDivider(color = MythosTokens.PanelBorder)

                        StatRow(
                            icon = Icons.Default.EmojiEvents,
                            title = "Victories",
                            value = "${numberFormat.format(progress.totalVictories)} (${progress.winRatePercent}%)"
                        )
                        HorizontalDivider(color = MythosTokens.PanelBorder)

                        StatRow(
                            icon = Icons.Default.Close,
                            title = "Defeats",
                            value = numberFormat.format(progress.totalDefeats)
                        )
                        HorizontalDivider(color = MythosTokens.PanelBorder)

                        StatRow(
                            icon = Icons.Default.Layers,
                            title = "Cards Played",
                            value = numberFormat.format(progress.totalCardsPlayed)
                        )
                        HorizontalDivider(color = MythosTokens.PanelBorder)

                        StatRow(
                            icon = Icons.Default.Whatshot,
                            title = "Damage Dealt",
                            value = numberFormat.format(progress.totalDamageDealt)
                        )
                        HorizontalDivider(color = MythosTokens.PanelBorder)

                        StatRow(
                            icon = Icons.Default.Security,
                            title = "Damage Absorbed / Taken",
                            value = numberFormat.format(progress.totalDamageTaken)
                        )
                        HorizontalDivider(color = MythosTokens.PanelBorder)

                        StatRow(
                            icon = Icons.Default.Flag,
                            title = "Campaign Stages Cleared",
                            value = "${progress.totalCampaignStagesCleared} Stages"
                        )
                    }
                }

                // Level Rewards Roadmap Section (Phase 7D Section 6)
                Text(
                    text = "LEVEL MILESTONES & REWARDS",
                    style = MythosTypography.RarityLabel.copy(fontSize = 12.sp),
                    color = MythosTokens.PrimaryGold
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlayerProgressionConfig.LEVEL_REWARDS.values.take(6).forEach { reward ->
                        val isReached = progress.playerLevel >= reward.level
                        val isClaimed = progress.claimedLevelRewards.contains(reward.level)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    0.5.dp,
                                    if (isReached) MythosTokens.PrimaryGold else MythosTokens.PanelBorder,
                                    RoundedCornerShape(10.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isReached) MythosTokens.PanelElevated else MythosTokens.Panel
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isReached) MythosTokens.PrimaryGold.copy(alpha = 0.2f)
                                                else Color(0xFF261F33)
                                            )
                                            .border(
                                                1.dp,
                                                if (isReached) MythosTokens.PrimaryGold else MythosTokens.PanelBorder,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${reward.level}",
                                            style = MythosTypography.HeroName.copy(fontSize = 13.sp),
                                            color = if (isReached) MythosTokens.PrimaryGold else MythosTokens.TextMuted
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = "Level ${reward.level} Milestone",
                                            style = MythosTypography.CardName.copy(fontSize = 13.sp),
                                            color = if (isReached) Color.White else MythosTokens.TextMuted
                                        )
                                        Text(
                                            text = reward.description,
                                            style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                                            color = MythosTokens.SecondaryGold
                                        )
                                    }
                                }

                                if (isClaimed) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MythosTokens.Success.copy(alpha = 0.2f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "CLAIMED",
                                            style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                            color = MythosTokens.Success
                                        )
                                    }
                                } else if (isReached) {
                                    Button(
                                        onClick = {
                                            PlayerEconomyRepository.instance.claimPlayerLevelReward(reward.level)
                                        },
                                        modifier = Modifier
                                            .height(30.dp)
                                            .testTag("claim_level_reward_${reward.level}"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MythosTokens.PrimaryGold,
                                            contentColor = Color.Black
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                    ) {
                                        Text(
                                            text = "CLAIM",
                                            style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF261F33))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "LOCKED",
                                            style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                            color = MythosTokens.TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ResourceCard(
    modifier: Modifier = Modifier,
    icon: String,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(0.5.dp, MythosTokens.PanelBorder, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = icon, fontSize = 16.sp)
            Text(
                text = value,
                style = MythosTypography.HeroName.copy(fontSize = 14.sp),
                color = color
            )
            Text(
                text = label,
                style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                color = MythosTokens.TextMuted
            )
        }
    }
}

@Composable
private fun StatRow(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MythosTokens.SecondaryGold,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                style = MythosTypography.CardDescription.copy(fontSize = 13.sp),
                color = Color(0xFFD4CCE6)
            )
        }
        Text(
            text = value,
            style = MythosTypography.HeroName.copy(fontSize = 13.sp),
            color = Color.White
        )
    }
}
