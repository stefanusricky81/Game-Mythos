package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.Card

/**
 * CardView: Facade forwarding directly to the unified, data-driven CardFrame component.
 */
@Composable
fun CardView(
    card: Card,
    isPlayable: Boolean,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    isShaking: Boolean = false,
    canAfford: Boolean = true,
    onClick: () -> Unit = {}
) {
    CardFrame(
        card = card,
        isPlayable = isPlayable,
        modifier = modifier,
        canAfford = canAfford,
        isShaking = isShaking,
        onClick = onClick
    )
}
