package com.ivan.wallet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Moss,
    secondary = Amber,
    tertiary = Earth,
    background = Sand,
    surface = Paper
)

private val DarkColors = darkColorScheme(
    primary = Amber,
    secondary = Moss,
    tertiary = Sand
)

@Composable
fun WalletTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        content = content
    )
}
