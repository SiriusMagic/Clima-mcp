package com.zirakusa.clima.data

import java.io.IOException

/** Asks one address and gives the text back; the app uses the internet ([com.zirakusa.clima.Net]), tests a fake. */
fun interface Fetch { suspend fun get(url: String): String }

/** A forecast and whether asking for a newer one failed (then it is the saved one, or null). */
data class Outcome(val forecast: Forecast?, val failed: Boolean = false)

/**
 * The weather of a place: the saved forecast while it is fresh, otherwise a new one from Open‑Meteo (saved for next
 * time). Without internet the saved one is still given, marked [Outcome.failed].
 */
class WeatherService(private val places: Places, private val fetch: Fetch, private val now: () -> Long = System::currentTimeMillis) {
    suspend fun forecast(city: City, force: Boolean = false): Outcome {
        val saved = places.cached(city.id)
        if (!force && saved != null && saved.fresh(now())) return Outcome(saved)
        return try {
            val at = now()
            val text = fetch.get(Api.forecast(city.lat, city.lon))
            val fresh = Forecast.parse(text, at)
            places.store(city.id, text, at)
            Outcome(fresh)
        } catch (_: IOException) { Outcome(saved, failed = true) }
        catch (_: IllegalArgumentException) { Outcome(saved, failed = true) }
    }

    /** Places named like [words]; throws [IOException] without internet. */
    suspend fun search(words: String, language: String): List<City> = City.parseSearch(fetch.get(Api.search(words, language)))
}
