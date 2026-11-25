package com.zirakusa.clima

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The app's launcher icons: six skies, «Nubes» by default (since 0.2.0). Each is an `activity-alias` in the manifest;
 * exactly one is enabled. Alias names never change (Android keeps the enabled state by name), so the old colour aliases
 * now carry the new pictures and «Luna» has a new one.
 */
enum class AppIcon(val alias: String, val label: Int, val preview: Int, val background: Long) {
    MOON("HomeLuna", R.string.icon_moon, R.mipmap.ic_launcher_moon_foreground, 0xFF023AA8),
    CLOUDS("Home", R.string.icon_clouds, R.mipmap.ic_launcher_clouds_foreground, 0xFF14AAFA),
    STORM("HomeVerde", R.string.icon_storm, R.mipmap.ic_launcher_storm_foreground, 0xFF065EBF),
    RAIN("HomeAzul", R.string.icon_rain, R.mipmap.ic_launcher_rain_foreground, 0xFF08A0F6),
    SUN("HomeClaro", R.string.icon_sun, R.mipmap.ic_launcher_sun_foreground, 0xFF0CB2FC),
    SUNSET("HomeDorado", R.string.icon_sunset, R.mipmap.ic_launcher_sunset_foreground, 0xFF3665C9);

    private fun component(context: Context) = ComponentName(context.packageName, "com.zirakusa.clima.$alias")
    fun isEnabled(context: Context): Boolean = when (context.packageManager.getComponentEnabledSetting(component(context))) {
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> this == CLOUDS           // as the manifest says
        else -> false
    }

    companion object {
        var current by mutableStateOf(CLOUDS); private set

        /** Reads the enabled one; if none is (an odd update), «Nubes» comes back so the app never loses its icon. */
        fun load(context: Context) { val on = entries.firstOrNull { it.isEnabled(context) }; if (on == null) apply(context, CLOUDS) else current = on }

        /** Enables [icon] first and then disables the others, without closing the app. Launchers refresh on their own. */
        fun apply(context: Context, icon: AppIcon) {
            val pm = context.packageManager
            pm.setComponentEnabledSetting(icon.component(context), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            entries.filter { it != icon }.forEach {
                pm.setComponentEnabledSetting(it.component(context), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            }
            current = icon
        }
    }
}

/** «Icono de la app»: the six icons in two rows; a tap changes the home-screen icon. */
@Composable fun IconCard(card: Color, text: Color, muted: Color, accent: Color) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(card).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.icon_title), color = text, fontSize = 18.sp)
        AppIcon.entries.chunked(3).forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            row.forEach { icon ->
                val on = icon == AppIcon.current
                val name = stringResource(icon.label)
                Column(Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClickLabel = name) { AppIcon.apply(context, icon) }.padding(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Color(icon.background))
                        .border(if (on) 3.dp else 0.dp, if (on) accent else Color.Transparent, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                        Image(painterResource(icon.preview), null, Modifier.requiredSize(72.dp))   // the 108dp layer, cropped like a launcher
                    }
                    Text(name, color = if (on) accent else muted, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1)
                }
            }
        } }
        Text(stringResource(R.string.icon_hint), color = muted, fontSize = 13.sp)
    }
}
