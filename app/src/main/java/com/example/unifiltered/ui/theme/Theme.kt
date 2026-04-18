package com.example.unifiltered.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Ember is inherently a dark theme, so we map it primarily to the DarkColorScheme
private val EmberColorScheme = darkColorScheme(
    primary = EmberPrimary,
    onPrimary = EmberBackground,
    secondary = EmberSecondary,
    onSecondary = EmberTextHighlight,
    tertiary = EmberPrimaryVariant,
    background = EmberBackground,
    onBackground = EmberTextHighlight,
    surface = EmberSurface,
    onSurface = EmberTextHighlight
)

@Composable
fun UniFilteredTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set dynamicColor to false by default so the Ember theme isn't overwritten by system colors
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Force the Ember scheme regardless of system light/dark mode,
        // since Ember is a highly specific stylistic choice.
        else -> EmberColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Ensure you have your typography defined
        content = content
    )
}