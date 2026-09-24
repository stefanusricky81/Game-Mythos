package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.MythosConfig
import com.example.R
import com.example.data.HerculesIdentity
import com.example.monetization.PlayerEconomyRepository
import com.example.ui.components.MythosButton
import com.example.ui.components.MythosButtonStyle
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun TitleScreen(
    onStartBattle: () -> Unit,
    onViewDeck: () -> Unit,
    onOpenShop: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showGuideDialog by remember { mutableStateOf(false) }
    val economyState by PlayerEconomyRepository.instance.economyState.collectAsState()
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MythosTokens.Background)
    ) {
        // Subtle battlefield background
        Image(
            painter = painterResource(id = R.drawable.img_battlefield_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.25f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Title & Subtitle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                // Section 18: Prototype Badge shown ONLY when DEBUG_BUILD is true
                if (MythosConfig.DEBUG_BUILD) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MythosTokens.SecondaryGold.copy(alpha = 0.3f))
                            .border(1.dp, MythosTokens.PrimaryGold, RoundedCornerShape(6.dp))
                            .padding(horizontal = 12.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "VERTICAL SLICE • HERCULES TEST",
                            style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                            color = MythosTokens.PrimaryGold,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = "MYTHOS",
                    style = MythosTypography.GameTitle.copy(fontSize = 36.sp),
                    letterSpacing = 4.sp
                )

                Text(
                    text = "AGE OF HEROES",
                    style = MythosTypography.GameSubtitle.copy(fontSize = 15.sp),
                    letterSpacing = 3.sp
                )

                Text(
                    text = "Original Mythology-Inspired Collectible Card Battle",
                    style = MythosTypography.HeroTitle.copy(fontSize = 11.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Player Currencies Status Bar
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MythosTokens.PanelElevated)
                        .border(1.dp, MythosTokens.PrimaryGold.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .clickable { onOpenShop() }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "🪙", fontSize = 12.sp)
                        Text(
                            text = numberFormat.format(economyState.gold),
                            style = MythosTypography.HeroName.copy(fontSize = 12.sp),
                            color = MythosTokens.PrimaryGold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(12.dp)
                            .background(MythosTokens.PanelBorder)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "💎", fontSize = 12.sp)
                        Text(
                            text = numberFormat.format(economyState.mythGems),
                            style = MythosTypography.HeroName.copy(fontSize = 12.sp),
                            color = MythosTokens.DivineBlueLight
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Hero Showcase: Hercules
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MythosTokens.SecondaryGold, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(74.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, MythosTokens.PrimaryGold, RoundedCornerShape(10.dp))
                        ) {
                            Image(
                                painter = painterResource(id = HerculesIdentity.assets.portrait.resolveResId()),
                                contentDescription = HerculesIdentity.NAME,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = HerculesIdentity.NAME.uppercase(),
                                style = MythosTypography.HeroName.copy(fontSize = 18.sp),
                                color = MythosTokens.PrimaryGold
                            )
                            Text(
                                text = "${HerculesIdentity.TITLE} • ${HerculesIdentity.FACTION.displayName}",
                                style = MythosTypography.HeroTitle.copy(fontSize = 11.sp)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                MiniStatBadge("HP 10,000", MythosTokens.Success)
                                MiniStatBadge("ATK 2,800", MythosTokens.Damage)
                                MiniStatBadge("DEF 2,600", MythosTokens.DivineBlue)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Abilities Preview
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MythosTokens.BackgroundSurface)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = MythosTokens.PrimaryGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Passive — LAST STAND:",
                                style = MythosTypography.CardName,
                                color = MythosTokens.PrimaryGold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "< 30% HP triggers +25% ATK",
                                style = MythosTypography.CardDescription
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = MythosTokens.MythPowerFlame,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ultimate — TWELVE LABORS:",
                                style = MythosTypography.CardName,
                                color = MythosTokens.MythPowerFlame
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "100 MP deals 4,500 damage",
                                style = MythosTypography.CardDescription
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons via reusable MythosButton system
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MythosButton(
                    text = "START BATTLE (VS ARES)",
                    onClick = onStartBattle,
                    style = MythosButtonStyle.PRIMARY,
                    icon = Icons.Default.PlayArrow,
                    testTag = "start_battle_button",
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                )

                MythosButton(
                    text = "INSPECT CARD DECK",
                    subtitle = "10 Cards • Rarity Showcase",
                    onClick = onViewDeck,
                    style = MythosButtonStyle.SECONDARY,
                    icon = Icons.Default.Layers,
                    testTag = "view_deck_button",
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                )

                MythosButton(
                    text = "SHOP & SUMMONS",
                    subtitle = "Myth Gems • Bundles • Altar",
                    onClick = onOpenShop,
                    style = MythosButtonStyle.SECONDARY,
                    icon = Icons.Default.ShoppingCart,
                    testTag = "open_shop_button",
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                )

                TextButton(
                    onClick = { showGuideDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = MythosTokens.TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "How to Play & Resource Rules",
                        style = MythosTypography.HeroTitle.copy(fontSize = 12.sp)
                    )
                }
            }
        }

        // Gameplay Rules Guide Modal
        if (showGuideDialog) {
            Dialog(onDismissRequest = { showGuideDialog = false }) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MythosTokens.PrimaryGold, RoundedCornerShape(16.dp)),
                    color = MythosTokens.Panel
                ) {
                    Column(
                        modifier = Modifier
                            .padding(18.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "GAMEPLAY RULES & MECHANICS",
                            style = MythosTypography.GameTitle.copy(fontSize = 16.sp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        GuideSection(
                            title = "1. Energy (Max 10)",
                            body = "Used to play cards from hand. Starts at 5 and grows +1 max capacity each turn (up to 10). Automatically refills each turn."
                        )

                        GuideSection(
                            title = "2. Myth Power (Max 100)",
                            body = "Required to unleash Hercules' ultimate ability: TWELVE LABORS (100 MP). Increases as you play cards (+12 MP), take damage (+15 MP), and between turns (+10 MP)."
                        )

                        GuideSection(
                            title = "3. Passive: Last Stand",
                            body = "When Hercules falls below 30% HP (<= 3,000 HP), his Attack power increases by +25% for the rest of battle."
                        )

                        GuideSection(
                            title = "4. Combat & Shields",
                            body = "Cards deal physical/magical damage or provide defensive shields. Shields absorb incoming damage before health is lost."
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        MythosButton(
                            text = "GOT IT",
                            onClick = { showGuideDialog = false },
                            style = MythosButtonStyle.PRIMARY,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideSection(title: String, body: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = title, color = MythosTokens.LightGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(text = body, color = MythosTokens.TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun MiniStatBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.5.dp, color, RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(text = text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}
