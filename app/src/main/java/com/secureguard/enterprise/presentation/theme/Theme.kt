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

private val DarkColors = darkColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF003C8F),
    secondary = Color(0xFFFFB300),
    onSecondary = Color.Black,
    tertiary = Color(0xFF2E7D32),
    background = Color(0xFF0D1117),
    onBackground = Color(0xFFE6EDF3),
    surface = Color(0xFF161B22),
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF1F2630),
    error = Color(0xFFE53935)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    secondary = Color(0xFFEF6C00),
    tertiary = Color(0xFF2E7D32),
    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF0D1117),
    surface = Color.White,
    onSurface = Color(0xFF0D1117),
    surfaceVariant = Color(0xFFE3E8EE),
    error = Color(0xFFC62828)
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
