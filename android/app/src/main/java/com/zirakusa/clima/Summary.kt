package com.zirakusa.clima

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.zirakusa.clima.data.Advice
import com.zirakusa.clima.data.City
import com.zirakusa.clima.data.Forecast
import com.zirakusa.clima.data.Places
import com.zirakusa.clima.data.Settings
import com.zirakusa.clima.data.SharedValues
import com.zirakusa.clima.data.Sky
import com.zirakusa.clima.data.Tip
import com.zirakusa.clima.data.TempUnit
import com.zirakusa.clima.data.Units

/** The lines of «Hoy» for the family summary, from the saved forecast only (never the internet). Pure. */
object SummaryLines {
    /** Older than this, the saved forecast says nothing. */
    const val STALE_MS = 12 * 3_600_000L
    fun lines(city: City?, f: Forecast?, now: Long, unit: TempUnit, sky: (Sky) -> String, maxMin: (Int, Int) -> String, tip: (Tip) -> String): List<String> {
        if (city == null || f == null || now - f.fetchedAt !in 0..STALE_MS) return emptyList()
        return listOfNotNull("${city.name} · ${Units.temp(f.now.temp, unit)}° ${sky(f.now.sky)}",
            f.today?.let { maxMin(Units.temp(it.max, unit), Units.temp(it.min, unit)) }) + Advice.tips(f).take(2).map(tip)
    }
}

/**
 * The family summary (see Inicio's FAMILIA.md): what Inicio shows in its «Hoy» card for Clima. Only apps signed by the
 * same developer may ask (Android's signature permission and the caller's signature checked here).
 */
class SummaryProvider : ContentProvider() {
    override fun onCreate() = true
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method != "summary") return null
        val context = context ?: return null
        val caller = callingPackage ?: return null
        if (context.packageManager.checkSignatures(context.packageName, caller) != PackageManager.SIGNATURE_MATCH) return null
        val values = SharedValues(context)
        val settings = Settings(values)
        val places = Places(values)
        val shown = Places.shown(places.list(), places.here.takeIf { settings.here })
        val city = places.selected?.let { id -> shown.firstOrNull { it.id == id } } ?: shown.firstOrNull()
        val r = context.resources
        val lines = SummaryLines.lines(city, city?.let { places.cached(it.id) }, System.currentTimeMillis(), settings.temp,
            { r.getString(com.zirakusa.clima.ui.screens.skyName(it)) }, { max, min -> r.getString(R.string.max_min, max, min) },
            { r.getString(com.zirakusa.clima.ui.screens.tipText(it)) })
        return Bundle().apply {
            putString("title", context.getString(R.string.summary_title))
            putStringArrayList("lines", ArrayList(lines))
            putString("empty", context.getString(if (city == null) R.string.summary_no_place else R.string.summary_open))
        }
    }
    override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?) = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?) = 0
}
