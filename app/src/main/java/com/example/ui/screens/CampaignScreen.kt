package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.*
import com.example.monetization.PlayerEconomyRepository
import com.example.ui.components.MythosButton
import com.example.ui.components.MythosButtonStyle
import com.example.ui.theme.MythosTokens
import com.example.ui.theme.MythosTypography
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CampaignScreen(
    onNavigateBack: () -> Unit,
    onStartStageBattle: (BattleEncounterConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val economyState by PlayerEconomyRepository.instance.economyState.collectAsState()
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }
    val world = CampaignCatalog.getDefaultWorld()
    val stages = world.stages

    // Selected stage for detail dialog
    var selectedStageForDetail by remember { mutableStateOf<CampaignStage?>(null) }
    var showDeckInvalidDialog by remember { mutableStateOf(false) }
    var deckValidationResult by remember { mutableStateOf<DeckValidationResult?>(null) }

    val completedCount = stages.count { economyState.completedStageIds.contains(it.stageId) }
    val totalStarsEarned = stages.sumOf { economyState.stageStars[it.stageId] ?: 0 }
    val maxStars = stages.size * 3 // 15 stars total
    val isWorldComplete = completedCount >= stages.size

    val selectedHeroDef = HeroCatalog.findHero(economyState.selectedHeroId) ?: HeroCatalog.HERCULES

    // Real deterministic player combat power calculation (Phase 7B Requirement #7)
    val playerCombatPower = remember(economyState.selectedHeroId, economyState.activeDeck, economyState.cardLevels) {
        PlayerPowerCalculator.calculate(
            hero = selectedHeroDef,
            activeDeck = economyState.activeDeck,
            cardLevels = economyState.cardLevels
        )
    }

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
            alpha = 0.22f
        )

        Column(modifier = Modifier.fillMaxSize()) {
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
                                .testTag("campaign_back_button")
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
                                text = "CAMPAIGN",
                                style = MythosTypography.GameSubtitle.copy(fontSize = 17.sp),
                                color = MythosTokens.PrimaryGold,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = "AEGEAN / OLYMPUS",
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

            // 2. SCROLLABLE CAMPAIGN PATH
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // WORLD PROGRESS HEADER CARD (Requirement #5)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            if (isWorldComplete) 2.dp else 1.5.dp,
                            if (isWorldComplete) MythosTokens.Success else MythosTokens.PrimaryGold,
                            RoundedCornerShape(16.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MythosTokens.SecondaryGold.copy(alpha = 0.3f))
                                    .border(1.dp, MythosTokens.PrimaryGold, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "WORLD 1",
                                    style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                                    color = MythosTokens.PrimaryGold,
                                    letterSpacing = 1.sp
                                )
                            }

                            // Active hero & Combat Power
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, MythosTokens.PrimaryGold, CircleShape)
                                ) {
                                    Image(
                                        painter = painterResource(id = selectedHeroDef.portraitResId),
                                        contentDescription = selectedHeroDef.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Text(
                                    text = "⚔ ${numberFormat.format(playerCombatPower)}",
                                    style = MythosTypography.HeroName.copy(fontSize = 11.sp),
                                    color = MythosTokens.PrimaryGold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // World Title
                        Text(
                            text = "AEGEAN / OLYMPUS",
                            style = MythosTypography.GameTitle.copy(fontSize = 20.sp),
                            color = MythosTokens.PrimaryGold,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = world.subtitle,
                            style = MythosTypography.HeroTitle.copy(fontSize = 12.sp),
                            color = MythosTokens.TextPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // WORLD METRICS: Stages Completed & Total Stars (Requirement #5)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MythosTokens.BackgroundSurface)
                                .border(0.5.dp, MythosTokens.PanelBorder, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "PROGRESSION",
                                    style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                    color = MythosTokens.TextMuted
                                )
                                Text(
                                    text = "$completedCount / ${stages.size} STAGES COMPLETED",
                                    style = MythosTypography.HeroName.copy(fontSize = 12.sp),
                                    color = if (isWorldComplete) MythosTokens.Success else MythosTokens.PrimaryGold
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "TOTAL STARS",
                                    style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                    color = MythosTokens.TextMuted
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = MythosTokens.PrimaryGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "$totalStarsEarned / $maxStars",
                                        style = MythosTypography.HeroName.copy(fontSize = 13.sp),
                                        color = MythosTokens.PrimaryGold
                                    )
                                }
                            }
                        }

                        // WORLD COMPLETE BANNER (Requirement #5)
                        if (isWorldComplete) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                MythosTokens.Success.copy(alpha = 0.25f),
                                                MythosTokens.PrimaryGold.copy(alpha = 0.35f),
                                                MythosTokens.Success.copy(alpha = 0.25f)
                                            )
                                        )
                                    )
                                    .border(1.dp, MythosTokens.Success, RoundedCornerShape(8.dp))
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MythosTokens.Success,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "WORLD COMPLETE — OLYMPUS ASCENDED",
                                        style = MythosTypography.GameSubtitle.copy(fontSize = 11.sp),
                                        color = MythosTokens.Success,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Visual progress bar
                        val progressFraction = if (stages.isNotEmpty()) completedCount.toFloat() / stages.size.toFloat() else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MythosTokens.BackgroundSurface)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = progressFraction)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(MythosTokens.SecondaryGold, MythosTokens.PrimaryGold)
                                        )
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // STAGE NODES (Requirements #1, #4, #8)
                stages.forEachIndexed { index, stage ->
                    val isUnlocked = stage.stageNumber <= economyState.highestUnlockedStage
                    val isCompleted = economyState.completedStageIds.contains(stage.stageId)
                    val isCurrent = isUnlocked && !isCompleted
                    val stageStars = economyState.stageStars[stage.stageId] ?: if (isCompleted) 1 else 0

                    // Stage Node Card
                    CampaignStageNodeCard(
                        stage = stage,
                        isUnlocked = isUnlocked,
                        isCompleted = isCompleted,
                        isCurrent = isCurrent,
                        stars = stageStars,
                        onClick = {
                            selectedStageForDetail = stage
                        }
                    )

                    // Connecting Line to next stage (if not last stage)
                    if (index < stages.size - 1) {
                        CampaignPathConnector(isNextUnlocked = (stages[index + 1].stageNumber <= economyState.highestUnlockedStage))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // WORLD 2 TEASER (Requirement #5: World 2 visually locked / future content)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, MythosTokens.PanelBorder.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = MythosTokens.TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "WORLD 2 — UNDERWORLD / TARTARUS",
                                style = MythosTypography.GameSubtitle.copy(fontSize = 13.sp),
                                color = MythosTokens.TextMuted,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "Descent into the Realm of Hades • Future Expansion",
                            style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                            color = MythosTokens.TextMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // 3. STAGE DETAIL DIALOG (Requirements #6, #7, #8)
        selectedStageForDetail?.let { stage ->
            val isUnlocked = stage.stageNumber <= economyState.highestUnlockedStage
            val isCompleted = economyState.completedStageIds.contains(stage.stageId)
            val starsEarned = economyState.stageStars[stage.stageId] ?: if (isCompleted) 1 else 0

            StageDetailDialog(
                stage = stage,
                isUnlocked = isUnlocked,
                isCompleted = isCompleted,
                stars = starsEarned,
                playerPower = playerCombatPower,
                onDismiss = { selectedStageForDetail = null },
                onStartBattle = {
                    val validation = DeckValidator.validate(economyState.activeDeck, economyState.ownedCardCounts)
                    if (!validation.isValid) {
                        deckValidationResult = validation
                        showDeckInvalidDialog = true
                    } else {
                        selectedStageForDetail = null
                        onStartStageBattle(stage.toEncounterConfig())
                    }
                }
            )
        }

        // Deck Invalid Modal if deck is somehow invalid
        if (showDeckInvalidDialog) {
            AlertDialog(
                onDismissRequest = { showDeckInvalidDialog = false },
                title = {
                    Text(
                        text = "DECK VALIDATION FAILED",
                        style = MythosTypography.GameSubtitle,
                        color = MythosTokens.Damage
                    )
                },
                text = {
                    val issueMessage = deckValidationResult?.issues?.firstOrNull()?.message
                    Text(
                        text = issueMessage ?: "Your active deck must contain exactly 20 valid cards.",
                        style = MythosTypography.CardDescription,
                        color = MythosTokens.TextPrimary
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showDeckInvalidDialog = false }) {
                        Text("OK", color = MythosTokens.PrimaryGold)
                    }
                },
                containerColor = MythosTokens.Panel
            )
        }
    }
}

