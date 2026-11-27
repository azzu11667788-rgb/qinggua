package com.qinggua.app.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object C {
    val Keep = Color(0xFF1D9E75)
    val KeepBg = Color(0xFFE1F5EE)
    val Delete = Color(0xFFA32D2D)
    val DeleteBg = Color(0xFFFCEBEB)
    val Pending = Color(0xFFBA7517)
    val PendingBg = Color(0xFFFAEEDA)
    val Info = Color(0xFF185FA5)
    val InfoBg = Color(0xFFE6F1FB)
    val Ink = Color(0xFF1C1C1A)
    val Ink2 = Color(0xFF5F5E5A)
    val Ink3 = Color(0xFF9A9992)
    val Line = Color(0xFFE6E4DD)
    val Surface = Color(0xFFFFFFFF)
    val Page = Color(0xFFF7F6F2)
}

private val LightColors = lightColorScheme(
    primary = C.Keep,
    onPrimary = Color.White,
    primaryContainer = C.KeepBg,
    secondary = C.Pending,
    background = C.Page,
    onBackground = C.Ink,
    surface = C.Surface,
    onSurface = C.Ink,
    surfaceVariant = C.Page,
    onSurfaceVariant = C.Ink2,
    error = C.Delete,
    onError = Color.White,
    outline = C.Line
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5DCAA5),
    onPrimary = Color(0xFF04342C),
    background = Color(0xFF14150F),
    surface = Color(0xFF1E1F19),
    error = Color(0xFFF09595)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

private val AppTypography = Typography(
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 15.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun QingGuaTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
