package com.zirakusa.clima.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.clima.R
import com.zirakusa.clima.data.City
import com.zirakusa.clima.data.Places
import com.zirakusa.clima.data.Units
import com.zirakusa.clima.ui.ClimaState
import com.zirakusa.clima.ui.Page
import com.zirakusa.clima.ui.theme.ClimaColors
import kotlinx.coroutines.delay

/** «Lugares»: search to add (Open‑Meteo), «Mi ubicación», and the saved places with their weather (tap: show it). */
@Composable fun PlacesScreen(state: ClimaState, toast: (String) -> Unit, onLocation: () -> Unit) {
    LaunchedEffect(state.query) { delay(400); state.search() }
    val full = stringResource(R.string.full, Places.MAX)
    Column(Modifier.fillMaxSize().background(ClimaColors.Background).systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PageTop(stringResource(R.string.places)) { state.page = Page.MAIN }
        OutlinedTextField(state.query, { state.query = it.take(60) }, Modifier.fillMaxWidth(), singleLine = true, shape = CircleShape,
            leadingIcon = { Icon(Icons.Outlined.Search, null) }, placeholder = { Text(stringResource(R.string.search_place)) },
            trailingIcon = { if (state.searching) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) })
        if (state.searchFailed) Text(stringResource(R.string.search_offline), Modifier.padding(8.dp), color = ClimaColors.Muted)
        if (state.query.trim().length >= 2 && !state.searching && !state.searchFailed && state.results.isEmpty())
            Text(stringResource(R.string.nothing_found), Modifier.padding(8.dp), color = ClimaColors.Muted)
        if (state.results.isNotEmpty()) Group {
            state.results.forEachIndexed { i, c ->
                if (i > 0) Divider()
                Row(Modifier.fillMaxWidth().clickable { if (!state.add(c)) toast(full) }.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(c.name, fontSize = 18.sp)
                        if (c.region.isNotBlank()) Text(c.region, color = ClimaColors.Muted, fontSize = 14.sp)
                    }
                    Icon(Icons.Outlined.Add, stringResource(R.string.add_place), tint = ClimaColors.AccentText)
                }
            }
        }
        Group { SwitchItem(stringResource(R.string.here), stringResource(R.string.here_hint), state.hereOn) { on -> if (on) onLocation() else state.changeHere(false) } }
        Caption(stringResource(R.string.saved_places))
        val saved = state.shown.filter { it.id != City.HERE }
        if (saved.isEmpty()) Text(stringResource(R.string.no_places), Modifier.padding(8.dp), color = ClimaColors.Muted)
        else Group {
            saved.forEachIndexed { i, c ->
                if (i > 0) Divider()
                PlaceRow(state, c, first = i == 0)
            }
        }
        Text("", Modifier.padding(bottom = 16.dp))
    }
}

@Composable private fun PlaceRow(state: ClimaState, c: City, first: Boolean) =
    Row(Modifier.fillMaxWidth().clickable { state.select(state.shown.indexOf(c)); state.page = Page.MAIN }.padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        val f = state.forecasts[c.id]
        if (c.id == City.HERE) Icon(Icons.Outlined.MyLocation, null, Modifier.padding(end = 8.dp).size(18.dp))
        Column(Modifier.weight(1f)) {
            Text(c.name, fontSize = 18.sp)
            Text(f?.let { stringResource(skyName(it.now.sky)) } ?: c.region, color = ClimaColors.Muted, fontSize = 14.sp)
        }
        f?.let { Text("${Units.temp(it.now.temp, state.temp)}°", Modifier.padding(horizontal = 8.dp), fontSize = 22.sp) }
        if (!first) RoundButton(Icons.Outlined.KeyboardArrowUp, stringResource(R.string.move_up), size = 40.dp) { state.move(c, -1) }
        RoundButton(Icons.Outlined.Close, stringResource(R.string.remove), Modifier.padding(start = 6.dp), size = 40.dp) { state.remove(c) }
    }
