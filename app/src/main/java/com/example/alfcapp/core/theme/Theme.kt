package com.example.alfcapp.core.theme

import android.app.Activity
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.alfcapp.screens.ChurchBackground
import com.example.alfcapp.screens.ChurchError
import com.example.alfcapp.screens.ChurchOnBackground
import com.example.alfcapp.screens.ChurchOnPrimary
import com.example.alfcapp.screens.ChurchOnSurface
import com.example.alfcapp.screens.ChurchPrimary
import com.example.alfcapp.screens.ChurchSecondary
import com.example.alfcapp.screens.ChurchSurface
import com.example.alfcapp.screens.ChurchTypography


private val LightColorScheme = lightColorScheme(
    primary = ChurchPrimary,
    onPrimary = ChurchOnPrimary,
    primaryContainer = Color(0xFFE3F2FD),
    secondary = ChurchSecondary,
    secondaryContainer = Color(0xFFFFE0B2),
    background = ChurchBackground,
    surface = ChurchSurface,
    onBackground = ChurchOnBackground,
    onSurface = ChurchOnSurface,
    error = ChurchError

)

private val DarkColorScheme = darkColorScheme(
    primary = ChurchPrimary,
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF004A77),
    secondary = ChurchSecondary,
    secondaryContainer = Color(0xFF5D4037),
    onBackground = Color(0xFFE1E1E1),
    onSurface = Color(0xFFE1E1E1),
    error = Color(0xFFCF6679)
)

@Composable
fun ALFCAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ChurchTypography,
        content = content
    )
}