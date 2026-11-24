package com.zirakusa.clima

import com.zirakusa.clima.data.City
import com.zirakusa.clima.data.MapValues
import com.zirakusa.clima.data.Places
import com.zirakusa.clima.data.WeatherService
import com.zirakusa.clima.mcp.Access
import com.zirakusa.clima.mcp.HubLink
import com.zirakusa.clima.mcp.Kind
import com.zirakusa.clima.mcp.McpHandler
import com.zirakusa.clima.mcp.Token
import com.zirakusa.clima.mcp.WeatherData
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Clima's MCP: tools list, good calls, bad arguments, not found, offline, read-only tokens, wrong and paused tokens, hub link. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class McpTest {
    private val places = Places(MapValues())
    private val net = Fixtures.Internet()
    private val service = WeatherService(places, net) { 1_000L }
    private val data = object : WeatherData {
        override fun places() = places.list()
        override fun selected() = places.list().firstOrNull()
        override suspend fun forecast(city: City) = service.forecast(city)
        override suspend fun search(words: String) = service.search(words, "es")
        override fun add(city: City) = places.add(city)
        override fun remove(id: Long) = places.remove(id)
    }
    private val writer = Token("w", "Claude", "cli_" + "w".repeat(40), Access.WRITE, Kind.HUB, "Clima")
    private val reader = Token("r", "Lector", "cli_" + "r".repeat(40), Access.READ)
    private val paused = Token("p", "Viejo", "cli_" + "p".repeat(40), Access.WRITE, paused = true)
    private val handler = McpHandler({ listOf(writer, reader, paused) }, data, "0.1.0")

    private fun rpc(token: Token, method: String, params: JSONObject = JSONObject()) = runBlocking {
        JSONObject(handler.handle("POST", "/mcp", mapOf("Authorization" to "Bearer ${token.secret}"), emptyMap(),
            JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", method).put("params", params).toString()).body).getJSONObject("result")
    }
    private fun tool(name: String, args: JSONObject, token: Token = writer) = rpc(token, "tools/call", JSONObject().put("name", name).put("arguments", args))
    private fun ok(name: String, args: JSONObject = JSONObject()) =
        tool(name, args).also { assertFalse(it.toString(), it.optBoolean("isError")) }.getJSONObject("structuredContent")
    private fun code(name: String, args: JSONObject = JSONObject(), token: Token = writer) = tool(name, args, token).also { assertTrue(it.optBoolean("isError")) }
        .getJSONObject("structuredContent").getJSONObject("error").getString("code")
    private fun names(token: Token) = rpc(token, "tools/list").getJSONArray("tools").let { t -> (0 until t.length()).map { t.getJSONObject(it).getString("name") } }
    private fun addQuito() = ok("weather_place_add", JSONObject().put("id", 3652462).put("name", "Quito").put("latitude", -0.2299).put("longitude", -78.5249).put("country", "Ecuador"))

    @Test fun listsTheTools() {
        assertEquals(listOf("weather_places", "weather_now", "weather_forecast", "weather_search", "weather_place_add", "weather_place_remove"), names(writer))
        assertEquals(listOf("weather_places", "weather_now", "weather_forecast", "weather_search"), names(reader))
        val init = rpc(writer, "initialize")
        assertEquals("clima", init.getJSONObject("serverInfo").getString("name"))
        assertEquals("hub", init.getJSONObject("_meta").getJSONObject(McpHandler.TOKEN_META).getString("kind"))
    }

    @Test fun searchAddReadAndRemove() {
        assertEquals("NO_PLACES", code("weather_now"))
        val found = ok("weather_search", JSONObject().put("query", "Cuenca")).getJSONArray("places")
        assertEquals("Azuay", found.getJSONObject(0).getString("admin"))
        addQuito()
        assertEquals("Quito", ok("weather_places").getJSONArray("places").getJSONObject(0).getString("name"))
        val now = ok("weather_now")
        assertEquals(14.2, now.getDouble("temp_c"), 0.0); assertEquals("partly_cloudy", now.getString("sky"))
        assertEquals(19.4, now.getJSONObject("today").getDouble("max_c"), 0.0); assertEquals("umbrella", now.getJSONArray("tips").getString(0))
        assertEquals("Quito", ok("weather_now", JSONObject().put("place", "quito")).getJSONObject("place").getString("name"))
        val f = ok("weather_forecast", JSONObject().put("place", 3652462).put("days", 3).put("hours", 2))
        assertEquals(3, f.getJSONArray("days").length()); assertEquals(2, f.getJSONArray("hours").length())
        assertEquals("2026-11-22", f.getJSONArray("days").getJSONObject(1).getString("date"))
        assertEquals(3652462L, ok("weather_place_remove", JSONObject().put("id", 3652462)).getLong("removed"))
        assertTrue(places.list().isEmpty())
        assertTrue(net.asked.all { it.startsWith("https://api.open-meteo.com/") || it.startsWith("https://geocoding-api.open-meteo.com/") })
    }

    @Test fun badArgumentsAreRefused() {
        addQuito()
        assertEquals("BAD_ARGUMENT", code("weather_forecast", JSONObject().put("days", 9)))
        assertEquals("BAD_ARGUMENT", code("weather_forecast", JSONObject().put("hours", "12")))
        assertEquals("BAD_ARGUMENT", code("weather_now", JSONObject().put("url", "https://evil.example")))
        assertEquals("BAD_ARGUMENT", code("weather_search", JSONObject().put("query", "Q")))
        assertEquals("BAD_ARGUMENT", code("weather_place_add", JSONObject().put("id", 1).put("name", "X").put("latitude", 95).put("longitude", 0)))
        assertEquals("BAD_ARGUMENT", code("weather_place_add", JSONObject().put("id", 1).put("name", " ").put("latitude", 0).put("longitude", 0)))
        assertEquals("BAD_ARGUMENT", code("weather_place_add", JSONObject().put("id", 1).put("name", "X").put("latitude", "0").put("longitude", 0)))
        assertEquals("BAD_ARGUMENT", code("weather_place_remove", JSONObject().put("id", City.HERE)))
        assertEquals(1, places.list().size)
    }

    @Test fun notFoundOfflineFullAndReadOnly() {
        addQuito()
        assertEquals("NOT_FOUND", code("weather_now", JSONObject().put("place", "Lima")))
        assertEquals("NOT_FOUND", code("weather_place_remove", JSONObject().put("id", 99)))
        assertEquals("UNKNOWN_TOOL", code("weather_fetch_url"))
        assertEquals("READ_ONLY", code("weather_place_remove", JSONObject().put("id", 3652462), reader))
        assertEquals("READ_ONLY", code("weather_place_add", JSONObject().put("id", 1).put("name", "X").put("latitude", 0).put("longitude", 0), reader))
        ok("weather_now")                                                                      // Quito's forecast is saved
        net.online = false
        assertEquals("OFFLINE", code("weather_search", JSONObject().put("query", "Lima")))
        ok("weather_place_add", JSONObject().put("id", 7).put("name", "Loja").put("latitude", -4.0).put("longitude", -79.2))
        assertEquals("OFFLINE", code("weather_now", JSONObject().put("place", "Loja")))
        assertEquals(14.2, ok("weather_now", JSONObject().put("place", "Quito")).getDouble("temp_c"), 0.0) // the saved forecast
        repeat(Places.MAX) { places.add(City(100L + it, "C$it", 0.0, 0.0)) }
        assertEquals("FULL", code("weather_place_add", JSONObject().put("id", 5).put("name", "Ambato").put("latitude", -1.2).put("longitude", -78.6)))
    }

    @Test fun wrongAndPausedTokensAndHubLink() = runBlocking {
        val body = """{"jsonrpc":"2.0","id":1,"method":"tools/list"}"""
        assertEquals(401, handler.handle("POST", "/mcp", mapOf("Authorization" to "Bearer ${paused.secret}"), emptyMap(), body).status)
        assertEquals(401, handler.handle("POST", "/mcp", mapOf("Authorization" to "Bearer cli_wrong"), emptyMap(), body).status)
        assertEquals(401, handler.handle("POST", "/mcp", emptyMap(), emptyMap(), body).status)
        assertEquals(200, handler.handle("POST", "/mcp", emptyMap(), mapOf("token" to reader.secret), body).status)
        assertEquals(404, handler.handle("POST", "/other", emptyMap(), emptyMap(), body).status)
        assertTrue(Token.secret().startsWith("cli_"))
        assertEquals("NOT_SAME_DEVELOPER", HubLink.answer(false).getString("error"))
        assertEquals("REDIRECT_OFF", HubLink.answer(true).getString("error"))
        // «Conectar todas» and Lucía's tools: only for the same developer, and tools only with «Redireccionar a Hub» on.
        assertEquals("NOT_SAME_DEVELOPER", HubLink.connect(false).getString("error"))
        assertEquals("NOT_SAME_DEVELOPER", HubLink.tool(false, "{}").getString("error"))
        assertEquals("REDIRECT_OFF", HubLink.tool(true, "{}").getString("error"))
    }

    /** Lucía runs a tool on the phone through the same handler, only while «Redireccionar a Hub» is on. */
    @Test fun luciaRunsToolsOnThePhone() {
        val relay = com.zirakusa.clima.mcp.McpMode.relayUrl
        com.zirakusa.clima.mcp.McpMode.relayUrl = ""                                             // no relay in tests
        com.zirakusa.clima.mcp.McpMode.load(org.robolectric.RuntimeEnvironment.getApplication())
        com.zirakusa.clima.mcp.McpMode.redirect(false)
        assertEquals(null, com.zirakusa.clima.mcp.McpMode.local("{}"))
        HubLink.connect(true); assertTrue(com.zirakusa.clima.mcp.McpMode.hubRedirect)                // «Conectar todas»
        val list = JSONObject(com.zirakusa.clima.mcp.McpMode.local("""{"jsonrpc":"2.0","id":1,"method":"tools/list"}""")!!)
        assertTrue(list.getJSONObject("result").getJSONArray("tools").length() > 0)
        val viaProvider = JSONObject(HubLink.tool(true, """{"jsonrpc":"2.0","id":2,"method":"tools/list"}""").getString("response"))
        assertEquals(2, viaProvider.getInt("id"))
        com.zirakusa.clima.mcp.McpMode.redirect(false)
        com.zirakusa.clima.mcp.McpMode.relayUrl = relay                                          // as the other tests expect
    }
}
