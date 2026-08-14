package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ForyouVioletLight,
    onPrimary = Color.Black,
    primaryContainer = ForyouVioletDark,
    onPrimaryContainer = Color.White,
    secondary = ForyouPinkLight,
    onSecondary = Color.Black,
    secondaryContainer = ForyouPinkDark,
    onSecondaryContainer = Color.White,
    tertiary = ForyouCyan,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkSurfaceElevated,
    error = ForyouRose,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = ForyouViolet,
    onPrimary = Color.White,
    primaryContainer = ForyouVioletLight.copy(alpha = 0.2f),
    onPrimaryContainer = ForyouVioletDark,
    secondary = ForyouPink,
    onSecondary = Color.White,
    secondaryContainer = ForyouPinkLight.copy(alpha = 0.2f),
    onSecondaryContainer = ForyouPinkDark,
    tertiary = ForyouCyan,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    outlineVariant = LightSurfaceElevated,
    error = ForyouRose,
    onError = Color.White
)

@Composable
fun ForyouTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
