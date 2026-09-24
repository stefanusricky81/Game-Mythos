package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.Hero

/**
 * HeroStatusView: Facade forwarding to the unified, reusable HeroStatusPanel.
 */
@Composable
fun HeroStatusView(
    hero: Hero,
    isPlayer: Boolean,
    modifier: Modifier = Modifier
) {
    HeroStatusPanel(
        hero = hero,
        isPlayer = isPlayer,
        modifier = modifier
    )
}
