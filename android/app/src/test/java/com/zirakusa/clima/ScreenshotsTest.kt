package com.zirakusa.clima

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.zirakusa.clima.data.City
import com.zirakusa.clima.data.MapValues
import com.zirakusa.clima.data.Places
import com.zirakusa.clima.ui.ClimaApp
import com.zirakusa.clima.ui.ClimaState
import com.zirakusa.clima.ui.Page
import com.zirakusa.clima.ui.theme.ClimaTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Every screen, painted: welcome, the weather, loading bones, offline, «Lugares» with a search, Ajustes and Modo MCP. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "es-w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotsTest {
    @get:Rule val compose = createComposeRule()
    private val values = MapValues()
    private val net = Fixtures.Internet()
    private val quito = City(3652462, "Quito", -0.2299, -78.5249, "Pichincha", "Ecuador")

    private fun shot(name: String) { compose.waitForIdle(); onView(isRoot()).captureRoboImage("build/screenshots/clima-$name.png") }
    private fun show(state: ClimaState) = compose.setContent { ClimaTheme(state.themeMode, state.accent) { ClimaApp(state) } }
    private fun state(run: Boolean = true) = ClimaState(values, net, launch = { if (run) runBlocking { it() } }, now = { 1_000L })

    @Test fun welcomeWeatherAndPlaces() {
        val state = state()
        show(state)
        compose.onNodeWithText("¿Qué tiempo hace?").assertExists()
        shot("welcome")
        Places(values).add(quito); state.reload(); state.refresh()
        compose.onNodeWithText("Quito").assertExists()
        compose.onNodeWithText("Parcialmente nublado").assertExists()
        compose.onNodeWithText("☂ Lleva paraguas").assertExists()
        compose.onNodeWithText("Máx 19° · Mín 9°").assertExists()
        compose.onNodeWithText("Actualizado ahora").assertExists()
        shot("weather")
        state.page = Page.PLACES; state.query = "Cuenca"; state.search()
        compose.onNodeWithText("Azuay, Ecuador").assertExists()
        shot("places")
        state.page = Page.SETTINGS
        compose.onNodeWithText("Unidades").assertExists()
        shot("settings")
        state.page = Page.MCP
        compose.onNodeWithText("Activar MCP").assertExists()
        shot("mcp")
    }

    @Test fun loadingAndOffline() {
        Places(values).add(quito)
        val loading = state(run = false)
        loading.refresh()
        show(loading)
        compose.onNodeWithText("Quito").assertExists()
        shot("loading")
    }

    @Test fun offline() {
        Places(values).add(quito)
        net.online = false
        val state = state()
        state.refresh()
        show(state)
        compose.onNodeWithText("Reintentar").assertExists()
        shot("offline")
    }
}
