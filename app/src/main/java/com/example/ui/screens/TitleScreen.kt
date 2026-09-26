package com.example.ui.screens

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
import com.example.data.DeckValidationResult
import com.example.data.DeckValidator
import com.example.data.HerculesIdentity
import com.example.data.HeroCatalog
import com.example.monetization.PlayerEconomyRepository
import com.example.ui.components.MythosButton
import com.example.ui.components.MythosButtonStyle
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun TitleScreen(
    onStartBattle: () -> Unit,
    onOpenCampaign: () -> Unit = {},
    onOpenHeroSelection: () -> Unit = {},
    onViewDeck: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenCollection: () -> Unit = {},
    onOpenDeckBuilder: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showGuideDialog by remember { mutableStateOf(false) }
    var showInvalidDeckDialog by remember { mutableStateOf(false) }
    var invalidDeckResult by remember { mutableStateOf<DeckValidationResult?>(null) }
    val economyState by PlayerEconomyRepository.instance.economyState.collectAsState()
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }
    val currentHero = HeroCatalog.findHero(economyState.selectedHeroId) ?: HeroCatalog.HERCULES

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

            // Main Hero Showcase: Dynamic Active Champion (Clickable to switch heroes)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MythosTokens.SecondaryGold, RoundedCornerShape(16.dp))
                    .clickable { onOpenHeroSelection() }
                    .testTag("hero_showcase_card"),
                colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
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
                            Box(
                                modifier = Modifier
                                    .size(74.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, MythosTokens.PrimaryGold, RoundedCornerShape(10.dp))
                            ) {
                                Image(
                                    painter = painterResource(id = currentHero.portraitResId),
                                    contentDescription = currentHero.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = currentHero.name.uppercase(),
                                    style = MythosTypography.HeroName.copy(fontSize = 18.sp),
                                    color = MythosTokens.PrimaryGold
                                )
                                Text(
                                    text = "${currentHero.title} • ${currentHero.faction}",
                                    style = MythosTypography.HeroTitle.copy(fontSize = 11.sp)
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    MiniStatBadge("HP ${numberFormat.format(currentHero.baseHp)}", MythosTokens.Success)
                                    MiniStatBadge("ATK ${numberFormat.format(currentHero.baseAttack)}", MythosTokens.Damage)
                                    MiniStatBadge("DEF ${numberFormat.format(currentHero.baseDefense)}", MythosTokens.DivineBlue)
                                }
                            }
                        }

                        // Switch Hero pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MythosTokens.PanelElevated)
                                .border(0.5.dp, MythosTokens.PrimaryGold.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Switch",
                                    tint = MythosTokens.PrimaryGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "HEROES",
                                    style = MythosTypography.RarityLabel.copy(fontSize = 9.sp),
                                    color = MythosTokens.PrimaryGold
                                )
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
                                text = "Passive — ${currentHero.passiveName}:",
                                style = MythosTypography.CardName,
                                color = MythosTokens.PrimaryGold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentHero.passiveDescription,
                                style = MythosTypography.CardDescription,
                                maxLines = 1
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
                                text = "Ultimate — ${currentHero.ultimateName}:",
                                style = MythosTypography.CardName,
                                color = MythosTokens.MythPowerFlame
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentHero.ultimateDescription,
                                style = MythosTypography.CardDescription,
                                maxLines = 1
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
                // Primary CTA: CAMPAIGN (Phase 7A Requirement)
                MythosButton(
                    text = "CAMPAIGN",
                    subtitle = "World 1 • Aegean / Olympus • 5 Stages",
                    onClick = onOpenCampaign,
                    style = MythosButtonStyle.PRIMARY,
                    icon = Icons.Default.Explore,
                    testTag = "open_campaign_button",
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                )

                // Hero Selection
                MythosButton(
                    text = "HERO SELECTION",
                    subtitle = "Choose Champion • Hercules, Achilles, Merlin",
                    onClick = onOpenHeroSelection,
                    style = MythosButtonStyle.SECONDARY,
                    icon = Icons.Default.Person,
                    testTag = "open_hero_selection_button",
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                )

                MythosButton(
                    text = "COLLECTION",
                    subtitle = "Cards, Heroes & Progression",
                    onClick = onOpenCollection,
                    style = MythosButtonStyle.SECONDARY,
                    icon = Icons.Default.CollectionsBookmark,
                    testTag = "open_collection_button",
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                )

                MythosButton(
                    text = "INSPECT CARD DECK",
                    subtitle = "Active Deck • 20 Cards",
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

                // Quick direct battle retained for development and test coverage
                MythosButton(
                    text = "DIRECT BATTLE (VS ARES)",
                    subtitle = "Quick Battle Testing",
                    onClick = {
                        val validation = DeckValidator.validate(economyState.activeDeck, economyState.ownedCardCounts)
                        if (!validation.isValid) {
                            invalidDeckResult = validation
                            showInvalidDeckDialog = true
                        } else {
                            onStartBattle()
                        }
                    },
                    style = MythosButtonStyle.SECONDARY,
                    icon = Icons.Default.PlayArrow,
                    testTag = "start_battle_button",
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

        // Invalid Deck Warning Modal (Requirement #4, #17)
        if (showInvalidDeckDialog) {
            Dialog(onDismissRequest = { showInvalidDeckDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, MythosTokens.Damage, RoundedCornerShape(16.dp))
                        .testTag("invalid_deck_dialog"),
                    colors = CardDefaults.cardColors(containerColor = MythosTokens.Panel)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MythosTokens.Damage.copy(alpha = 0.2f))
                                .border(1.dp, MythosTokens.Damage, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MythosTokens.Damage,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = "Your deck is invalid.",
                            style = MythosTypography.GameTitle.copy(fontSize = 20.sp),
                            color = MythosTokens.Damage,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Create and save a valid 20-card deck before entering battle.",
                            style = MythosTypography.HeroTitle.copy(fontSize = 13.sp),
                            color = MythosTokens.TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        val errorMsg = invalidDeckResult?.primaryErrorMessage
                            ?: "Deck must contain exactly 20 cards within card ownership limits."
                        Surface(
                            color = MythosTokens.BackgroundSurface,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.8.dp, MythosTokens.PanelBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMsg,
                                style = MythosTypography.CardDescription.copy(fontSize = 11.sp),
                                color = MythosTokens.TextSecondary,
                                modifier = Modifier.padding(10.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                showInvalidDeckDialog = false
                                onOpenDeckBuilder()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("go_to_deck_builder_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MythosTokens.PrimaryGold,
                                contentColor = Color(0xFF161202)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("GO TO DECK BUILDER", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showInvalidDeckDialog = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("cancel_invalid_deck_dialog"),
                            border = BorderStroke(1.dp, MythosTokens.PanelBorder),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("CANCEL", fontSize = 12.sp, color = MythosTokens.TextMuted)
                        }
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
