package com.example.video_player_native

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Defina sua fonte aqui ou use a padrão
val AppFontFamily = FontFamily.Default

val Typography = Typography(
    body1 = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    // Defina outros estilos conforme necessário
    h1 = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    // Adicione mais estilos como body2, h2, subtitle1, etc.
)
