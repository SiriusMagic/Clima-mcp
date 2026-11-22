package com.zirakusa.clima

import com.zirakusa.clima.data.Advice
import com.zirakusa.clima.data.Api
import com.zirakusa.clima.data.City
import com.zirakusa.clima.data.Forecast
import com.zirakusa.clima.data.MapValues
import com.zirakusa.clima.data.Places
import com.zirakusa.clima.data.Sky
import com.zirakusa.clima.data.TempUnit
import com.zirakusa.clima.data.Tip
import com.zirakusa.clima.data.Units
import com.zirakusa.clima.data.WeatherService
import com.zirakusa.clima.data.WindUnit
import com.zirakusa.clima.ui.ClimaState
import com.zirakusa.clima.ui.Page
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime

/** The pure rules: reading Open-Meteo, units, tips, places, the two fixed addresses, the saved forecast offline, the summary and the state. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WeatherMathTest {
    private val quito = City(3652462, "Quito", -0.2299, -78.5249, "Pichincha", "Ecuador")

    @Test fun readsTheForecast() {
        val f = Forecast.parse(Fixtures.forecast, 1000)
        assertEquals(LocalDateTime.of(2026, 11, 21, 9, 0), f.now.time)
        assertEquals(14.2, f.now.temp, 0.0); assertEquals(Sky.PARTLY_CLOUDY, f.now.sky); assertTrue(f.now.day)
        assertEquals(7, f.days.size); assertEquals(168, f.hours.size); assertEquals("America/Guayaquil", f.zone)
        assertEquals(LocalDate.of(2026, 11, 21), f.today!!.date); assertEquals(19.4, f.today!!.max, 0.0)
        assertEquals(LocalDateTime.of(2026, 11, 21, 6, 2), f.today!!.sunrise)
        assertEquals(LocalDateTime.of(2026, 11, 21, 9, 0), f.nextHours(24).first().time); assertEquals(24, f.nextHours(24).size)
        assertEquals(Sky.STORM, Sky.of(96)); assertEquals(Sky.SHOWERS, Sky.of(81)); assertEquals(Sky.CLOUDY, Sky.of(1234))
        assertTrue(f.fresh(1000 + Forecast.FRESH_MS - 1)); assertFalse(f.fresh(1000 + Forecast.FRESH_MS)); assertFalse(f.fresh(0))
        assertEquals(5, f.age(1000 + 5 * 60_000))
        for (bad in listOf("", "{}", "{\"current\":{}}", "<html>")) assertTrue(runCatching { Forecast.parse(bad, 0) }.exceptionOrNull() is IllegalArgumentException)
    }

    @Test fun unitsAndTips() {
        assertEquals(14, Units.temp(14.2, TempUnit.C)); assertEquals(58, Units.temp(14.2, TempUnit.F)); assertEquals(-40, Units.temp(-40.0, TempUnit.F))
        assertEquals(10, Units.wind(36.0, WindUnit.MS)); assertEquals(22, Units.wind(36.0, WindUnit.MPH)); assertEquals(36, Units.wind(36.0, WindUnit.KMH))
        assertEquals(0, Units.point(350)); assertEquals(2, Units.point(90)); assertEquals(3, Units.point(120)); assertEquals(7, Units.point(-45))
        assertEquals(listOf(0, 1, 2, 3, 4), listOf(1.0, 4.0, 7.0, 9.0, 12.0).map(Units::uvLevel))
        val f = Forecast.parse(Fixtures.forecast, 0)
        assertEquals(listOf(Tip.UMBRELLA, Tip.SUNSCREEN), Advice.tips(f))                    // 72% rain within 12 h, UV 9.5 by day
        val night = f.copy(now = f.now.copy(day = false, feels = 5.0, wind = 45.0), hours = f.hours.map { it.copy(rain = 0, sky = Sky.CLEAR) })
        assertEquals(listOf(Tip.COLD, Tip.WIND), Advice.tips(night))
        val storm = f.copy(hours = f.hours.mapIndexed { i, h -> if (i == 12) h.copy(sky = Sky.STORM) else h })
        assertEquals(Tip.STORM, Advice.tips(storm).first())
    }

    @Test fun onlyTheTwoFixedAddresses() {
        val url = Api.forecast(-0.22991, -78.52495)
        assertTrue(url.startsWith("https://api.open-meteo.com/v1/forecast?latitude=-0.2299&longitude=-78.5250&"))
        assertTrue(runCatching { Api.forecast(91.0, 0.0) }.isFailure); assertTrue(runCatching { Api.forecast(Double.NaN, 0.0) }.isFailure)
        assertEquals("https://geocoding-api.open-meteo.com/v1/search?name=San+Jos%C3%A9+%26+x%3D1&count=10&language=es&format=json", Api.search(" San José & x=1 ", "es"))
        assertTrue(Api.search("Quito", "../evil").endsWith("language=es&format=json"))
        assertTrue(runCatching { Api.search("Q", "es") }.isFailure)
        val found = City.parseSearch(Fixtures.search)
        assertEquals(listOf("Azuay, Ecuador", "Castilla-La Mancha, España", ""), found.map { it.region })
        assertTrue(City.parseSearch("{}").isEmpty()); assertTrue(City.parseSearch("nope").isEmpty())
    }

    @Test fun placesAndTheSavedForecast() = runBlocking {
        val kv = MapValues()
        val places = Places(kv)
        assertTrue(places.add(quito)); assertTrue(places.add(quito)); assertEquals(1, places.list().size)
        repeat(Places.MAX - 1) { places.add(City(100L + it, "C$it", 0.0, 0.0)) }
        assertFalse(places.add(City(999, "Una más", 0.0, 0.0)))
        places.move(quito.id, 3); assertEquals(3, places.list().indexOf(quito))
        assertEquals(quito, Places.find(places.list(), "quíto")); assertEquals(quito, Places.find(places.list(), "3652462")); assertNull(Places.find(places.list(), "Lima"))
        val here = City(City.HERE, "Mi ubicación", 1.0, 2.0)
        assertEquals(here, Places.shown(places.list(), here).first())

        var t = 1_000_000L
        val net = Fixtures.Internet()
        val service = WeatherService(places, net) { t }
        assertFalse(service.forecast(quito).failed); assertEquals(1, net.asked.size)
        t += 60_000; service.forecast(quito); assertEquals(1, net.asked.size)                 // fresh: not asked again
        t += Forecast.FRESH_MS; net.online = false
        val out = service.forecast(quito)
        assertTrue(out.failed); assertEquals(14.2, out.forecast!!.now.temp, 0.0)              // offline: the saved one
        assertTrue(places.remove(quito.id)); assertNull(places.cached(quito.id)); assertFalse(places.remove(quito.id))
        assertTrue(net.asked.all { u -> Api.hosts.any { u.startsWith("https://$it/") } })
    }

    @Test fun summaryAndState() {
        val f = Forecast.parse(Fixtures.forecast, 0)
        val lines = SummaryLines.lines(quito, f, 60_000, TempUnit.C, { it.key }, { a, b -> "$a/$b" }, { it.key })
        assertEquals(listOf("Quito · 14° partly_cloudy", "19/9", "umbrella", "sunscreen"), lines)
        assertTrue(SummaryLines.lines(quito, f, SummaryLines.STALE_MS + 1, TempUnit.C, { "" }, { _, _ -> "" }, { "" }).isEmpty())
        assertTrue(SummaryLines.lines(null, null, 0, TempUnit.C, { "" }, { _, _ -> "" }, { "" }).isEmpty())

        val kv = MapValues()
        val net = Fixtures.Internet()
        val state = ClimaState(kv, net, launch = { runBlocking { it() } }, now = { 1_000L })
        assertNull(state.current)
        state.query = "Cuenca"; state.search()
        assertEquals(3, state.results.size)
        assertTrue(state.add(state.results.first()))
        assertEquals("Cuenca", state.current!!.name); assertEquals(Page.MAIN, state.page); assertEquals(14.2, state.forecast!!.now.temp, 0.0)
        state.query = "Cuenca"; state.search(); assertEquals(2, state.results.size)            // the saved one is not offered again
        net.online = false; state.query = "Quito"; state.search(); assertTrue(state.searchFailed)
        state.located(-0.2, -78.5, "Mi ubicación")
        assertEquals(1, state.shown.size)                                                       // «Mi ubicación» is off
        state.changeHere(true); assertEquals(City.HERE, state.current!!.id)
        state.changeTemp(TempUnit.F)
        val again = ClimaState(kv, net, launch = { runBlocking { it() } }, now = { 1_000L })
        assertEquals(2, again.shown.size); assertEquals(TempUnit.F, again.temp); assertEquals(14.2, again.forecasts[state.shown[1].id]!!.now.temp, 0.0)
        again.remove(again.shown[0]); assertFalse(again.hereOn)
    }
}
