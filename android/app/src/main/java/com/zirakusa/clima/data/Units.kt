package com.zirakusa.clima.data

import java.net.URLEncoder
import java.util.Locale
import kotlin.math.roundToInt

/** Ajustes › Temperatura. */
enum class TempUnit(val symbol: String) { C("°C"), F("°F") }

/** Ajustes › Viento. */
enum class WindUnit(val symbol: String) { KMH("km/h"), MS("m/s"), MPH("mph") }

/** Turning Open‑Meteo's metric values into the user's units, rounded as shown. Pure. */
object Units {
    fun temp(celsius: Double, unit: TempUnit): Int = (if (unit == TempUnit.F) celsius * 9 / 5 + 32 else celsius).roundToInt()
    fun wind(kmh: Double, unit: WindUnit): Int = when (unit) { WindUnit.KMH -> kmh; WindUnit.MS -> kmh / 3.6; WindUnit.MPH -> kmh / 1.609344 }.roundToInt()
    /** Where the wind comes from, as one of 8 points: 0 N, 1 NE … 7 NO. */
    fun point(degrees: Int) = (((degrees % 360 + 360) % 360 + 22) / 45) % 8
    /** UV level: 0 low (0–2), 1 moderate, 2 high, 3 very high, 4 extreme (11+). */
    fun uvLevel(uv: Double) = when { uv < 3 -> 0; uv < 6 -> 1; uv < 8 -> 2; uv < 11 -> 3; else -> 4 }
}

/** Something worth saying about the next hours. [key] is what MCP uses. */
enum class Tip(val key: String) { UMBRELLA("umbrella"), STORM("storm"), SUNSCREEN("sunscreen"), COLD("cold"), HOT("hot"), WIND("wind") }

/** The tips for the next 12 hours: umbrella, storm, sunscreen, cold, heat and wind. Pure. */
object Advice {
    fun tips(f: Forecast): List<Tip> {
        val next = f.nextHours(12)
        return buildList {
            if (next.any { it.sky == Sky.STORM }) add(Tip.STORM)
            else if (next.any { it.rain >= 50 } || f.now.sky.wet) add(Tip.UMBRELLA)
            if ((f.today?.uv ?: f.now.uv) >= 6 && f.now.day) add(Tip.SUNSCREEN)
            if (f.now.feels <= 8) add(Tip.COLD)
            if (f.now.feels >= 30) add(Tip.HOT)
            if (f.now.wind >= 40) add(Tip.WIND)
        }
    }
}

/**
 * The only two addresses Clima asks, both Open‑Meteo (free, no account, no key). Only the place's coordinates or the
 * words typed go out, checked and encoded here: never an address chosen by the AI.
 */
object Api {
    const val FORECAST = "https://api.open-meteo.com/v1/forecast"
    const val SEARCH = "https://geocoding-api.open-meteo.com/v1/search"
    val hosts = setOf("api.open-meteo.com", "geocoding-api.open-meteo.com")

    fun forecast(lat: Double, lon: Double): String {
        require(City.valid(lat, lon)) { "Bad coordinates" }
        return "$FORECAST?latitude=${"%.4f".format(Locale.ROOT, lat)}&longitude=${"%.4f".format(Locale.ROOT, lon)}" +
            "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,is_day,precipitation,wind_speed_10m," +
            "wind_direction_10m,pressure_msl,uv_index&hourly=temperature_2m,weather_code,precipitation_probability,is_day" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset,uv_index_max" +
            "&timezone=auto&forecast_days=7"
    }

    fun search(words: String, language: String): String {
        val q = words.trim().take(SEARCH_MAX)
        require(q.length >= 2) { "Too short" }
        val lang = language.lowercase().takeIf { it.matches(Regex("[a-z]{2}")) } ?: "es"
        return "$SEARCH?name=${URLEncoder.encode(q, "UTF-8")}&count=10&language=$lang&format=json"
    }

    const val SEARCH_MAX = 60
}
