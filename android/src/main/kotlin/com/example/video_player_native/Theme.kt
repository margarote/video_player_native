package com.example.video_player_native
// ui/theme/Theme.kt

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.*

import androidx.compose.runtime.Composable

private val DarkColorPalette = darkColors(
    primary = Purple200,
    primaryVariant = Purple700,
    secondary = Teal200
)

private val LightColorPalette = lightColors(
    primary = Purple500,
    primaryVariant = Purple700,
    secondary = Teal200

    /* Outras cores podem ser sobrescritas aqui */
)

@Composable
fun VideoPlayerNativeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography, // Certifique-se de que Typography está definido
        shapes = Shapes,         // Certifique-se de que Shapes está definido
        content = content
    )
}
