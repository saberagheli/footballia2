package com.example.ui.theme

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

private val DarkColorScheme =
  darkColorScheme(
    primary = SportLightGreen,
    secondary = SportGold,
    tertiary = SportMint,
    background = StadiumDark,
    surface = StadiumSurface,
    onPrimary = Color.White,
    onSecondary = StadiumDark,
    onTertiary = Color.White,
    onBackground = StadiumWhite,
    onSurface = StadiumWhite,
    surfaceVariant = StadiumCard,
    onSurfaceVariant = StadiumWhite
  )

private val LightColorScheme =
  lightColorScheme(
    primary = SportGreen,
    secondary = SportMint,
    tertiary = SportGold,
    background = Color(0xFFF1F8F3),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1A2E20),
    onSurface = Color(0xFF1A2E20),
    surfaceVariant = Color(0xFFE4EFE6),
    onSurfaceVariant = Color(0xFF1A2E20)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
