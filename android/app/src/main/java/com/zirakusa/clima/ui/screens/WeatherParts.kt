package com.zirakusa.clima.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.clima.R
import com.zirakusa.clima.data.Advice
import com.zirakusa.clima.data.Forecast
import com.zirakusa.clima.data.Units
import com.zirakusa.clima.ui.ClimaState
import java.time.LocalDateTime
import java.time.format.TextStyle

private val Ink = Color.White
private val Soft = Color(0xCCFFFFFF)
private val Glass = Color(0x26FFFFFF)

/** A round see-through button over the sky. */
@Composable fun GlassButton(icon: ImageVector, label: String, onClick: () -> Unit) =
    Box(Modifier.padding(start = 8.dp).size(44.dp).clip(CircleShape).background(Glass).clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center) { Icon(icon, label, tint = Ink) }

@Composable private fun Pane(title: String? = null, content: @Composable ColumnScope.() -> Unit) =
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Glass).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (title != null) Text(title, color = Soft, fontSize = 14.sp)
        content()
    }

/** «14°», the sky, «Máx 19° · Mín 9°» and «Sensación 13°». The number counts up. */
@Composable fun Now(f: Forecast, state: ClimaState) = Column(Modifier.padding(top = 8.dp)) {
    val u = state.temp
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("${countUp(Units.temp(f.now.temp, u))}°", color = Ink, fontSize = 104.sp, fontWeight = FontWeight.Light)
        Icon(skyIcon(f.now.sky, f.now.day), null, Modifier.padding(start = 12.dp).size(64.dp), tint = Ink)
    }
    Text(stringResource(skyName(f.now.sky)), color = Ink, fontSize = 22.sp)
    f.today?.let { Text(stringResource(R.string.max_min, Units.temp(it.max, u), Units.temp(it.min, u)), color = Soft, fontSize = 17.sp) }
    Text(stringResource(R.string.feels, Units.temp(f.now.feels, u)), color = Soft, fontSize = 17.sp)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun Tips(f: Forecast) {
    val tips = Advice.tips(f)
    if (tips.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tips.forEach { Text(stringResource(tipText(it)), Modifier.clip(CircleShape).background(Glass).padding(horizontal = 14.dp, vertical = 8.dp), color = Ink, fontSize = 15.sp) }
    }
}

private fun clock(t: LocalDateTime, h24: Boolean) = if (h24) "%02d:%02d".format(t.hour, t.minute) else "%d %s".format((t.hour % 12).let { if (it == 0) 12 else it }, if (t.hour < 12) "a. m." else "p. m.")

/** The next 24 hours, side by side. */
@Composable fun Hourly(f: Forecast, state: ClimaState) = Pane(stringResource(R.string.hourly)) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(f.nextHours(24), key = { it.time.toString() }) { h ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (h.time.hour == f.now.time.hour && h.time.toLocalDate() == f.now.time.toLocalDate()) stringResource(R.string.now) else clock(h.time, state.h24),
                    color = Soft, fontSize = 13.sp)
                Icon(skyIcon(h.sky, h.day), null, Modifier.size(26.dp), tint = Ink)
                Text("${Units.temp(h.temp, state.temp)}°", color = Ink, fontSize = 17.sp)
                Text(if (h.rain >= 20) "${h.rain}%" else " ", color = Color(0xFFB3E5FC), fontSize = 12.sp)
            }
        }
    }
}

/** 7 days: name, sky, rain, min, a bar of the day's range within the week, max. */
@Composable fun Daily(f: Forecast, state: ClimaState) = Pane(stringResource(R.string.daily)) {
    val locale = LocalConfiguration.current.locales[0]
    val low = f.days.minOf { it.min }; val high = f.days.maxOf { it.max }
    val span = (high - low).coerceAtLeast(1.0)
    f.days.forEachIndexed { i, d ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (i == 0) stringResource(R.string.today) else d.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).replace(".", "")
                .replaceFirstChar { it.titlecase(locale) }, Modifier.width(52.dp), color = Ink, fontSize = 16.sp)
            Icon(skyIcon(d.sky), null, Modifier.size(24.dp), tint = Ink)
            Text(if (d.rain >= 20) "${d.rain}%" else "", Modifier.width(44.dp).padding(start = 6.dp), color = Color(0xFFB3E5FC), fontSize = 13.sp)
            Text("${Units.temp(d.min, state.temp)}°", Modifier.width(40.dp), color = Soft, fontSize = 16.sp)
            Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(Color(0x33FFFFFF))) {
                val start = ((d.min - low) / span).toFloat(); val end = ((d.max - low) / span).toFloat()
                Row(Modifier.fillMaxWidth()) {
                    if (start > 0f) Box(Modifier.weight(start).height(6.dp))
                    Box(Modifier.weight((end - start).coerceAtLeast(0.04f)).height(6.dp).clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(Color(0xFF8FD3FE), Color(0xFFFFC46B)))))
                    if (end < 1f) Box(Modifier.weight(1f - end).height(6.dp))
                }
            }
            Text("${Units.temp(d.max, state.temp)}°", Modifier.width(44.dp).padding(start = 8.dp), color = Ink, fontSize = 16.sp)
        }
    }
}

/** Humidity, wind, UV, pressure, sunrise and sunset. */
@OptIn(ExperimentalLayoutApi::class)
@Composable fun Details(f: Forecast, state: ClimaState) {
    val points = stringArrayResource(R.array.points)
    val uv = stringArrayResource(R.array.uv_levels)
    val today = f.today
    val items = listOfNotNull(
        stringResource(R.string.humidity) to "${f.now.humidity}%",
        stringResource(R.string.wind) to "${Units.wind(f.now.wind, state.wind)} ${state.wind.symbol} ${points[Units.point(f.now.windFrom)]}",
        stringResource(R.string.uv) to "${f.now.uv.toInt()} · ${uv[Units.uvLevel(f.now.uv)]}",
        stringResource(R.string.pressure) to "${f.now.pressure.toInt()} hPa",
        today?.sunrise?.let { stringResource(R.string.sunrise) to clock(it, state.h24) },
        today?.sunset?.let { stringResource(R.string.sunset) to clock(it, state.h24) },
    )
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), maxItemsInEachRow = 2) {
        items.forEach { (label, value) ->
            Column(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(Glass).padding(16.dp)) {
                Text(label, color = Soft, fontSize = 13.sp)
                Text(value, color = Ink, fontSize = 20.sp)
            }
        }
    }
}
