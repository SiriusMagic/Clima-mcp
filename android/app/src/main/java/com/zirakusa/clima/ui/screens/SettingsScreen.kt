package com.zirakusa.clima.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zirakusa.clima.BuildConfig
import com.zirakusa.clima.data.TempUnit
import com.zirakusa.clima.data.WindUnit
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.sp
import com.zirakusa.clima.R
import com.zirakusa.clima.mcp.McpMode
import com.zirakusa.clima.mcp.RelayLink
import com.zirakusa.clima.ui.Page
import com.zirakusa.clima.ui.ClimaState
import com.zirakusa.clima.ui.theme.ClimaColors

/** «Ajustes»: units, «Mi ubicación», Modo MCP, Temas, privacy and version. */
@Composable fun SettingsScreen(state: ClimaState, onLocation: () -> Unit) {
    var privacy by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(ClimaColors.Background).systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PageTop(stringResource(R.string.settings)) { state.page = Page.MAIN }
        Caption(stringResource(R.string.units))
        Group {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.temperature), fontSize = 18.sp)
                Segmented(TempUnit.entries.map { it.symbol }, state.temp.ordinal, Modifier.fillMaxWidth()) { state.changeTemp(TempUnit.entries[it]) }
                Text(stringResource(R.string.wind), fontSize = 18.sp)
                Segmented(WindUnit.entries.map { it.symbol }, state.wind.ordinal, Modifier.fillMaxWidth()) { state.changeWind(WindUnit.entries[it]) }
            }
        }
        Group { SwitchItem(stringResource(R.string.here), stringResource(R.string.here_hint), state.hereOn) { on -> if (on) onLocation() else state.changeHere(false) } }
        Group {
            val status = when {
                !McpMode.active -> stringResource(R.string.mcp_status_off)
                McpMode.hubRedirect -> stringResource(R.string.mcp_redirect)
                McpMode.state == RelayLink.State.ONLINE -> stringResource(R.string.mcp_online)
                McpMode.state == RelayLink.State.CONNECTING -> stringResource(R.string.mcp_connecting)
                else -> stringResource(R.string.mcp_offline)
            }
            Item(stringResource(R.string.mcp_title), "$status · ${stringResource(R.string.mcp_settings_hint)}") { state.page = Page.MCP }
        }
        ThemeCard(state.themeMode, state.accent, state::changeTheme, state::changeAccent)
        Caption(stringResource(R.string.about))
        Group {
            Item(stringResource(R.string.privacy), null) { privacy = true }
            Divider()
            Item(stringResource(R.string.version_title), BuildConfig.VERSION_NAME, onClick = null)
        }
        Text("", Modifier.padding(bottom = 16.dp))
    }
    if (privacy) AlertDialog({ privacy = false }, confirmButton = { TextButton({ privacy = false }) { Text(stringResource(R.string.done), color = ClimaColors.AccentText) } },
        title = { Text(stringResource(R.string.privacy)) }, text = { Text(stringResource(R.string.privacy_text)) })
}