/**
 * Stage Node Card presenting distinct visual states:
 * LOCKED, CURRENT, COMPLETED with stars (★☆☆, ★★☆, ★★★), and BOSS (Stage 5).
 * (Phase 7B Requirements #1, #4, #8)
 */
@Composable
private fun CampaignStageNodeCard(
    stage: CampaignStage,
    isUnlocked: Boolean,
    isCompleted: Boolean,
    isCurrent: Boolean,
    stars: Int,
    onClick: () -> Unit
) {
    val borderColor = when {
        stage.isBoss && isCurrent -> MythosTokens.Damage
        stage.isBoss -> MythosTokens.Damage.copy(alpha = 0.8f)
        isCurrent -> MythosTokens.PrimaryGold
        isCompleted -> MythosTokens.SecondaryGold.copy(alpha = 0.85f)
        else -> MythosTokens.PanelBorder.copy(alpha = 0.4f)
    }

    val containerAlpha = if (isUnlocked) 1.0f else 0.5f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                if (isCurrent || stage.isBoss) 2.dp else 1.dp,
                borderColor,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .testTag("stage_node_${stage.stageNumber}"),
        colors = CardDefaults.cardColors(
            containerColor = when {
                stage.isBoss && isUnlocked -> Color(0xFF1E1015)
                isCurrent -> MythosTokens.PanelElevated
                else -> MythosTokens.Panel
            }.copy(alpha = containerAlpha)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Stage Status & Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Stage number badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (stage.isBoss) MythosTokens.Damage.copy(alpha = 0.3f)
                                else MythosTokens.PrimaryGold.copy(alpha = 0.2f)
                            )
                            .border(
                                0.5.dp,
                                if (stage.isBoss) MythosTokens.Damage else MythosTokens.PrimaryGold,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (stage.isBoss) "STAGE ${stage.stageNumber} • BOSS" else "STAGE ${stage.stageNumber}",
                            style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                            color = if (stage.isBoss) MythosTokens.Damage else MythosTokens.PrimaryGold,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // CURRENT badge
                    if (isCurrent) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MythosTokens.PrimaryGold)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "CURRENT",
                                style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Completion Stars or Locked Badge
                if (isCompleted) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        // Render exact 3-star display (★☆☆, ★★☆, ★★★)
                        for (i in 1..3) {
                            Icon(
                                imageVector = if (i <= stars) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (i <= stars) MythosTokens.PrimaryGold else Color(0xFF554D66),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                } else if (!isUnlocked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = MythosTokens.TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "LOCKED",
                            style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                            color = MythosTokens.TextMuted
                        )
                    }
                } else {
                    Text(
                        text = "READY",
                        style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                        color = MythosTokens.PrimaryGold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stage Main Content: Enemy Portrait + Name + Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            1.dp,
                            if (stage.isBoss) MythosTokens.Damage else borderColor,
                            RoundedCornerShape(10.dp)
                        )
                ) {
                    Image(
                        painter = painterResource(id = stage.enemyPortraitResId),
                        contentDescription = stage.enemyName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = if (isUnlocked) 1.0f else 0.35f
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
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (stage.isBoss) "${stage.name.uppercase()} — BOSS" else stage.name,
                        style = MythosTypography.HeroName.copy(fontSize = 15.sp),
                        color = when {
                            !isUnlocked -> MythosTokens.TextMuted
                            stage.isBoss -> MythosTokens.Damage
                            else -> MythosTokens.TextPrimary
                        },
                        fontWeight = if (stage.isBoss) FontWeight.Black else FontWeight.Bold
                    )

                    Text(
                        text = "Enemy: ${stage.enemyName} (${stage.enemyTitle})",
                        style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                        color = MythosTokens.TextMuted
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (!isUnlocked) {
                        Text(
                            text = stage.unlockRequirement,
                            style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                            color = MythosTokens.Damage.copy(alpha = 0.85f)
                        )
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚔ Rec: ${stage.recommendedPower}",
                                style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                color = MythosTokens.SecondaryGold
                            )
                            Text(
                                text = "•",
                                color = MythosTokens.TextMuted,
                                fontSize = 8.sp
                            )
                            Text(
                                text = "🪙 ${stage.firstClearGold}",
                                style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                color = MythosTokens.PrimaryGold
                            )
                            Text(
                                text = "•",
                                color = MythosTokens.TextMuted,
                                fontSize = 8.sp
                            )
                            Text(
                                text = "🃏 ${stage.firstClearCardName}",
                                style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                color = MythosTokens.getRarityColor(stage.firstClearCardRarity),
                                maxLines = 1
                            )
                        }
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Details",
                    tint = if (isUnlocked) MythosTokens.PrimaryGold else MythosTokens.TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun CampaignPathConnector(isNextUnlocked: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val lineColor = if (isNextUnlocked) MythosTokens.PrimaryGold.copy(alpha = 0.8f) else MythosTokens.PanelBorder
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(8.dp)
                .background(lineColor)
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(lineColor)
        )
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(8.dp)
                .background(lineColor)
        )
    }
}

