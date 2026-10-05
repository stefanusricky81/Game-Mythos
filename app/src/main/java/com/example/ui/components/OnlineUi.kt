package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.backend.MythosBackend
import com.example.backend.online.OnlineStatus
import com.example.monetization.PlayerEconomyRepository
import com.example.monetization.PlayerEconomyState
import com.example.ui.theme.MythosTokens
import kotlinx.coroutines.flow.combine

/**
 * The economy state a screen should DISPLAY. LOCAL_DEVELOPMENT: exactly the offline economy (a plain
 * collectAsState, i.e. no behaviour change). ONLINE_AUTHORITATIVE: the offline economy with the server's
 * Arena / World Boss / Raid / Event wallet / Alliance state overlaid, so the existing screens show
 * server truth without being rewritten.
 */
@Composable
fun rememberDisplayEconomyState(): State<PlayerEconomyState> {
    val repository = PlayerEconomyRepository.instance
    if (!MythosBackend.isOnline) return repository.economyState.collectAsState()

    val online = MythosBackend.online
    val flow = remember { combine(repository.economyState, online.snapshot) { economy, snapshot -> online.overlay(economy, snapshot) } }
    return flow.collectAsState(initial = online.overlay(repository.economyState.value, online.snapshot.value))
}

/** Renders nothing in LOCAL_DEVELOPMENT. Online: syncing / offline / refused state with a retry action. */
@Composable
fun OnlineStatusBanner(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    if (!MythosBackend.isOnline) return
    val status = MythosBackend.online.status.collectAsState().value
    val (text, color) = when (status) {
        OnlineStatus.Idle -> return
        OnlineStatus.Ready -> return
        OnlineStatus.Syncing -> "Syncing with the server..." to Color(0xFFC7C1D4)
        is OnlineStatus.Error -> status.message to if (status.offline) Color(0xFFFFD27A) else Color(0xFFFF8A80)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MythosTokens.PanelElevated)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("online_status_banner"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = text, color = color, fontSize = 11.sp, modifier = Modifier.weight(1f))
        if (status is OnlineStatus.Error) {
            TextButton(onClick = onRetry) { Text("RETRY", color = MythosTokens.PrimaryGold, fontSize = 11.sp) }
        }
    }
}
