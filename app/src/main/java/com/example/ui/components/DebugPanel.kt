package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.MythosGoldPrimary
import com.example.ui.theme.MythosRed
import com.example.viewmodel.BattleViewModel

@Composable
fun DebugPanelDialog(
    viewModel: BattleViewModel,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, Color(0xFFFF9800), RoundedCornerShape(16.dp)),
            color = Color(0xFF1B1824)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DEV DEBUG PANEL",
                            color = Color(0xFFFF9800),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Text(
                    text = "Development-only test tools for vertical slice verification",
                    color = Color(0xFFAFA7BA),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                // Grid of debug buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DebugButton(
                            text = "+5 Energy",
                            color = Color(0xFF22D3EE),
                            modifier = Modifier.weight(1f),
                            testTag = "debug_add_energy"
                        ) {
                            viewModel.debugAddEnergy(5)
                        }

                        DebugButton(
                            text = "+50 Myth Power",
                            color = Color(0xFFFBBF24),
                            modifier = Modifier.weight(1f),
                            testTag = "debug_add_myth_power"
                        ) {
                            viewModel.debugAddMythPower(50)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DebugButton(
                            text = "Hit Ares (-3000)",
                            color = Color(0xFFEF4444),
                            modifier = Modifier.weight(1f),
                            testTag = "debug_damage_enemy"
                        ) {
                            viewModel.debugDamageEnemy(3000)
                        }

                        DebugButton(
                            text = "Heal Herc (+3000)",
                            color = Color(0xFF10B981),
                            modifier = Modifier.weight(1f),
                            testTag = "debug_heal_hercules"
                        ) {
                            viewModel.debugHealHercules(3000)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DebugButton(
                            text = "Set Herc <30% HP (Last Stand)",
                            color = Color(0xFFFF5722),
                            modifier = Modifier.weight(1f),
                            testTag = "debug_last_stand"
                        ) {
                            viewModel.debugSetHerculesLowHp(2500)
                        }

                        DebugButton(
                            text = "Reset Battle",
                            color = Color(0xFF9E9E9E),
                            modifier = Modifier.weight(1f),
                            testTag = "debug_reset_battle"
                        ) {
                            viewModel.startNewBattle()
                            onDismiss()
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DebugButton(
                            text = "Force Victory",
                            color = MythosGoldPrimary,
                            modifier = Modifier.weight(1f),
                            testTag = "debug_force_victory"
                        ) {
                            viewModel.debugForceVictory()
                            onDismiss()
                        }

                        DebugButton(
                            text = "Force Defeat",
                            color = MythosRed,
                            modifier = Modifier.weight(1f),
                            testTag = "debug_force_defeat"
                        ) {
                            viewModel.debugForceDefeat()
                            onDismiss()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DebugButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(42.dp)
            .testTag(testTag),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.2f),
            contentColor = color
        ),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
