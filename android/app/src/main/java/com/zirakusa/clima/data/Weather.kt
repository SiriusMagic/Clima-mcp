package com.zirakusa.clima.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime

/** The sky, from the WMO weather code Open‑Meteo gives. [key] is what MCP uses. */
enum class Sky(val key: String, vararg val codes: Int) {
    CLEAR("clear", 0), MOSTLY_CLEAR("mostly_clear", 1), PARTLY_CLOUDY("partly_cloudy", 2), CLOUDY("cloudy", 3),
    FOG("fog", 45, 48), DRIZZLE("drizzle", 51, 53, 55, 56, 57), RAIN("rain", 61, 63, 65, 66, 67), SHOWERS("showers", 80, 81, 82),
    SNOW("snow", 71, 73, 75, 77, 85, 86), STORM("storm", 95, 96, 99);
    val wet get() = this in setOf(DRIZZLE, RAIN, SHOWERS, SNOW, STORM)
    companion object { fun of(code: Int) = entries.firstOrNull { code in it.codes } ?: CLOUDY }
}

data class Now(val time: LocalDateTime, val temp: Double, val feels: Double, val humidity: Int, val sky: Sky, val day: Boolean,
    val precipitation: Double, val wind: Double, val windFrom: Int, val pressure: Double, val uv: Double)
data class Hour(val time: LocalDateTime, val temp: Double, val sky: Sky, val rain: Int, val day: Boolean)
data class Day(val date: LocalDate, val sky: Sky, val max: Double, val min: Double, val rain: Int, val sunrise: LocalDateTime?,
    val sunset: LocalDateTime?, val uv: Double)

/** One place's weather: now, by the hour and 7 days, in the place's own time ([zone]); [fetchedAt] says how fresh. */
data class Forecast(val now: Now, val hours: List<Hour>, val days: List<Day>, val zone: String, val fetchedAt: Long) {
    val today get() = days.firstOrNull { it.date == now.time.toLocalDate() } ?: days.firstOrNull()
    /** The next [count] hours from the current one. */
    fun nextHours(count: Int = 24) = hours.filter { !it.time.isBefore(now.time.withMinute(0)) }.take(count)
    fun fresh(at: Long) = at - fetchedAt in 0 until FRESH_MS
    fun age(at: Long) = ((at - fetchedAt) / 60_000).coerceAtLeast(0)

    companion object {
        /** A forecast younger than this is shown without asking again (Open‑Meteo updates about every 15 min to 1 h). */
        const val FRESH_MS = 30 * 60_000L

        /** Reads Open‑Meteo's answer. Broken or partial answers throw [IllegalArgumentException]. */
        fun parse(text: String, fetchedAt: Long): Forecast = try {
            val o = JSONObject(text)
            val c = o.getJSONObject("current")
            val now = Now(LocalDateTime.parse(c.getString("time")), c.getDouble("temperature_2m"), c.getDouble("apparent_temperature"),
                c.getInt("relative_humidity_2m"), Sky.of(c.getInt("weather_code")), c.optInt("is_day", 1) == 1, c.optDouble("precipitation", 0.0),
                c.optDouble("wind_speed_10m", 0.0), c.optInt("wind_direction_10m", 0), c.optDouble("pressure_msl", 0.0), c.optDouble("uv_index", 0.0))
            val h = o.getJSONObject("hourly")
            val ht = h.getJSONArray("time")
            val hours = (0 until ht.length()).map { i ->
                Hour(LocalDateTime.parse(ht.getString(i)), h.getJSONArray("temperature_2m").getDouble(i), Sky.of(h.getJSONArray("weather_code").getInt(i)),
                    h.optJSONArray("precipitation_probability").int(i), h.optJSONArray("is_day")?.optInt(i, 1) != 0)
            }
            val d = o.getJSONObject("daily")
            val dt = d.getJSONArray("time")
            val days = (0 until dt.length()).map { i ->
                Day(LocalDate.parse(dt.getString(i)), Sky.of(d.getJSONArray("weather_code").getInt(i)), d.getJSONArray("temperature_2m_max").getDouble(i),
                    d.getJSONArray("temperature_2m_min").getDouble(i), d.optJSONArray("precipitation_probability_max").int(i),
                    d.optJSONArray("sunrise").time(i), d.optJSONArray("sunset").time(i), d.optJSONArray("uv_index_max")?.optDouble(i, 0.0) ?: 0.0)
            }
            require(days.isNotEmpty() && hours.isNotEmpty())
            Forecast(now, hours, days, o.optString("timezone"), fetchedAt)
        } catch (e: Exception) { throw IllegalArgumentException("Not a forecast", e) }

        private fun JSONArray?.int(i: Int) = this?.optInt(i, 0) ?: 0
        private fun JSONArray?.time(i: Int) = this?.optString(i)?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
    }
}

/** A saved place. [id] is Open‑Meteo's; [HERE] is «Mi ubicación». */
data class City(val id: Long, val name: String, val lat: Double, val lon: Double, val admin: String = "", val country: String = "") {
    /** «Azuay, Ecuador». */
    val region get() = listOf(admin, country).filter { it.isNotBlank() && it != name }.joinToString(", ")
    fun json(): JSONObject = JSONObject().put("id", id).put("name", name).put("lat", lat).put("lon", lon).put("admin", admin).put("country", country)

    companion object {
        const val HERE = -1L
        fun from(o: JSONObject) = City(o.getLong("id"), o.getString("name"), o.getDouble("lat"), o.getDouble("lon"), o.optString("admin"), o.optString("country"))
        fun valid(lat: Double, lon: Double) = lat in -90.0..90.0 && lon in -180.0..180.0 && !lat.isNaN() && !lon.isNaN()

        /** Open‑Meteo's place search; places without coordinates are dropped. */
        fun parseSearch(text: String): List<City> = runCatching {
            val a = JSONObject(text).optJSONArray("results") ?: return emptyList()
            (0 until a.length()).map { a.getJSONObject(it) }.mapNotNull { r ->
                val lat = r.optDouble("latitude"); val lon = r.optDouble("longitude")
                if (!valid(lat, lon) || r.optString("name").isBlank()) null
                else City(r.getLong("id"), r.getString("name"), lat, lon, r.optString("admin1"), r.optString("country"))
            }
        }.getOrDefault(emptyList())
    }
}
