package com.zirakusa.clima

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat

/**
 * «Mi ubicación»: the phone's approximate position (network or the last one known), with Android's own location, no
 * Google services. Only after the user allows it; only the coordinates go to Open‑Meteo.
 */
object Here {
    @SuppressLint("MissingPermission") // checked by the caller (MainActivity asks first)
    fun find(context: Context, done: (Location?) -> Unit) {
        val lm = context.getSystemService(LocationManager::class.java) ?: return done(null)
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER)
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        val last = providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }
        val provider = providers.firstOrNull()
        if (Build.VERSION.SDK_INT >= 30 && provider != null) {
            runCatching { lm.getCurrentLocation(provider, CancellationSignal(), ContextCompat.getMainExecutor(context)) { done(it ?: last) } }
                .onFailure { done(last) }
        } else done(last)
    }
}
