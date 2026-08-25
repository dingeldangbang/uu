package com.secureguard.enterprise.presentation.theme

import android.content.Context
import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.GoogleFont.Provider
import androidx.compose.ui.unit.sp

/**
 * Design-System „Industrial Precision 2.0" (Stitch-Referenz):
 *   Light — Primary #005EB8 · Secondary #1A1C1E · Tertiary #EE3124 · Neutral #F8F9FA
 *   Dark  — Primary #A9C7FF · Secondary #C6C6C9 · Tertiary #FFB4A9 · Neutral #76777B
 * Schrift: Inter (Google Fonts Provider, Fallback SansSerif).
 */
private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF002F5F),
    primaryContainer = Color(0xFF004A8F),
    onPrimaryContainer = Color(0xFFD3E3FF),
    secondary = Color(0xFFC6C6C9),
    onSecondary = Color(0xFF1A1C1E),
    secondaryContainer = Color(0xFF30343A),
    onSecondaryContainer = Color(0xFFE2E3E5),
    tertiary = Color(0xFFFFB4A9),
    onTertiary = Color(0xFF4A0E07),
    tertiaryContainer = Color(0xFF8C2A1C),
    onTertiaryContainer = Color(0xFFFFDAD4),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFF8F9FA),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFF8F9FA),
    surfaceVariant = Color(0xFF30343A),
    onSurfaceVariant = Color(0xFFC6C6C9),
    error = Color(0xFFFFB4A9),
    onError = Color(0xFF4A0E07)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF005EB8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E3FF),
    onPrimaryContainer = Color(0xFF001B3D),
    secondary = Color(0xFF1A1C1E),
    onSecondary = Color(0xFFF8F9FA),
    secondaryContainer = Color(0xFFE2E3E5),
    onSecondaryContainer = Color(0xFF1A1C1E),
    tertiary = Color(0xFFEE3124),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFB4A9),
    onTertiaryContainer = Color(0xFF4A0E07),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF1A1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE3E8EE),
    onSurfaceVariant = Color(0xFF44484C),
    error = Color(0xFFEE3124),
    onError = Color.White
)

/**
 * Inter-Font via Downloadable Fonts (Google Fonts Provider).
 *
 * Lädt die echten Inter-Glyphen zur Laufzeit über den Google Fonts
 * Provider (Standard auf Honeywell CT45P mit GMS). Fallback auf
 * SansSerif, falls der Provider offline / nicht verfügbar ist.
 *
 * Alternativ (ohne Netz zur Laufzeit): inter_regular.ttf,
 * inter_medium.ttf, inter_bold.ttf nach app/src/main/res/font/ legen
 * und `fontFamilyOf()` durch die Ressourcen-Variante ersetzen.
 */
private fun interFontFamily(context: Context): FontFamily {
    return try {
        val provider = Provider(
            authority = Provider.DefaultProvider.authority,
            packageName = Provider.DefaultProvider.packageName,
            certs = Provider.DefaultProvider.certs
        )
        FontFamily(
            Font(GoogleFont("Inter"), provider, FontWeight.Normal),
            Font(GoogleFont("Inter"), provider, FontWeight.Medium),
            Font(GoogleFont("Inter"), provider, FontWeight.Bold)
        )
    } catch (e: Exception) {
        Log.w("SecureGuardTheme", "Inter nicht verfügbar → SansSerif", e)
        FontFamily.SansSerif
    }
}

/** Baut die Typography mit einer Font-Familie. */
private fun buildTypography(family: FontFamily): Typography = Typography(
    displayLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall  = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold,    fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium= TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge    = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium   = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium,   fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall    = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium,   fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge     = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal,   fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium    = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal,   fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall     = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal,   fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge    = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium,   fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium   = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium,   fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall    = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium,   fontSize = 11.sp, lineHeight = 16.sp)
)

@Composable
fun SecureGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val family = remember { interFontFamily(LocalContext.current) }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = remember(family) { buildTypography(family) },
        content = content
    )
}
