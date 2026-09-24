package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.HerculesIdentity
import com.example.ui.theme.MythPowerFlame
import com.example.ui.theme.MythPowerGold
import com.example.ui.theme.MythosGoldLight
import com.example.ui.theme.MythosGoldPrimary

@Composable
fun TwelveLaborsCinematic(
    modifier: Modifier = Modifier
) {
    val scaleAnim = remember { Animatable(0.7f) }
    val alphaAnim = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "rays")
    val rayAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ray_alpha"
    )

    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, animationSpec = tween(300))
        scaleAnim.animateTo(1.05f, animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f * alphaAnim.value)),
        contentAlignment = Alignment.Center
    ) {
        // Shockwave aura
        Box(
            modifier = Modifier
                .size(340.dp)
                .scale(scaleAnim.value)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.radialGradient(
                        listOf(
                            MythPowerFlame.copy(alpha = 0.6f * rayAlpha),
                            MythPowerGold.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Main cinematic frame
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .scale(scaleAnim.value)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF2B1D0C),
                            Color(0xFF140F0A),
                            Color(0xFF1C1309)
                        )
                    )
                )
                .border(2.dp, MythosGoldPrimary, RoundedCornerShape(20.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Banner Header
            Text(
                text = "⚡ HEROIC ULTIMATE ⚡",
                color = MythosGoldPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Cinematic Art of Twelve Labors
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, MythPowerGold, RoundedCornerShape(14.dp))
            ) {
                Image(
                    painter = painterResource(id = HerculesIdentity.assets.twelveLabors.resolveResId()),
                    contentDescription = "Twelve Labors Ultimate",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Golden radiant gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.6f)
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Ability Title
            Text(
                text = "TWELVE LABORS",
                color = MythosGoldLight,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "CONSTELLATION OF THE NEMEAN BEAST",
                color = MythPowerGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Hercules unleashes celestial strikes forged through twelve mythical trials, unleashing a devastating 4,500 damage burst!",
                color = Color(0xFFE8DFC8),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Damage readout badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF5A1616))
                    .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "💥 DEVASTATING STRIKE 💥",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
