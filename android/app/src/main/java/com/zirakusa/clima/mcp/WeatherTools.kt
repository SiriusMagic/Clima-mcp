package com.zirakusa.clima.mcp

import com.zirakusa.clima.data.Advice
import com.zirakusa.clima.data.Api
import com.zirakusa.clima.data.City
import com.zirakusa.clima.data.Forecast
import com.zirakusa.clima.data.Outcome
import com.zirakusa.clima.data.Places
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import kotlin.math.roundToInt

/** What the tools read and change; the app gives the real places and Open‑Meteo, tests a fake. */
interface WeatherData {
    /** The places in the order shown («Mi ubicación» first when it is on). */
    fun places(): List<City>
    fun selected(): City?
    suspend fun forecast(city: City): Outcome
    suspend fun search(words: String): List<City>
    /** false when the list is full. */
    fun add(city: City): Boolean
    fun remove(id: Long): Boolean
}

/** An expected failure the AI can read (structuredContent.error.code), never a crash. */
class ToolFailure(val code: String, message: String) : Exception(message)

/**
 * Clima's MCP tools: the weather now and the forecast of a saved place, the saved places and a place search; with a
 * read-and-write token also add and remove places. The AI only gives words or coordinates: Clima itself asks
 * Open‑Meteo, at its two fixed addresses.
 */
object WeatherTools {
    class Tool(val name: String, val description: String, val write: Boolean, val schema: JSONObject)

    private fun schema(required: List<String>, vararg props: Pair<String, JSONObject>) = JSONObject()
        .put("type", "object").put("properties", JSONObject().apply { props.forEach { (k, v) -> put(k, v) } })
        .put("required", JSONArray(required)).put("additionalProperties", false)
    private fun prop(type: String, description: String) = JSONObject().put("type", type).put("description", description)
    private val place = "place" to prop("string", "A saved place: its id or name (default: the one shown in Clima).")

    val tools = listOf(
        Tool("weather_places", "The places saved in Clima, in order, and which one is shown.", false, schema(emptyList())),
        Tool("weather_now", "The weather now at a saved place: temperature, feels like, sky, humidity, wind, UV, today's max/min, " +
            "rain chance and tips (umbrella, sunscreen…). Metric units.", false, schema(emptyList(), place)),
        Tool("weather_forecast", "The forecast of a saved place: by day (up to 7) and by hour (up to 48). Metric units, local time of the place.", false,
            schema(emptyList(), place, "days" to prop("integer", "1–7, default 7."), "hours" to prop("integer", "0–48, default 12."))),
        Tool("weather_search", "Looks for places by name (Open‑Meteo), to add one with weather_place_add.", false,
            schema(listOf("query"), "query" to prop("string", "2–${Api.SEARCH_MAX} characters, e.g. «Cuenca»."))),
        Tool("weather_place_add", "Saves a place in Clima (from weather_search).", true,
            schema(listOf("id", "name", "latitude", "longitude"), "id" to prop("integer", "From weather_search."),
                "name" to prop("string", "Up to 80 characters."), "latitude" to prop("number", "-90–90."), "longitude" to prop("number", "-180–180."),
                "admin" to prop("string", "Region."), "country" to prop("string", "Country."))),
        Tool("weather_place_remove", "Removes a saved place.", true, schema(listOf("id"), "id" to prop("integer", "From weather_places."))),
    )

    fun available(access: Access) = tools.filter { !it.write || access == Access.WRITE }

    suspend fun call(name: String, args: JSONObject, access: Access, data: WeatherData): JSONObject {
        val tool = tools.firstOrNull { it.name == name } ?: throw ToolFailure("UNKNOWN_TOOL", "No tool named $name.")
        if (tool.write && access != Access.WRITE) throw ToolFailure("READ_ONLY", "This token can only read. Ask for a read-and-write token.")
        val known = tool.schema.getJSONObject("properties").keys().asSequence().toSet()
        args.keys().forEach { if (it !in known) throw ToolFailure("BAD_ARGUMENT", "Unknown argument «$it».") }
        return when (name) {
            "weather_places" -> JSONObject().put("selected", data.selected()?.id ?: JSONObject.NULL)
                .put("places", JSONArray(data.places().map { place(it) }))
            "weather_now" -> pick(args, data).let { c -> now(c, get(c, data)) }
            "weather_forecast" -> forecast(args, data)
            "weather_search" -> search(args, data)
            "weather_place_add" -> add(args, data)
            "weather_place_remove" -> long(args, "id").let { id ->
                if (id == City.HERE) throw ToolFailure("BAD_ARGUMENT", "«Mi ubicación» is turned off in Clima › Ajustes, not removed.")
                if (!data.remove(id)) throw ToolFailure("NOT_FOUND", "No saved place with id $id.")
                JSONObject().put("removed", id)
            }
            else -> throw ToolFailure("UNKNOWN_TOOL", "No tool named $name.")
        }
    }

    private fun place(c: City) = JSONObject().put("id", c.id).put("name", c.name).put("region", c.region)
        .put("latitude", c.lat).put("longitude", c.lon).put("here", c.id == City.HERE)

