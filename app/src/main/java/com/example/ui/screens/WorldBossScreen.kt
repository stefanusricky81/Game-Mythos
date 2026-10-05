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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.backend.MythosBackend
import com.example.backend.online.BackendException
import com.example.backend.online.OnlineBackend
import com.example.data.*
import com.example.ui.components.OnlineStatusBanner
import com.example.ui.components.rememberDisplayEconomyState
import kotlinx.coroutines.launch
import com.example.monetization.PlayerEconomyRepository
import com.example.ui.components.MythosButton
import com.example.ui.components.MythosButtonStyle
import com.example.ui.theme.MythosTokens
import com.example.ui.theme.MythosTypography
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldBossScreen(
    bossId: String = "world_boss_kronos",
    onNavigateBack: () -> Unit,
    onEnterBossBattle: (BattleEncounterConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val economyState by rememberDisplayEconomyState()
    val boss = remember(bossId) { EndgameCatalog.findWorldBoss(bossId) ?: EndgameCatalog.KRONOS }
    val currentHp = economyState.worldBossCurrentHp
    val hpRatio = (currentHp.toFloat() / boss.maxHp.toFloat()).coerceIn(0f, 1f)
    val currentPhase = boss.getCurrentPhase(currentHp)
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }
    val contrib = economyState.worldBossContributions[boss.bossId]
        ?: WorldBossContribution(bossId = boss.bossId)
    val eligibleTier = remember(contrib.totalDamage) {
        WorldBossRewardTier.determineTier(contrib.totalDamage)
    }

    var claimMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val isOnline = MythosBackend.isOnline
    val describeError: (Throwable) -> String = { e ->
        (e as? BackendException)?.let(OnlineBackend::userMessage) ?: e.message ?: "Something went wrong."
    }
    LaunchedEffect(Unit) { if (isOnline) MythosBackend.online.refreshAll() }

    // Offline: the boss fights the locally tracked HP and pays the encounter rewards locally. Online: the
    // server issued the attempt (enemy HP included) and decides every reward, so the encounter pays nothing.
    val buildEncounter: (Int, String?) -> BattleEncounterConfig = { enemyHp, sessionId ->
        val enemyHero = Hero(
            id = boss.bossId,
            name = boss.name,
            title = boss.title,
            currentHp = enemyHp,
            maxHp = enemyHp,
            baseAttack = (boss.baseAttack * currentPhase.attackMultiplier).toInt(),
            baseDefense = (boss.baseDefense * currentPhase.defenseMultiplier).toInt(),
            portraitResId = boss.portraitResId
        )
        BattleEncounterConfig(
            stageId = boss.bossId,
            stageNumber = 999,
            encounterName = "${boss.name} [World Boss]",
            enemyHero = enemyHero,
            rewards = if (sessionId != null) {
                BattleRewards(gold = 0, xp = 0, cardRewardName = "Decided by the server", cardRewardRarity = CardRarity.RARE)
            } else {
                BattleRewards(gold = 5_000, xp = 3_000, cardRewardName = "Titan Slayer Crate", cardRewardRarity = CardRarity.LEGENDARY)
            },
            rewardCardId = "c_titans_wrath",
            isBoss = true,
            isWorldBoss = true,
            worldBossId = boss.bossId,
            maxTurnsForStarCondition = 10,
            onlineSessionId = sessionId
        )
    }
    val alliance = economyState.playerAlliance
    val allianceMembersCount = alliance?.members?.size ?: 18
    val allianceName = alliance?.name ?: "Olympus Guardians"
    val allianceTotalDamage = remember(contrib.totalDamage) {
        12_840_500L + contrib.totalDamage
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MythosTokens.Background)
    ) {
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
                            text = "WORLD BOSS",
                            style = MythosTypography.GameTitle.copy(fontSize = 18.sp),
                            color = MythosTokens.PrimaryGold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("world_boss_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MythosTokens.PrimaryGold
                            )
                        }
                    },
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MythosTokens.PanelElevated)
                                .border(1.dp, MythosTokens.PanelBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = "🪙", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${economyState.eventTokens} TOKENS",
                                style = MythosTypography.RarityLabel.copy(fontSize = 11.sp),
                                color = MythosTokens.PrimaryGold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OnlineStatusBanner(onRetry = { coroutineScope.launch { MythosBackend.online.refreshAll() } })

                // Boss Hero Header Presentation
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, MythosTokens.Damage, RoundedCornerShape(16.dp))
                        .testTag("world_boss_header_card"),
                    colors = CardDefaults.cardColors(containerColor = MythosTokens.PanelElevated)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Boss Crest / Portrait
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(MythosTokens.Panel)
                                .border(2.dp, MythosTokens.Damage, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = boss.portraitResId),
                                contentDescription = boss.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = boss.name.uppercase(),
                            style = MythosTypography.HeroName.copy(fontSize = 22.sp),
                            color = MythosTokens.PrimaryGold,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "${boss.title} • ${boss.mythology}",
                            style = MythosTypography.HeroTitle.copy(fontSize = 12.sp),
                            color = MythosTokens.TextMuted,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Boss HP Bar
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "BOSS HP: ${(hpRatio * 100).toInt()}%",
                                    style = MythosTypography.RarityLabel.copy(fontSize = 11.sp),
                                    color = MythosTokens.Damage
                                )
                                Text(
                                    text = "${numberFormat.format(currentHp)} / ${numberFormat.format(boss.maxHp)}",
                                    style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { hpRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(7.dp)),
                                color = if (hpRatio > 0.33f) MythosTokens.Damage else MythosTokens.Error,
                                trackColor = MythosTokens.Background
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Current Phase Badge
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MythosTokens.Panel)
                                .border(1.dp, MythosTokens.PrimaryGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "⚡", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = currentPhase.name.uppercase(),
                                    style = MythosTypography.CardName.copy(fontSize = 12.sp),
                                    color = MythosTokens.PrimaryGold
                                )
                                Text(
                                    text = "${currentPhase.abilityName}: ${currentPhase.abilityDescription}",
                                    style = MythosTypography.CardDescription.copy(fontSize = 10.sp),
                                    color = MythosTokens.TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // ENTER BATTLE CTA
                        MythosButton(
                            text = "ENTER BATTLE",
                            subtitle = "Deal Damage • Earn Contribution & Tokens",
                            onClick = {
                                if (isOnline) {
                                    coroutineScope.launch {
                                        MythosBackend.online.startWorldBoss(boss.bossId).fold(
                                            onSuccess = { session -> onEnterBossBattle(buildEncounter(session.enemyHp, session.sessionId)) },
                                            onFailure = { e -> claimMessage = describeError(e) }
                                        )
                                    }
                                } else {
                                    onEnterBossBattle(buildEncounter(currentHp.coerceAtMost(35_000L).toInt(), null))
                                }
                            },
                            style = MythosButtonStyle.PRIMARY,
                            icon = Icons.Default.Whatshot,
                            testTag = "world_boss_enter_battle_button",
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        )
                    }
                }

                // Personal Contribution Section (Phase 10 Section 5)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MythosTokens.PanelBorder, RoundedCornerShape(12.dp))
                        .testTag("world_boss_personal_contribution_card"),
                    colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PERSONAL CONTRIBUTION",
                                style = MythosTypography.RarityLabel,
                                color = MythosTokens.PrimaryGold
                            )
                            Text(
                                text = "Tier: ${eligibleTier.displayName}",
                                style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                                color = MythosTokens.DivineBlueLight
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ContributionMetric("Total Damage", numberFormat.format(contrib.totalDamage), Modifier.weight(1f))
                            ContributionMetric("Battles", "${contrib.battlesCompleted}", Modifier.weight(1f))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ContributionMetric("Highest Hit", numberFormat.format(contrib.highestSingleHit), Modifier.weight(1f))
                            ContributionMetric("Contribution", numberFormat.format(contrib.contributionScore), Modifier.weight(1f))
                        }

                        // Claim Reward Button
                        if (contrib.claimedRewardTier != null) {
                            Text(
                                text = "✓ Rewards Claimed (${contrib.claimedRewardTier})",
                                style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                                color = MythosTokens.Success,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        } else if (contrib.participationCount > 0) {
                            Button(
                                onClick = {
                                    if (isOnline) {
                                        coroutineScope.launch {
                                            MythosBackend.online.claimWorldBossReward(boss.bossId).fold(
                                                onSuccess = { claim -> claimMessage = "Claimed ${claim.tier ?: "rewards"}!" },
                                                onFailure = { e -> claimMessage = describeError(e) }
                                            )
                                        }
                                    } else {
                                        val result = PlayerEconomyRepository.instance.claimWorldBossReward(boss.bossId)
                                        claimMessage = if (result.isSuccess) {
                                            "Claimed ${result.getOrNull()?.displayName}!"
                                        } else {
                                            result.exceptionOrNull()?.message
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("claim_world_boss_reward_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MythosTokens.PrimaryGold)
                            ) {
                                Text(
                                    text = "CLAIM CONTRIBUTION REWARDS",
                                    style = MythosTypography.RarityLabel,
                                    color = Color.Black
                                )
                            }
                        }

                        claimMessage?.let { msg: String ->
                            Text(
                                text = msg,
                                style = MythosTypography.CardDescription,
                                color = MythosTokens.PrimaryGold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Alliance Raid Board Section (Phase 10 Section 7)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MythosTokens.PanelBorder, RoundedCornerShape(12.dp))
                        .testTag("world_boss_alliance_raid_board_card"),
                    colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "ALLIANCE RAID BOARD",
                            style = MythosTypography.RarityLabel,
                            color = MythosTokens.PrimaryGold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Alliance: $allianceName",
                                style = MythosTypography.HeroName.copy(fontSize = 13.sp),
                                color = Color.White
                            )
                            Text(
                                text = "Members: $allianceMembersCount / 20",
                                style = MythosTypography.CardDescription.copy(fontSize = 12.sp),
                                color = MythosTokens.TextMuted
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Damage: ${numberFormat.format(allianceTotalDamage)}",
                                style = MythosTypography.CardName.copy(fontSize = 12.sp),
                                color = MythosTokens.Damage
                            )
                            Text(
                                text = "Rank: #7",
                                style = MythosTypography.HeroName.copy(fontSize = 13.sp),
                                color = MythosTokens.PrimaryGold
                            )
                        }
                    }
                }

                // Local Leaderboard Section (Phase 10 Section 18)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MythosTokens.PanelBorder, RoundedCornerShape(12.dp))
                        .testTag("world_boss_leaderboard_card"),
                    colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "ALLIANCE RAID LEADERBOARD",
                            style = MythosTypography.RarityLabel,
                            color = MythosTokens.PrimaryGold
                        )
                        EndgameCatalog.getMockAllianceRaidBoard().take(5).forEach { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "#${entry.rank}",
                                        style = MythosTypography.HeroName.copy(fontSize = 12.sp),
                                        color = if (entry.rank <= 3) MythosTokens.PrimaryGold else MythosTokens.TextMuted,
                                        modifier = Modifier.width(28.dp)
                                    )
                                    Text(
                                        text = entry.allianceName,
                                        style = MythosTypography.CardName.copy(fontSize = 12.sp),
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = numberFormat.format(entry.totalDamage),
                                    style = MythosTypography.CardDescription.copy(fontSize = 12.sp),
                                    color = MythosTokens.Damage
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContributionMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MythosTokens.PanelElevated)
            .border(1.dp, MythosTokens.PanelBorder, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column {
            Text(
                text = label.uppercase(),
                style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                color = MythosTokens.TextMuted
            )
            Text(
                text = value,
                style = MythosTypography.HeroName.copy(fontSize = 14.sp),
                color = Color.White
            )
        }
    }
}
