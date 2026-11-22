package com.zirakusa.clima.data

import androidx.core.content.edit
import org.json.JSONArray
import java.text.Normalizer

/** Where the settings live: the phone's preferences in the app, a map in the tests. */
interface KeyValues {
    fun get(key: String): String?
    fun put(key: String, value: String?)
}

class MapValues(private val map: MutableMap<String, String> = mutableMapOf()) : KeyValues {
    override fun get(key: String) = map[key]
    override fun put(key: String, value: String?) { if (value == null) map.remove(key) else map[key] = value }
}

/** Every setting of Clima, typed. Unknown or broken values fall back to the defaults. */
class Settings(private val kv: KeyValues) {
    private fun flag(key: String, default: Boolean) = kv.get(key)?.let { it == "1" } ?: default
    private fun setFlag(key: String, on: Boolean) = kv.put(key, if (on) "1" else "0")

    var temp: TempUnit get() = kv.get("temp")?.let { runCatching { TempUnit.valueOf(it) }.getOrNull() } ?: TempUnit.C; set(v) = kv.put("temp", v.name)
    var wind: WindUnit get() = kv.get("wind")?.let { runCatching { WindUnit.valueOf(it) }.getOrNull() } ?: WindUnit.KMH; set(v) = kv.put("wind", v.name)
    /** «Mi ubicación» as the first place (needs the approximate location permission). */
    var here: Boolean get() = flag("here", false); set(v) = setFlag("here", v)
    var theme: String? get() = kv.get("theme"); set(v) = kv.put("theme", v)
    var accent: String? get() = kv.get("accent"); set(v) = kv.put("accent", v)
}

/**
 * The saved places, the one shown, and each place's last forecast (Open‑Meteo's own text and when it came), so
 * Clima opens at once and works offline. At most [MAX] places.
 */
class Places(private val kv: KeyValues) {
    fun list(): List<City> = runCatching {
        val a = JSONArray(kv.get(KEY) ?: "[]")
        (0 until a.length()).map { City.from(a.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    /** Adds a place (or keeps the one already there); false when full. */
    fun add(city: City): Boolean {
        val items = list()
        if (items.any { it.id == city.id }) return true
        if (items.size >= MAX) return false
        save(items + city); return true
    }

    fun remove(id: Long): Boolean {
        val items = list()
        if (items.none { it.id == id }) return false
        save(items.filterNot { it.id == id }); kv.put("fc_$id", null); kv.put("fa_$id", null)
        return true
    }

    fun move(id: Long, to: Int) {
        val items = list().toMutableList()
        val i = items.indexOfFirst { it.id == id }.takeIf { it >= 0 } ?: return
        val c = items.removeAt(i); items.add(to.coerceIn(0, items.size), c); save(items)
    }

    var selected: Long? get() = kv.get("selected")?.toLongOrNull(); set(v) = kv.put("selected", v?.toString())

    /** «Mi ubicación»: the last coordinates the phone gave. */
    var here: City? get() = kv.get("here_city")?.let { runCatching { City.from(org.json.JSONObject(it)) }.getOrNull() }
        set(v) = kv.put("here_city", v?.json()?.toString())

    fun cached(id: Long): Forecast? {
        val text = kv.get("fc_$id") ?: return null
        val at = kv.get("fa_$id")?.toLongOrNull() ?: return null
        return runCatching { Forecast.parse(text, at) }.getOrNull()
    }

    fun store(id: Long, text: String, at: Long) { kv.put("fc_$id", text); kv.put("fa_$id", at.toString()) }

    private fun save(items: List<City>) = kv.put(KEY, JSONArray(items.map { it.json() }).toString())

    companion object {
        const val MAX = 12
        private const val KEY = "cities"

        /** The places in the order shown: «Mi ubicación» first (when on and known), then the saved ones. */
        fun shown(saved: List<City>, here: City?) = listOfNotNull(here) + saved.filterNot { it.id == City.HERE }

        /** A saved place by its id or its name (accents and case ignored). */
        fun find(cities: List<City>, what: String): City? {
            what.trim().toLongOrNull()?.let { id -> cities.firstOrNull { it.id == id }?.let { return it } }
            val q = plain(what)
            return cities.firstOrNull { plain(it.name) == q } ?: cities.firstOrNull { plain(it.name).startsWith(q) }
        }

        fun plain(s: String) = Normalizer.normalize(s.trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
    }
}

/** The phone's preferences file «clima» (kept out of backups). The MCP tools and the family summary read the same. */
class SharedValues(context: android.content.Context) : KeyValues {
    private val prefs = context.applicationContext.getSharedPreferences("clima", android.content.Context.MODE_PRIVATE)
    override fun get(key: String): String? = prefs.getString(key, null)
    override fun put(key: String, value: String?) {
        prefs.edit(commit = true) { if (value == null) remove(key) else putString(key, value) }
    }
}
