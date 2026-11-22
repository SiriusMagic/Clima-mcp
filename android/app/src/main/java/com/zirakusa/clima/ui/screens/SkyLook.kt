package com.zirakusa.clima.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.FilterDrama
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Dehaze
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.zirakusa.clima.R
import com.zirakusa.clima.data.Sky
import com.zirakusa.clima.data.Tip
import kotlin.math.roundToInt

/** The icon of a sky (moon instead of sun at night). */
fun skyIcon(sky: Sky, day: Boolean = true): ImageVector = when (sky) {
    Sky.CLEAR, Sky.MOSTLY_CLEAR -> if (day) Icons.Outlined.WbSunny else Icons.Outlined.Bedtime
    Sky.PARTLY_CLOUDY -> Icons.Outlined.FilterDrama
    Sky.CLOUDY -> Icons.Outlined.Cloud
    Sky.FOG -> Icons.Outlined.Dehaze
    Sky.DRIZZLE -> Icons.Outlined.Grain
    Sky.RAIN, Sky.SHOWERS -> Icons.Outlined.Umbrella
    Sky.SNOW -> Icons.Outlined.AcUnit
    Sky.STORM -> Icons.Outlined.Thunderstorm
}

fun skyName(sky: Sky) = when (sky) {
    Sky.CLEAR -> R.string.sky_clear; Sky.MOSTLY_CLEAR -> R.string.sky_mostly_clear; Sky.PARTLY_CLOUDY -> R.string.sky_partly
    Sky.CLOUDY -> R.string.sky_cloudy; Sky.FOG -> R.string.sky_fog; Sky.DRIZZLE -> R.string.sky_drizzle; Sky.RAIN -> R.string.sky_rain
    Sky.SHOWERS -> R.string.sky_showers; Sky.SNOW -> R.string.sky_snow; Sky.STORM -> R.string.sky_storm
}

fun tipText(t: Tip) = when (t) {
    Tip.UMBRELLA -> R.string.tip_umbrella; Tip.STORM -> R.string.tip_storm; Tip.SUNSCREEN -> R.string.tip_sunscreen
    Tip.COLD -> R.string.tip_cold; Tip.HOT -> R.string.tip_hot; Tip.WIND -> R.string.tip_wind
}

/** The background of the weather: a gradient that follows the sky and the night. Text on it is always white. */
fun skyBrush(sky: Sky?, day: Boolean): Brush {
    val (top, bottom) = when {
        sky == null -> 0xFF1C2433 to 0xFF0E131C
        !day && sky in setOf(Sky.CLEAR, Sky.MOSTLY_CLEAR, Sky.PARTLY_CLOUDY) -> 0xFF0B1026 to 0xFF2B3A67
        !day -> 0xFF161B26 to 0xFF343F52
        sky == Sky.CLEAR || sky == Sky.MOSTLY_CLEAR -> 0xFF2F80ED to 0xFF56B4F2
        sky == Sky.PARTLY_CLOUDY -> 0xFF3D78C9 to 0xFF8AAFD6
        sky == Sky.CLOUDY -> 0xFF4F5D70 to 0xFF8492A3
        sky == Sky.FOG -> 0xFF5D6779 to 0xFF97A1B3
        sky == Sky.SNOW -> 0xFF5F7C9C to 0xFF9FB6CE
        sky == Sky.STORM -> 0xFF1F2124 to 0xFF3E4249
        else -> 0xFF34465A to 0xFF5D7186
    }
    return Brush.verticalGradient(listOf(Color(top), Color(bottom)))
}

/** A number that counts up (or down) to [target] when it first shows or changes. */
@Composable fun countUp(target: Int, millis: Int = 900): Int {
    val value = remember { Animatable(0f) }
    LaunchedEffect(target) { value.animateTo(target.toFloat(), tween(millis)) }
    return value.value.roundToInt()
}

/** A grey block that softly pulses while the weather loads. */
@Composable fun Bone(width: Dp, height: Dp, modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "bone").animateFloat(0.25f, 0.55f,
        infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse), label = "pulse")
    Box(modifier.width(width).height(height).clip(RoundedCornerShape(14)).alpha(pulse).background(Color.White))
}
