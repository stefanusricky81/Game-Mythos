package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import com.example.ui.screens.BattleScreen
import com.example.ui.screens.DeckInspectScreen
import com.example.ui.screens.ShopScreen
import com.example.ui.screens.TitleScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.MythosDarkBg
import com.example.viewmodel.BattleViewModel

enum class MythosScreen {
    TITLE,
    BATTLE,
    DECK_INSPECT,
    SHOP
}

class MainActivity : ComponentActivity() {

    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        soundManager = SoundManager(this)

        setContent {
            MyApplicationTheme {
                val battleViewModel: BattleViewModel = viewModel()
                LaunchedEffect(Unit) {
                    battleViewModel.setSoundManager(soundManager)
                }

                var currentScreen by remember { mutableStateOf(MythosScreen.TITLE) }

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
                                onViewDeck = {
                                    currentScreen = MythosScreen.DECK_INSPECT
                                },
                                onOpenShop = {
                                    currentScreen = MythosScreen.SHOP
                                }
                            )
                        }
                        MythosScreen.BATTLE -> {
                            BattleScreen(
                                viewModel = battleViewModel,
                                onNavigateBack = {
                                    currentScreen = MythosScreen.TITLE
                                }
                            )
                        }
                        MythosScreen.DECK_INSPECT -> {
                            DeckInspectScreen(
                                onNavigateBack = {
                                    currentScreen = MythosScreen.TITLE
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
