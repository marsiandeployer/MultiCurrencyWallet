package com.swaponline.wallet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = McwBrand,
    secondary = McwBrandHover,
    background = McwBackground,
    surface = McwSurface,
    onSurfaceVariant = McwNotice,
)

@Composable
fun McwTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content,
    )
}
