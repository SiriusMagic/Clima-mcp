package com.zirakusa.clima.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.clima.R
import com.zirakusa.clima.data.City
import com.zirakusa.clima.ui.ClimaState
import com.zirakusa.clima.ui.Page

private val Ink = Color.White
private val Soft = Color(0xCCFFFFFF)

/**
 * The weather of each place, one page per place (swipe to change): place and time on top, the temperature counting up,
 * tips, by the hour, 7 days and the details. While it first loads, grey bones; offline, the saved one and a note.
 */
@Composable fun WeatherScreen(state: ClimaState, onLocation: () -> Unit) {
    val f = state.forecast
    Box(Modifier.fillMaxSize().background(skyBrush(f?.now?.sky, f?.now?.day ?: true))) {
        if (state.shown.isEmpty()) return@Box Welcome(state, onLocation)
        val pager = rememberPagerState(state.index) { state.shown.size }
        LaunchedEffect(pager.currentPage) { state.select(pager.currentPage) }
        LaunchedEffect(state.index) { if (pager.currentPage != state.index) pager.scrollToPage(state.index) }
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Top(state)
            HorizontalPager(pager, Modifier.weight(1f)) { page ->
                val city = state.shown[page]
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) { PlacePage(state, city) }
            }
        }
    }
}

@Composable private fun Top(state: ClimaState) = Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
    val city = state.current ?: return@Row
    Column(Modifier.weight(1f).padding(start = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (city.id == City.HERE) Icon(Icons.Outlined.MyLocation, stringResource(R.string.here), Modifier.size(18.dp).padding(end = 4.dp), tint = Ink)
            Text(city.name, color = Ink, fontSize = 24.sp, maxLines = 1)
        }
        if (state.shown.size > 1) Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            state.shown.indices.forEach { i -> Box(Modifier.size(7.dp).clip(CircleShape).background(if (i == state.index) Ink else Color(0x66FFFFFF))) }
        }
    }
    GlassButton(Icons.Outlined.Refresh, stringResource(R.string.refresh)) { state.refresh(force = true) }
    GlassButton(Icons.AutoMirrored.Outlined.FormatListBulleted, stringResource(R.string.places)) { state.page = Page.PLACES }
    GlassButton(Icons.Outlined.Settings, stringResource(R.string.settings)) { state.page = Page.SETTINGS }
}

@Composable private fun PlacePage(state: ClimaState, city: City) {
    val f = state.forecasts[city.id]
    if (f == null) {
        if (city.id in state.failed) Offline(state) else Skeleton()
        return
    }
    Now(f, state)
    val age = f.age(state.now()).toInt()
    val ago = when {
        age < 1 -> stringResource(R.string.just_now)
        age < 60 -> pluralStringResource(R.plurals.minutes, age, age)
        age < 48 * 60 -> pluralStringResource(R.plurals.hours, age / 60, age / 60)
        else -> pluralStringResource(R.plurals.days, age / 1440, age / 1440)
    }
    Text(when {
        city.id in state.loading -> stringResource(R.string.updating)
        city.id in state.failed -> stringResource(R.string.offline_saved, ago)
        else -> stringResource(R.string.updated, ago)
    }, color = Soft, fontSize = 13.sp)
    Tips(f)
    Hourly(f, state)
    Daily(f, state)
    Details(f, state)
    Text(stringResource(R.string.source), Modifier.fillMaxWidth(), color = Color(0x99FFFFFF), fontSize = 12.sp, textAlign = TextAlign.Center)
}

/** The bones of the weather while it first loads. */
@Composable private fun Skeleton() = Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 24.dp)) {
    Bone(150.dp, 96.dp); Bone(190.dp, 22.dp); Bone(120.dp, 18.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { repeat(5) { Bone(58.dp, 96.dp) } }
    repeat(4) { Bone(360.dp, 26.dp) }
}

@Composable private fun Offline(state: ClimaState) = Column(Modifier.fillMaxWidth().padding(top = 80.dp), horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Icon(Icons.Outlined.CloudOff, null, Modifier.size(56.dp), tint = Ink)
    Text(stringResource(R.string.offline), color = Ink, fontSize = 18.sp, textAlign = TextAlign.Center)
    MainButton(stringResource(R.string.retry)) { state.refresh(force = true) }
}

/** No places yet: add one, or use the phone's location. */
@Composable private fun Welcome(state: ClimaState, onLocation: () -> Unit) = Column(Modifier.fillMaxSize().statusBarsPadding().padding(28.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
    Icon(skyIcon(com.zirakusa.clima.data.Sky.PARTLY_CLOUDY), null, Modifier.size(88.dp), tint = Ink)
    Text(stringResource(R.string.welcome_title), color = Ink, fontSize = 26.sp, textAlign = TextAlign.Center)
    Text(stringResource(R.string.welcome_text), color = Soft, fontSize = 16.sp, textAlign = TextAlign.Center)
    MainButton(stringResource(R.string.add_place)) { state.page = Page.PLACES }
    Row(Modifier.clip(CircleShape).background(Color(0x33FFFFFF)).clickable(onClick = onLocation).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.MyLocation, null, tint = Ink)
        Text(stringResource(R.string.use_location), Modifier.padding(start = 8.dp), color = Ink, fontSize = 16.sp)
    }
}
