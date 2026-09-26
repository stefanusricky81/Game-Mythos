package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.SoundManager
import com.example.monetization.PlayerEconomyRepository
import com.example.ui.screens.BattleScreen
import com.example.ui.screens.CampaignScreen
import com.example.ui.screens.CollectionScreen
import com.example.ui.screens.DeckBuilderScreen
import com.example.ui.screens.DeckInspectScreen
import com.example.ui.screens.HeroSelectionScreen
import com.example.ui.screens.ShopScreen
import com.example.ui.screens.TitleScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.MythosDarkBg
import com.example.viewmodel.BattleViewModel

enum class MythosScreen {
    TITLE,
    CAMPAIGN,
    HERO_SELECTION,
    BATTLE,
    DECK_INSPECT,
    DECK_BUILDER,
    SHOP,
    COLLECTION
}

class MainActivity : ComponentActivity() {

    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize local persistent collection & economy (Phase 6A Requirement #12)
        PlayerEconomyRepository.instance.initPersistence(this)

        soundManager = SoundManager(this)

        setContent {
            MyApplicationTheme {
                val battleViewModel: BattleViewModel = viewModel()
                LaunchedEffect(Unit) {
                    battleViewModel.setSoundManager(soundManager)
                }

                var currentScreen by remember { mutableStateOf(MythosScreen.TITLE) }
                var focusedDeckBuilderCardId by remember { mutableStateOf<String?>(null) }

                BackHandler(enabled = currentScreen != MythosScreen.TITLE) {
                    currentScreen = when (currentScreen) {
                        MythosScreen.DECK_BUILDER -> MythosScreen.COLLECTION
                        MythosScreen.BATTLE -> if (battleViewModel.activeEncounterConfig != null) MythosScreen.CAMPAIGN else MythosScreen.TITLE
                        else -> MythosScreen.TITLE
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MythosDarkBg
                ) {
                    when (currentScreen) {
                        MythosScreen.TITLE -> {
                            TitleScreen(
                                onStartBattle = {
                                    battleViewModel.startNewBattle()
                                    currentScreen = MythosScreen.BATTLE
                                },
                                onOpenCampaign = {
                                    currentScreen = MythosScreen.CAMPAIGN
                                },
                                onOpenHeroSelection = {
                                    currentScreen = MythosScreen.HERO_SELECTION
                                },
                                onViewDeck = {
                                    currentScreen = MythosScreen.DECK_INSPECT
                                },
                                onOpenShop = {
                                    currentScreen = MythosScreen.SHOP
                                },
                                onOpenCollection = {
                                    currentScreen = MythosScreen.COLLECTION
                                },
                                onOpenDeckBuilder = {
                                    focusedDeckBuilderCardId = null
                                    currentScreen = MythosScreen.DECK_BUILDER
                                }
                            )
                        }
                        MythosScreen.CAMPAIGN -> {
                            CampaignScreen(
                                onNavigateBack = {
                                    currentScreen = MythosScreen.TITLE
                                },
                                onStartStageBattle = { config ->
                                    battleViewModel.startNewBattle(encounterConfig = config)
                                    currentScreen = MythosScreen.BATTLE
                                }
                            )
                        }
                        MythosScreen.HERO_SELECTION -> {
                            HeroSelectionScreen(
                                onNavigateBack = {
                                    currentScreen = MythosScreen.TITLE
                                }
                            )
                        }
                        MythosScreen.BATTLE -> {
                            BattleScreen(
                                viewModel = battleViewModel,
                                onNavigateBack = {
                                    currentScreen = if (battleViewModel.activeEncounterConfig != null) {
                                        MythosScreen.CAMPAIGN
                                    } else {
                                        MythosScreen.TITLE
                                    }
                                },
                                onOpenDeckBuilder = {
                                    focusedDeckBuilderCardId = null
                                    currentScreen = MythosScreen.DECK_BUILDER
                                },
                                onGoToCollection = {
                                    currentScreen = MythosScreen.COLLECTION
                                }
                            )
                        }
                        MythosScreen.DECK_INSPECT -> {
                            DeckInspectScreen(
                                onNavigateBack = {
                                    currentScreen = MythosScreen.TITLE
                                },
                                onOpenDeckBuilder = {
                                    focusedDeckBuilderCardId = null
                                    currentScreen = MythosScreen.DECK_BUILDER
                                }
                            )
                        }
                        MythosScreen.DECK_BUILDER -> {
                            DeckBuilderScreen(
                                initialFocusCardId = focusedDeckBuilderCardId,
                                onNavigateBack = {
                                    focusedDeckBuilderCardId = null
                                    currentScreen = MythosScreen.COLLECTION
                                },
                                onStartBattleWithDeck = {
                                    focusedDeckBuilderCardId = null
                                    battleViewModel.startNewBattle()
                                    currentScreen = MythosScreen.BATTLE
                                }
                            )
                        }
                        MythosScreen.SHOP -> {
                            ShopScreen(
                                onNavigateBack = {
                                    currentScreen = MythosScreen.TITLE
                                }
                            )
                        }
                        MythosScreen.COLLECTION -> {
                            CollectionScreen(
                                onNavigateBack = {
                                    currentScreen = MythosScreen.TITLE
                                },
                                onOpenDeckBuilder = { targetCardId ->
                                    focusedDeckBuilderCardId = targetCardId
                                    currentScreen = MythosScreen.DECK_BUILDER
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Android") }
}