/**
 * Stage Detail Dialog presenting Enemy stats, Recommended Power vs Player Power,
 * Best Result stars, First-clear vs Replay Rewards, and Battle/Replay button.
 * (Phase 7B Requirements #6, #7, #8)
 */
@Composable
private fun StageDetailDialog(
    stage: CampaignStage,
    isUnlocked: Boolean,
    isCompleted: Boolean,
    stars: Int,
    playerPower: Int,
    onDismiss: () -> Unit,
    onStartBattle: () -> Unit
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(
                    if (stage.isBoss) 2.dp else 1.5.dp,
                    if (stage.isBoss) MythosTokens.Damage else MythosTokens.PrimaryGold,
                    RoundedCornerShape(18.dp)
                )
                .testTag("stage_detail_dialog"),
            colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header: Stage Number & World
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (stage.isBoss) MythosTokens.Damage.copy(alpha = 0.25f)
                                else MythosTokens.SecondaryGold.copy(alpha = 0.2f)
                            )
                            .border(
                                0.5.dp,
                                if (stage.isBoss) MythosTokens.Damage else MythosTokens.PrimaryGold,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (stage.isBoss) "STAGE ${stage.stageNumber} • BOSS" else "STAGE ${stage.stageNumber}",
                            style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                            color = if (stage.isBoss) MythosTokens.Damage else MythosTokens.PrimaryGold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MythosTokens.TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Stage Title
                Text(
                    text = if (stage.isBoss) "${stage.name.uppercase()} — BOSS" else stage.name.uppercase(),
                    style = MythosTypography.GameTitle.copy(fontSize = 20.sp),
                    color = if (stage.isBoss) MythosTokens.Damage else MythosTokens.PrimaryGold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stage.description,
                    style = MythosTypography.CardDescription.copy(fontSize = 12.sp),
                    color = MythosTokens.TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // Enemy Card Details (Name, Portrait, HP, ATK, DEF)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MythosTokens.PanelBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = MythosTokens.BackgroundSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MythosTokens.Damage, RoundedCornerShape(8.dp))
                        ) {
                            Image(
                                painter = painterResource(id = stage.enemyPortraitResId),
                                contentDescription = stage.enemyName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = stage.enemyName,
                                style = MythosTypography.HeroName.copy(fontSize = 15.sp),
                                color = MythosTokens.Damage
                            )
                            Text(
                                text = "${stage.enemyTitle} • ${stage.enemyFaction}",
                                style = MythosTypography.HeroTitle.copy(fontSize = 11.sp),
                                color = MythosTokens.TextMuted
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                MiniStatBadge("HP ${numberFormat.format(stage.enemyHp)}", MythosTokens.Success)
                                MiniStatBadge("ATK ${numberFormat.format(stage.enemyAtk)}", MythosTokens.Damage)
                                MiniStatBadge("DEF ${numberFormat.format(stage.enemyDef)}", MythosTokens.DivineBlue)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // POWER COMPARISON: Recommended Power vs Player Combat Power (Requirement #7)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MythosTokens.PanelElevated)
                        .border(0.5.dp, MythosTokens.PanelBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "RECOMMENDED POWER",
                            style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                            color = MythosTokens.TextMuted
                        )
                        Text(
                            text = "⚔ ${numberFormat.format(stage.recommendedPower)}",
                            style = MythosTypography.HeroName.copy(fontSize = 12.sp),
                            color = MythosTokens.TextSecondary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "YOUR COMBAT POWER",
                            style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                            color = MythosTokens.TextMuted
                        )
                        val isMatched = playerPower >= stage.recommendedPower
                        Text(
                            text = "⚔ ${numberFormat.format(playerPower)}",
                            style = MythosTypography.HeroName.copy(fontSize = 13.sp),
                            color = if (isMatched) MythosTokens.Success else MythosTokens.PrimaryGold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // BEST RESULT (Requirement #6)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MythosTokens.BackgroundSurface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Best Result",
                        style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                        color = MythosTokens.TextMuted
                    )

                    if (isCompleted && stars > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (s in 1..3) {
                                Icon(
                                    imageVector = if (s <= stars) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = null,
                                    tint = if (s <= stars) MythosTokens.PrimaryGold else Color(0xFF554D66),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$stars / 3 STARS",
                                style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                                color = MythosTokens.PrimaryGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = "NOT CLEARED",
                            style = MythosTypography.RarityLabel.copy(fontSize = 10.sp),
                            color = Color(0xFFAFA7BD)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // STAR CONDITIONS SUMMARY
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F0D16))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "STAR CONDITIONS",
                        style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                        color = MythosTokens.PrimaryGold
                    )
                    ConditionItem("★", "Clear Stage (Victory)")
                    ConditionItem("★", "Finish battle with HP > 50%")
                    ConditionItem("★", "Complete in ≤ ${stage.maxTurnsForStarCondition} turns")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // REWARDS BREAKDOWN: FIRST CLEAR vs REPLAY (Requirement #3, #6)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MythosTokens.BackgroundSurface)
                        .border(1.dp, MythosTokens.PanelBorder, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // First Clear Rewards
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "FIRST CLEAR REWARDS",
                                style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                color = MythosTokens.PrimaryGold
                            )
                            if (isCompleted) {
                                Text(
                                    text = "CLAIMED",
                                    style = MythosTypography.RarityLabel.copy(fontSize = 8.sp),
                                    color = MythosTokens.Success
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🪙 +${numberFormat.format(stage.firstClearGold)}",
                                style = MythosTypography.HeroName.copy(fontSize = 11.sp),
                                color = MythosTokens.PrimaryGold
                            )
                            Text(
                                text = "✦ +${numberFormat.format(stage.firstClearXp)} XP",
                                style = MythosTypography.HeroName.copy(fontSize = 11.sp),
                                color = MythosTokens.DivineBlueLight
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MythosTokens.getRarityColor(stage.firstClearCardRarity).copy(alpha = 0.2f))
                                    .border(0.5.dp, MythosTokens.getRarityColor(stage.firstClearCardRarity), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "🃏 ${stage.firstClearCardName}",
                                    style = MythosTypography.RarityLabel.copy(fontSize = 8.sp),
                                    color = MythosTokens.getRarityColor(stage.firstClearCardRarity)
                                )
                            }
                        }

                        // Hero Progression Rewards & Hero Unlock (Phase 7C Sections 11, 12, 16)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🛡️ +${numberFormat.format(stage.firstClearHeroXp)} Hero XP",
                                style = MythosTypography.HeroName.copy(fontSize = 10.sp),
                                color = MythosTokens.DivineBlueLight
                            )
                            Text(
                                text = "💎 +${stage.firstClearHeroShards} Shards",
                                style = MythosTypography.HeroName.copy(fontSize = 10.sp),
                                color = MythosTokens.PrimaryGold
                            )
                            when (stage.stageNumber) {
                                2 -> {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MythosTokens.Success.copy(alpha = 0.2f))
                                            .border(0.5.dp, MythosTokens.Success, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "⚔️ UNLOCKS ACHILLES",
                                            style = MythosTypography.RarityLabel.copy(fontSize = 8.sp),
                                            color = MythosTokens.Success
                                        )
                                    }
                                }
                                5 -> {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MythosTokens.Success.copy(alpha = 0.2f))
                                            .border(0.5.dp, MythosTokens.Success, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "⚔️ UNLOCKS MERLIN",
                                            style = MythosTypography.RarityLabel.copy(fontSize = 8.sp),
                                            color = MythosTokens.Success
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MythosTokens.PanelBorder, thickness = 0.5.dp)

                    // Replay Rewards
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "REPLAY REWARDS",
                            style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                            color = Color(0xFFAFA7BD)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🪙 +${numberFormat.format(stage.replayGold)}",
                                style = MythosTypography.HeroName.copy(fontSize = 11.sp),
                                color = MythosTokens.PrimaryGold.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "✦ +${numberFormat.format(stage.replayXp)} XP",
                                style = MythosTypography.HeroName.copy(fontSize = 11.sp),
                                color = MythosTokens.DivineBlueLight.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "🛡️ +${numberFormat.format(stage.replayHeroXp)} Hero XP",
                                style = MythosTypography.HeroName.copy(fontSize = 10.sp),
                                color = MythosTokens.DivineBlueLight.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Start or Replay Action Button (Requirement #6)
                if (isUnlocked) {
                    val buttonText = if (isCompleted) "REPLAY BATTLE" else "START BATTLE"
                    val buttonSub = if (isCompleted) "Replay stage for gold & XP bounty" else "Engage ${stage.enemyName} with active deck"
                    MythosButton(
                        text = buttonText,
                        subtitle = buttonSub,
                        onClick = onStartBattle,
                        style = if (stage.isBoss) MythosButtonStyle.DANGER else MythosButtonStyle.PRIMARY,
                        icon = if (isCompleted) Icons.Default.Refresh else Icons.Default.PlayArrow,
                        testTag = if (isCompleted) "replay_campaign_battle_button" else "start_campaign_battle_button",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    )
                } else {
                    MythosButton(
                        text = "STAGE LOCKED",
                        subtitle = stage.unlockRequirement,
                        onClick = {},
                        enabled = false,
                        style = MythosButtonStyle.DISABLED,
                        icon = Icons.Default.Lock,
                        testTag = "locked_stage_button",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ConditionItem(star: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = star,
            color = MythosTokens.PrimaryGold,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = text,
            color = Color(0xFFC7C1D4),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun MiniStatBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.5.dp, color, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
            color = color
        )
    }
}
