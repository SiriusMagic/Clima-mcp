package com.zirakusa.clima.ui

import androidx.compose.runtime.Composable
import com.zirakusa.clima.ui.screens.PlacesScreen
import com.zirakusa.clima.ui.screens.SettingsScreen
import com.zirakusa.clima.ui.screens.WeatherScreen

/** What the screens ask of the phone (the activity gives the real ones; tests leave the defaults). */
class AppActions(
    /** «Mi ubicación»: asks for the approximate location, then gives it to the state. */
    val location: () -> Unit = {},
    val toast: (String) -> Unit = {},
    val openArka: () -> Unit = {},
)

/** The whole app: the weather, or a page over it. */
@Composable fun ClimaApp(state: ClimaState, actions: AppActions = AppActions()) = when (state.page) {
    Page.MAIN -> WeatherScreen(state, actions.location)
    Page.PLACES -> PlacesScreen(state, actions.toast, actions.location)
    Page.SETTINGS -> SettingsScreen(state, actions.location, actions.openArka)
}