    private fun pick(args: JSONObject, data: WeatherData): City {
        val places = data.places()
        if (places.isEmpty()) throw ToolFailure("NO_PLACES", "Clima has no places yet: add one (weather_search, then weather_place_add).")
        if (!args.has("place")) return data.selected() ?: places.first()
        val what = args.opt("place").let { it as? String ?: (it as? Number)?.toLong()?.toString() } ?: throw ToolFailure("BAD_ARGUMENT", "«place» is an id or a name.")
        return Places.find(places, what) ?: throw ToolFailure("NOT_FOUND", "No saved place «$what». See weather_places.")
    }

    private suspend fun get(c: City, data: WeatherData): Forecast {
        val out = data.forecast(c)
        return out.forecast ?: throw ToolFailure("OFFLINE", "The phone couldn't reach Open‑Meteo and has no saved forecast for ${c.name}.")
    }

    private fun r(v: Double) = (v * 10).roundToInt() / 10.0

    private fun now(c: City, f: Forecast): JSONObject {
        val n = f.now; val t = f.today
        return JSONObject().put("place", place(c)).put("time", n.time.toString()).put("updated_minutes_ago", f.age(System.currentTimeMillis()))
            .put("temp_c", r(n.temp)).put("feels_c", r(n.feels)).put("sky", n.sky.key).put("day", n.day).put("humidity", n.humidity)
            .put("wind_kmh", r(n.wind)).put("wind_from_deg", n.windFrom).put("pressure_hpa", r(n.pressure)).put("uv", r(n.uv))
            .put("precipitation_mm", r(n.precipitation))
            .put("today", t?.let { JSONObject().put("max_c", r(it.max)).put("min_c", r(it.min)).put("rain_chance", it.rain).put("uv_max", r(it.uv))
                .put("sunrise", it.sunrise?.toString() ?: JSONObject.NULL).put("sunset", it.sunset?.toString() ?: JSONObject.NULL) } ?: JSONObject.NULL)
            .put("tips", JSONArray(Advice.tips(f).map { it.key }))
    }

    private suspend fun forecast(args: JSONObject, data: WeatherData): JSONObject {
        val days = if (args.has("days")) int(args, "days").takeIf { it in 1..7 } ?: throw ToolFailure("BAD_ARGUMENT", "«days» is 1–7.") else 7
        val hours = if (args.has("hours")) int(args, "hours").takeIf { it in 0..48 } ?: throw ToolFailure("BAD_ARGUMENT", "«hours» is 0–48.") else 12
        val c = pick(args, data)
        val f = get(c, data)
        return JSONObject().put("place", place(c)).put("timezone", f.zone)
            .put("days", JSONArray(f.days.take(days).map {
                JSONObject().put("date", it.date.toString()).put("sky", it.sky.key).put("max_c", r(it.max)).put("min_c", r(it.min))
                    .put("rain_chance", it.rain).put("uv_max", r(it.uv))
            }))
            .put("hours", JSONArray(f.nextHours(hours).map {
                JSONObject().put("time", it.time.toString()).put("temp_c", r(it.temp)).put("sky", it.sky.key).put("rain_chance", it.rain)
            }))
    }

    private suspend fun search(args: JSONObject, data: WeatherData): JSONObject {
        val q = (args.opt("query") as? String)?.trim() ?: throw ToolFailure("BAD_ARGUMENT", "«query» is text.")
        if (q.length !in 2..Api.SEARCH_MAX) throw ToolFailure("BAD_ARGUMENT", "«query» has 2–${Api.SEARCH_MAX} characters.")
        val found = try { data.search(q) } catch (_: IOException) { throw ToolFailure("OFFLINE", "The phone couldn't reach Open‑Meteo.") }
        return JSONObject().put("places", JSONArray(found.map { place(it).put("admin", it.admin).put("country", it.country) }))
    }

    private fun add(args: JSONObject, data: WeatherData): JSONObject {
        val id = long(args, "id").takeIf { it > 0 } ?: throw ToolFailure("BAD_ARGUMENT", "«id» comes from weather_search.")
        val name = (args.opt("name") as? String)?.trim()?.takeIf { it.isNotEmpty() && it.length <= 80 } ?: throw ToolFailure("BAD_ARGUMENT", "«name» is 1–80 characters.")
        val lat = (args.opt("latitude") as? Number)?.toDouble() ?: throw ToolFailure("BAD_ARGUMENT", "«latitude» is a number.")
        val lon = (args.opt("longitude") as? Number)?.toDouble() ?: throw ToolFailure("BAD_ARGUMENT", "«longitude» is a number.")
        if (!City.valid(lat, lon)) throw ToolFailure("BAD_ARGUMENT", "«latitude» is -90–90 and «longitude» -180–180.")
        fun s(k: String) = (args.opt(k) as? String)?.trim()?.take(80).orEmpty()
        val city = City(id, name, lat, lon, s("admin"), s("country"))
        if (!data.add(city)) throw ToolFailure("FULL", "Clima keeps up to ${Places.MAX} places. Remove one first.")
        return JSONObject().put("added", place(city))
    }

    private fun int(args: JSONObject, key: String) = (args.opt(key) as? Int) ?: (args.opt(key) as? Long)?.toInt()
        ?: throw ToolFailure("BAD_ARGUMENT", "«$key» is a whole number.")
    private fun long(args: JSONObject, key: String) = (args.opt(key) as? Int)?.toLong() ?: (args.opt(key) as? Long)
        ?: throw ToolFailure("BAD_ARGUMENT", "«$key» is a whole number.")
}
