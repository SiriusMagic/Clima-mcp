package com.zirakusa.clima

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.SideEffect
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.zirakusa.clima.data.SharedValues
import com.zirakusa.clima.mcp.McpMode
import com.zirakusa.clima.ui.AppActions
import com.zirakusa.clima.ui.ClimaApp
import com.zirakusa.clima.ui.ClimaState
import com.zirakusa.clima.ui.Page
import com.zirakusa.clima.ui.theme.ClimaColors
import com.zirakusa.clima.ui.theme.ClimaTheme
import kotlinx.coroutines.launch

/** Creates the state, asks for the location when the user wants «Mi ubicación», refreshes on return and does what the screens ask. */
class MainActivity : ComponentActivity() {
    private lateinit var state: ClimaState
    private val location = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) locate(turnOn = true) else Toast.makeText(this, R.string.location_denied, Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppIcon.load(this)
        enableEdgeToEdge()
        state = ClimaState(SharedValues(this), Net, launch = { block -> lifecycleScope.launch { block() } },
            language = resources.configuration.locales[0].language)
        McpMode.onChanged = { state.reload() }
        McpMode.load(this)
        val actions = AppActions(location = ::askLocation, toast = { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() },
            copyLink = ::copyLink, goToHub = ::goToHub)
        setContent {
            ClimaTheme(state.themeMode, state.accent) {
                // The weather is always on a sky (white text); the other pages follow the theme.
                val light = ClimaColors.isLight && state.page != Page.MAIN
                SideEffect { WindowCompat.getInsetsController(window, window.decorView).run { isAppearanceLightStatusBars = light; isAppearanceLightNavigationBars = light } }
                BackHandler(state.page != Page.MAIN) { state.page = if (state.page == Page.MCP) Page.SETTINGS else Page.MAIN }
                ClimaApp(state, actions)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        state.h24 = DateFormat.is24HourFormat(this)
        state.reload()
        if (state.hereOn && allowed()) locate(turnOn = false)
        state.refresh()
    }

    private fun allowed() = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun askLocation() = if (allowed()) locate(turnOn = true) else location.launch(Manifest.permission.ACCESS_COARSE_LOCATION)

    private fun locate(turnOn: Boolean) {
        if (turnOn) state.changeHere(true)
        Here.find(this) { where ->
            if (where != null) state.located(where.latitude, where.longitude, getString(R.string.here))
            else if (turnOn) Toast.makeText(this, R.string.location_unknown, Toast.LENGTH_LONG).show()
            if (turnOn) state.page = Page.MAIN
        }
    }

    private fun copyLink(link: String) {
        getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("Clima MCP", link))
        Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
    }

    /** «Ir al Hub»: copies the hub's link (in case it must be pasted) and opens the Gateway Hub app. */
    private fun goToHub() {
        getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("Clima › Gateway Hub", McpMode.hubLink()))
        val open = packageManager.getLaunchIntentForPackage("com.zirakusa.gatewayhub")
        if (open != null) startActivity(open) else Toast.makeText(this, R.string.mcp_paste_in_hub, Toast.LENGTH_LONG).show()
    }
}
