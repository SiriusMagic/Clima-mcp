package com.zirakusa.clima

import com.zirakusa.clima.data.Api
import com.zirakusa.clima.data.Fetch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Clima's only way to the internet for the weather: https to Open‑Meteo's two hosts ([Api.hosts]), 15 s at most,
 * answers up to 2 MiB, no cookies. Any other address is refused here, whoever asked.
 */
object Net : Fetch {
    private const val MAX = 2L shl 20
    private val http = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS).followRedirects(false).build()

    override suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val parsed = runCatching { url.toHttpUrl() }.getOrNull() ?: throw IOException("Bad address")
        if (!parsed.isHttps || parsed.host !in Api.hosts) throw IOException("Not an Open-Meteo address")
        http.newCall(Request.Builder().url(parsed).header("Accept", "application/json").build()).execute().use { r ->
            if (!r.isSuccessful) throw IOException("HTTP ${r.code}")
            val body = r.body
            if (body.contentLength() > MAX) throw IOException("Too large")
            val source = body.source()
            if (source.request(MAX + 1)) throw IOException("Too large") // reads it all, up to the limit
            source.buffer.readUtf8()
        }
    }
}
