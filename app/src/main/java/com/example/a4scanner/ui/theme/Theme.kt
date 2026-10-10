package com.example.a4scanner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Blue40,
    secondary = BlueGrey40,
    tertiary = Teal40
)

private val DarkColors = darkColorScheme(
    primary = Blue80,
    secondary = BlueGrey80,
    tertiary = Teal80
)

@Composable
fun A4ScannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
