package com.zirakusa.clima

import com.zirakusa.clima.data.Fetch
import java.io.IOException

/** Open-Meteo answers saved for the tests (Quito's forecast, the search «Cuenca») and a fake internet. */
object Fixtures {
    val forecast: String by lazy { text("forecast-quito.json") }
    val search: String by lazy { text("search-cuenca.json") }
    private fun text(name: String) = Fixtures::class.java.classLoader!!.getResource(name)!!.readText()

    /** Answers from the fixtures; [online] false throws as without internet. [asked] keeps every address. */
    class Internet(var online: Boolean = true) : Fetch {
        val asked = mutableListOf<String>()
        override suspend fun get(url: String): String {
            asked += url
            if (!online) throw IOException("offline")
            return if (url.contains("geocoding-api")) search else forecast
        }
    }
}
