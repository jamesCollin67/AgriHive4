package com.example.agrihive.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ============================================
// LIGHT THEME - Honey Yellow Primary
// ============================================

private val LightColorScheme = lightColorScheme(
    primary = HoneyPrimary,
    onPrimary = Color.White,
    primaryContainer = HoneyPrimaryLight,
    onPrimaryContainer = BrownDark,
    
    secondary = BrownDark,
    onSecondary = Color.White,
    secondaryContainer = BrownLight,
    onSecondaryContainer = Color.White,
    
    tertiary = StatusInfo,
    onTertiary = Color.White,
    tertiaryContainer = StatusInfoBg,
    onTertiaryContainer = StatusInfo,
    
    error = StatusError,
    onError = Color.White,
    errorContainer = StatusErrorBg,
    onErrorContainer = StatusError,
    
    background = BackgroundLight,
    onBackground = TextPrimary,
    
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundGray,
    onSurfaceVariant = TextSecondary,
    
    outline = Divider,
    outlineVariant = Color(0xFFE0E0E0),
)

// ============================================
// DARK THEME
// ============================================

private val DarkColorScheme = darkColorScheme(
    primary = HoneyPrimary,
    onPrimary = BrownDark,
    primaryContainer = HoneyPrimaryDark,
    onPrimaryContainer = Color.White,
    
    secondary = BrownLight,
    onSecondary = Color.White,
    secondaryContainer = BrownMedium,
    onSecondaryContainer = Color.White,
    
    tertiary = StatusInfoLight,
    onTertiary = BrownDark,
    tertiaryContainer = StatusInfo,
    onTertiaryContainer = Color.White,
    
    error = StatusErrorLight,
    onError = BrownDark,
    errorContainer = StatusError,
    onErrorContainer = Color.White,
    
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextSecondaryDark,
    
    outline = TextHintDark,
    outlineVariant = DividerDark,
)

// ============================================
// THEME COMPOSABLE
// ============================================

@Composable
fun AgriHiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled for consistent bee theme
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = if (darkTheme) {
                BackgroundDark.toArgb()
            } else {
                HoneyPrimary.toArgb()
            }
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
