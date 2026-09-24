package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.data.Card
import com.example.ui.theme.MythosTokens

/**
 * Dedicated, independent Card Artwork Container.
 * Cleanly separates artwork presentation from card frame and stats.
 * Allows instant swapping with PNG/WebP assets without modifying CardFrame, CardData, or combat logic.
 */
@Composable
fun CardArtwork(
    card: Card,
    modifier: Modifier = Modifier
) {
    val typeColor = MythosTokens.getCardTypeColor(card.type)
    val rarityColor = MythosTokens.getRarityColor(card.rarity)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        typeColor.copy(alpha = 0.35f),
                        MythosTokens.DarkNavyDeep,
                        Color(0xFF07060A)
                    )
                )
            )
            .border(
                0.5.dp,
                rarityColor.copy(alpha = 0.4f),
                RoundedCornerShape(6.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (card.artworkResId != null) {
            // Replaces procedural art when production PNG/WebP asset is assigned
            Image(
                painter = painterResource(id = card.artworkResId),
                contentDescription = card.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Clean, distinctive mythological placeholder artwork
            val iconVector: ImageVector = when (card.iconKey) {
                "strike", "crush" -> Icons.Default.Bolt
                "shield", "armor" -> Icons.Default.Shield
                "fire" -> Icons.Default.LocalFireDepartment
                "lightning", "thunder" -> Icons.Default.FlashOn
                "poison" -> Icons.Default.Coronavirus
                "spears" -> Icons.Default.Security
                "chalice" -> Icons.Default.EmojiEvents
                else -> Icons.Default.Star
            }

            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = typeColor,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
