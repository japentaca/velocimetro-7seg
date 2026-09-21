package com.velocimetro.seg7.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LedGreen = Color(0xFF00FF66)
val LedGreenDim = Color(0xFF0C2A18)

@Composable
fun VelocimetroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = LedGreen,
            onPrimary = Color.Black,
            background = Color.Black,
            onBackground = LedGreen,
            surface = Color(0xFF08120C),
            onSurface = LedGreen,
            surfaceVariant = Color(0xFF102018),
            onSurfaceVariant = LedGreen
        ),
        content = content
    )
}
