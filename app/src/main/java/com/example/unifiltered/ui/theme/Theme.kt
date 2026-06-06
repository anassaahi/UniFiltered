package com.example.unifiltered.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val IndustrialColorScheme = darkColorScheme(
    primary = IndustrialPrimary,
    onPrimary = ZincBlack,
    secondary = IndustrialSecondary,
    onSecondary = IndustrialText,
    tertiary = IndustrialPrimary,
    background = IndustrialBackground,
    onBackground = IndustrialText,
    surface = IndustrialSurface,
    onSurface = IndustrialText,
    outline = DarkGraphite
)

@Composable
fun UniFilteredTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> IndustrialColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
