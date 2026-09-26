package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.HeroCatalog
import com.example.data.HeroDefinition
import com.example.monetization.PlayerEconomyRepository
import com.example.ui.components.HeroDetailUpgradeDialog
import com.example.ui.components.MythosButton
import com.example.ui.components.MythosButtonStyle
import com.example.data.HeroProgressionConfig
import com.example.ui.theme.MythosTokens
import com.example.ui.theme.MythosTypography
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HeroSelectionScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val economyState by PlayerEconomyRepository.instance.economyState.collectAsState()
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }
    val heroes = HeroCatalog.SELECTION_HEROES

    // Currently viewed hero in the selection carousel (defaults to selected hero)
    var viewedHeroId by remember {
        mutableStateOf(economyState.selectedHeroId)
    }
    var showHeroDetailDialog by remember {
        mutableStateOf(false)
    }

    val currentHero = heroes.find { it.id == viewedHeroId } ?: HeroCatalog.HERCULES
    val heroProgress = economyState.getHeroProgress(currentHero.id)
    val isOwned = heroProgress.isUnlocked
    val isCurrentlySelected = economyState.selectedHeroId == currentHero.id
    val scaledStats = HeroProgressionConfig.getScaledStats(currentHero, heroProgress.level)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MythosTokens.Background)
    ) {
        // Atmospheric Olympus Background
        Image(
            painter = painterResource(id = R.drawable.img_battlefield_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.20f
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. TOP APP BAR
            Surface(
                color = MythosTokens.Panel,
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .testTag("hero_selection_back_button")
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MythosTokens.PanelElevated)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MythosTokens.PrimaryGold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "HERO SELECTION",
                                style = MythosTypography.GameSubtitle.copy(fontSize = 17.sp),
                                color = MythosTokens.PrimaryGold,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = "Choose your champion of Olympus",
                                style = MythosTypography.HeroTitle.copy(fontSize = 11.sp),
                                color = MythosTokens.TextMuted
                            )
                        }
                    }

                    // Currencies status
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(MythosTokens.PanelElevated)
                            .border(1.dp, MythosTokens.PrimaryGold.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(text = "🪙", fontSize = 11.sp)
                            Text(
                                text = numberFormat.format(economyState.gold),
                                style = MythosTypography.HeroName.copy(fontSize = 11.sp),
                                color = MythosTokens.PrimaryGold
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(text = "💎", fontSize = 11.sp)
                            Text(
                                text = numberFormat.format(economyState.mythGems),
                                style = MythosTypography.HeroName.copy(fontSize = 11.sp),
                                color = MythosTokens.DivineBlueLight
                            )
                        }
                    }
                }
            }

            // 2. HERO SELECTOR CAROUSEL
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                heroes.forEach { hero ->
                    val isHeroOwned = economyState.ownedHeroIds.contains(hero.id)
                    val isHeroSelected = economyState.selectedHeroId == hero.id
                    val isCardActive = viewedHeroId == hero.id
                    val heroLevel = economyState.heroProgression[hero.id] ?: 1

                    HeroSelectorTabItem(
                        hero = hero,
                        isOwned = isHeroOwned,
                        isSelected = isHeroSelected,
                        isActive = isCardActive,
                        level = heroLevel,
                        onClick = { viewedHeroId = hero.id }
                    )
                }
            }

            // 3. MAIN HERO SHOWCASE
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(
                            1.5.dp,
                            if (isCurrentlySelected) MythosTokens.PrimaryGold else MythosTokens.SecondaryGold.copy(alpha = 0.5f),
                            RoundedCornerShape(18.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Hero Portrait with Frame
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, MythosTokens.PrimaryGold, RoundedCornerShape(14.dp))
                        ) {
                            Image(
                                painter = painterResource(id = currentHero.portraitResId),
                                contentDescription = currentHero.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                alpha = if (isOwned) 1.0f else 0.45f
                            )

                            // Gradient overlay for readability
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.7f)
                                            )
                                        )
                                    )
                            )

                            // Locked Overlay Badge if not owned
                            if (!isOwned) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.Black.copy(alpha = 0.8f))
                                        .border(1.dp, MythosTokens.PanelBorder, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            tint = MythosTokens.TextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "LOCKED • FUTURE HERO",
                                            style = MythosTypography.RarityLabel.copy(fontSize = 11.sp),
                                            color = MythosTokens.TextMuted
                                        )
                                    }
                                }
                            }

                            // Active Tag on bottom left of portrait
                            if (isCurrentlySelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(10.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MythosTokens.PrimaryGold)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "ACTIVE CHAMPION",
                                            style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Rarity Badge top right
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MythosTokens.PanelElevated.copy(alpha = 0.9f))
                                    .border(1.dp, MythosTokens.getRarityColor(currentHero.rarity), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = currentHero.rarity.name,
                                    style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                                    color = MythosTokens.getRarityColor(currentHero.rarity)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Hero Name & Title
                        Text(
                            text = currentHero.name.uppercase(),
                            style = MythosTypography.GameTitle.copy(fontSize = 24.sp),
                            color = MythosTokens.PrimaryGold,
                            letterSpacing = 2.sp
                        )

                        Text(
                            text = "${currentHero.title} • ${currentHero.faction}",
                            style = MythosTypography.HeroTitle.copy(fontSize = 13.sp),
                            color = MythosTokens.TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Level, XP & Hero Shards Bar (Phase 7C Section 9)
                        if (isOwned) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = MythosTokens.BackgroundSurface,
                                border = BorderStroke(1.dp, MythosTokens.PanelBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "LEVEL ${heroProgress.level} / ${HeroProgressionConfig.MAX_HERO_LEVEL}",
                                            style = MythosTypography.HeroName.copy(fontSize = 13.sp),
                                            color = MythosTokens.PrimaryGold
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Diamond,
                                                contentDescription = null,
                                                tint = MythosTokens.SecondaryGold,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "${heroProgress.currentShards} Shards",
                                                style = MythosTypography.CardName.copy(fontSize = 12.sp),
                                                color = MythosTokens.SecondaryGold
                                            )
                                        }
                                    }

                                    if (!heroProgress.isMaxLevel) {
                                        LinearProgressIndicator(
                                            progress = { heroProgress.xpProgressFraction },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(5.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = MythosTokens.PrimaryGold,
                                            trackColor = MythosTokens.PanelElevated
                                        )
                                        Text(
                                            text = "${numberFormat.format(heroProgress.currentXp)} / ${numberFormat.format(heroProgress.xpRequiredForNextLevel)} XP",
                                            style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                                            color = MythosTokens.TextMuted,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Stats Row: HP, ATK, DEF scaled by progression level
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MythosTokens.BackgroundSurface)
                                .border(1.dp, MythosTokens.PanelBorder, RoundedCornerShape(10.dp))
                                .padding(vertical = 10.dp, horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HeroStatColumn("HP", numberFormat.format(scaledStats.hp), MythosTokens.Success)
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(MythosTokens.PanelBorder))
                            HeroStatColumn("ATK", numberFormat.format(scaledStats.attack), MythosTokens.Damage)
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(MythosTokens.PanelBorder))
                            HeroStatColumn("DEF", numberFormat.format(scaledStats.defense), MythosTokens.DivineBlueLight)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Combat Identity
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MythosTokens.PanelElevated)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = currentHero.combatIdentity,
                                style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                                color = Color(0xFFDDD8E8),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Abilities Section: Passive & Ultimate
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MythosTokens.BackgroundSurface)
                                .border(1.dp, MythosTokens.PanelBorder, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Passive Ability
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MythosTokens.PrimaryGold.copy(alpha = 0.2f))
                                        .border(1.dp, MythosTokens.PrimaryGold, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FlashOn,
                                        contentDescription = "Passive",
                                        tint = MythosTokens.PrimaryGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "PASSIVE — ${currentHero.passiveName}",
                                        style = MythosTypography.CardName.copy(fontSize = 12.sp),
                                        color = MythosTokens.PrimaryGold
                                    )
                                    Text(
                                        text = currentHero.passiveDescription,
                                        style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                                        color = MythosTokens.TextPrimary
                                    )
                                }
                            }

                            HorizontalDivider(color = MythosTokens.PanelBorder, thickness = 0.5.dp)

                            // Ultimate Ability
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MythosTokens.MythPowerFlame.copy(alpha = 0.2f))
                                        .border(1.dp, MythosTokens.MythPowerFlame, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = "Ultimate",
                                        tint = MythosTokens.MythPowerFlame,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "ULTIMATE — ${currentHero.ultimateName}",
                                        style = MythosTypography.CardName.copy(fontSize = 12.sp),
                                        color = MythosTokens.MythPowerFlame
                                    )
                                    Text(
                                        text = currentHero.ultimateDescription,
                                        style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                                        color = MythosTokens.TextPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Selection & Upgrade Action Buttons (Phase 7C Section 9 & 10)
                        if (!isOwned) {
                            MythosButton(
                                text = "LOCKED — UNLOCK THROUGH CAMPAIGN",
                                subtitle = when (currentHero.id) {
                                    "hero_achilles" -> "Complete Stage 2: Wrath of the Arena to unlock"
                                    "hero_merlin" -> "Complete Stage 5: Wrath of Olympus to unlock"
                                    else -> "Unlock through Campaign progression"
                                },
                                onClick = {},
                                enabled = false,
                                style = MythosButtonStyle.DISABLED,
                                icon = Icons.Default.Lock,
                                testTag = "locked_hero_button",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // UPGRADE Button
                                MythosButton(
                                    text = "UPGRADE HERO",
                                    subtitle = if (heroProgress.isMaxLevel) "MAX LEVEL 5" else "Ascend to LV ${heroProgress.level + 1}",
                                    onClick = { showHeroDetailDialog = true },
                                    style = MythosButtonStyle.SECONDARY,
                                    icon = Icons.Default.Upgrade,
                                    testTag = "open_hero_upgrade_button",
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                )

                                // SELECT / ACTIVE Button
                                if (isCurrentlySelected) {
                                    MythosButton(
                                        text = "ACTIVE CHAMPION",
                                        subtitle = "Equipped for battle",
                                        onClick = {},
                                        enabled = false,
                                        style = MythosButtonStyle.SECONDARY,
                                        icon = Icons.Default.CheckCircle,
                                        testTag = "hero_active_button",
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp)
                                    )
                                } else {
                                    MythosButton(
                                        text = "SELECT HERO",
                                        subtitle = "Equip as Champion",
                                        onClick = {
                                            PlayerEconomyRepository.instance.selectHero(currentHero.id)
                                        },
                                        style = MythosButtonStyle.PRIMARY,
                                        icon = Icons.Default.Check,
                                        testTag = "select_hero_button",
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Hero Detail & Upgrade Modal (Phase 7C Section 10)
        if (showHeroDetailDialog) {
            HeroDetailUpgradeDialog(
                heroDef = currentHero,
                economyState = economyState,
                onSelectHero = {
                    PlayerEconomyRepository.instance.selectHero(currentHero.id)
                    showHeroDetailDialog = false
                },
                onDismiss = { showHeroDetailDialog = false }
            )
        }
    }
}

@Composable
private fun HeroSelectorTabItem(
    hero: HeroDefinition,
    isOwned: Boolean,
    isSelected: Boolean,
    isActive: Boolean,
    level: Int = 1,
    onClick: () -> Unit
) {
    val borderColor = when {
        isSelected -> MythosTokens.PrimaryGold
        isActive -> MythosTokens.SecondaryGold
        else -> MythosTokens.PanelBorder
    }

    Surface(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(if (isActive) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("hero_tab_${hero.id}"),
        color = if (isActive) MythosTokens.PanelElevated else MythosTokens.Panel
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            ) {
                Image(
                    painter = painterResource(id = hero.portraitResId),
                    contentDescription = hero.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = if (isOwned) 1.0f else 0.4f
                )

                if (!isOwned) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(MythosTokens.PrimaryGold),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.Black,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = hero.name,
                style = MythosTypography.HeroName.copy(fontSize = 12.sp),
                color = if (isActive) MythosTokens.PrimaryGold else MythosTokens.TextPrimary,
                maxLines = 1
            )

            Text(
                text = if (isOwned) (if (isSelected) "ACTIVE • LV $level" else "UNLOCKED • LV $level") else "LOCKED",
                style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                color = when {
                    isSelected -> MythosTokens.PrimaryGold
                    isOwned -> MythosTokens.Success
                    else -> MythosTokens.TextMuted
                }
            )
        }
    }
}

@Composable
private fun HeroStatColumn(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
            color = MythosTokens.TextMuted
        )
        Text(
            text = value,
            style = MythosTypography.HeroName.copy(fontSize = 14.sp),
            color = color
        )
    }
}
