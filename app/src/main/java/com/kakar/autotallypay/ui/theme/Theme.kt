package com.kakar.autotallypay.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kakar.autotallypay.R
import com.kakar.autotallypay.ui.AppLanguage

private val GreenPrimary = Color(0xFF1B7A3D)
private val GreenDark = Color(0xFF145C2E)
private val GreenLight = Color(0xFFE6F4EA)
private val RedAlert = Color(0xFFC62828)
private val RedLight = Color(0xFFFDECEA)
private val AmberWarn = Color(0xFFB7791F)

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = GreenLight,
    onPrimaryContainer = GreenDark,
    secondary = Color(0xFF2E7D6F),
    error = RedAlert,
    onError = Color.White,
    errorContainer = RedLight,
    onErrorContainer = RedAlert,
    background = Color(0xFFFAFAF7),
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A)
)

/** Noto Nastaliq Urdu, bundled for the Urdu interface. */
val NastaliqFamily = FontFamily(Font(R.font.noto_nastaliq_urdu))

/** Big, shopkeeper-friendly text: nothing small. */
fun autotallyTypography(useNastaliq: Boolean): Typography {
    val family = if (useNastaliq) NastaliqFamily else FontFamily.Default
    // Nastaliq needs taller lines than Latin text.
    val bodySize = if (useNastaliq) 20.sp else 18.sp
    val titleSize = if (useNastaliq) 26.sp else 24.sp
    return Typography(
        displaySmall = TextStyle(fontFamily = family, fontSize = 30.sp, lineHeight = 44.sp),
        headlineMedium = TextStyle(fontFamily = family, fontSize = titleSize, lineHeight = 38.sp),
        titleLarge = TextStyle(fontFamily = family, fontSize = 22.sp, lineHeight = 34.sp),
        titleMedium = TextStyle(fontFamily = family, fontSize = 20.sp, lineHeight = 32.sp),
        bodyLarge = TextStyle(fontFamily = family, fontSize = bodySize, lineHeight = 32.sp),
        bodyMedium = TextStyle(fontFamily = family, fontSize = 18.sp, lineHeight = 30.sp),
        labelLarge = TextStyle(fontFamily = family, fontSize = 18.sp, lineHeight = 28.sp),
        labelMedium = TextStyle(fontFamily = family, fontSize = 16.sp, lineHeight = 26.sp)
    )
}

private val BigShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun AutoTallyPayTheme(language: AppLanguage, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = autotallyTypography(language == AppLanguage.URDU),
        shapes = BigShapes,
        content = content
    )
}

/** Status colours used across screens. */
object StatusColors {
    val paidGreen = Color(0xFF1B7A3D)
    val paidGreenBg = Color(0xFFE6F4EA)
    val unpaidRed = Color(0xFFC62828)
    val unpaidRedBg = Color(0xFFFDECEA)
    val warnAmber = AmberWarn
    val warnAmberBg = Color(0xFFFFF4E0)
}
