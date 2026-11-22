package com.zirakusa.clima.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zirakusa.clima.data.City
import com.zirakusa.clima.data.Fetch
import com.zirakusa.clima.data.Forecast
import com.zirakusa.clima.data.KeyValues
import com.zirakusa.clima.data.Places
import com.zirakusa.clima.data.Settings
import com.zirakusa.clima.data.TempUnit
import com.zirakusa.clima.data.WeatherService
import com.zirakusa.clima.data.WindUnit
import com.zirakusa.clima.ui.theme.Accent
import com.zirakusa.clima.ui.theme.ThemeMode
import java.io.IOException

/** What covers the weather: nothing, «Lugares», Ajustes or Modo MCP. */
enum class Page { MAIN, PLACES, SETTINGS, MCP }

/**
 * The only owner of Clima's state: the places, the one shown, each forecast (saved ones at once, new ones when old),
 * what is loading or failed, the place search and the settings. [launch] runs the internet work (the activity's
 * scope; tests run it at once). Screens only paint and call.
 */
class ClimaState(kv: KeyValues, fetch: Fetch, private val launch: (suspend () -> Unit) -> Unit,
    val now: () -> Long = System::currentTimeMillis, private val language: String = "es") {
    private val settings = Settings(kv)
    private val places = Places(kv)
    private val service = WeatherService(places, fetch, now)

    var page by mutableStateOf(Page.MAIN)
    var shown by mutableStateOf(emptyList<City>()); private set
    var index by mutableIntStateOf(0); private set
    var forecasts by mutableStateOf(emptyMap<Long, Forecast>()); private set
    var loading by mutableStateOf(emptySet<Long>()); private set
    var failed by mutableStateOf(emptySet<Long>()); private set
    val current get() = shown.getOrNull(index)
    val forecast get() = current?.let { forecasts[it.id] }

    var query by mutableStateOf("")
    var results by mutableStateOf(emptyList<City>()); private set
    var searching by mutableStateOf(false); private set
    var searchFailed by mutableStateOf(false); private set

    /** The phone uses 24 h (the activity says). */
    var h24 by mutableStateOf(true)
    var temp by mutableStateOf(settings.temp); private set
    var wind by mutableStateOf(settings.wind); private set
    var hereOn by mutableStateOf(settings.here); private set
    var themeMode by mutableStateOf(settings.theme?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.BLACK); private set
    var accent by mutableStateOf(settings.accent?.let { runCatching { Accent.valueOf(it) }.getOrNull() } ?: Accent.NARANJA); private set

    init { reload() }

    /** Reads places and saved forecasts again (after MCP changed them), keeping the place shown. */
    fun reload() {
        val keep = current?.id ?: places.selected
        shown = Places.shown(places.list(), places.here.takeIf { hereOn })
        index = shown.indexOfFirst { it.id == keep }.coerceAtLeast(0)
        forecasts = shown.mapNotNull { c -> places.cached(c.id)?.let { c.id to it } }.toMap()
    }

    fun select(i: Int) {
        if (i !in shown.indices) return
        index = i; places.selected = shown[i].id
        refresh()
    }

    /** A new forecast for the place shown when its saved one is old (or always with [force]). */
    fun refresh(force: Boolean = false) {
        val city = current ?: return
        if (city.id in loading || (!force && forecasts[city.id]?.fresh(now()) == true)) return
        loading = loading + city.id
        launch {
            val out = service.forecast(city, force)
            out.forecast?.let { forecasts = forecasts + (city.id to it) }
            failed = if (out.failed) failed + city.id else failed - city.id
            loading = loading - city.id
        }
    }

    /** Looks for places named like [query] (the screen waits a moment after typing). */
    fun search() {
        val q = query.trim()
        if (q.length < 2) { results = emptyList(); searchFailed = false; return }
        searching = true
        launch {
            try { results = service.search(q, language).filter { r -> shown.none { it.id == r.id } }; searchFailed = false }
            catch (_: IOException) { results = emptyList(); searchFailed = true }
            catch (_: IllegalArgumentException) { results = emptyList() }
            searching = false
        }
    }

    /** Saves a place from the search and shows it; false when full. */
    fun add(city: City): Boolean {
        if (!places.add(city)) return false
        query = ""; results = emptyList()
        reload(); select(shown.indexOfFirst { it.id == city.id }); page = Page.MAIN
        return true
    }

    fun remove(city: City) {
        if (city.id == City.HERE) return changeHere(false)
        places.remove(city.id); reload()
    }

    fun move(city: City, by: Int) {
        val saved = places.list()
        val i = saved.indexOfFirst { it.id == city.id }.takeIf { it >= 0 } ?: return
        places.move(city.id, i + by); reload()
    }

    /** The phone said where it is: «Mi ubicación» gets those coordinates. */
    fun located(lat: Double, lon: Double, name: String) {
        if (!City.valid(lat, lon)) return
        val before = places.here
        places.here = City(City.HERE, name, lat, lon)
        val moved = before == null || kotlin.math.abs(before.lat - lat) + kotlin.math.abs(before.lon - lon) > 0.05
        reload()
        if (moved && hereOn) { index = 0; refresh(force = true) }
    }

    fun changeHere(on: Boolean) { hereOn = on; settings.here = on; reload(); if (on) index = 0 }
    fun changeTemp(u: TempUnit) { temp = u; settings.temp = u }
    fun changeWind(u: WindUnit) { wind = u; settings.wind = u }
    fun changeTheme(mode: ThemeMode) { themeMode = mode; settings.theme = mode.name }
    fun changeAccent(next: Accent) { accent = next; settings.accent = next.name }
}
