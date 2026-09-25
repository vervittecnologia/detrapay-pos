package com.detrapay.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.detrapay.R

@OptIn(ExperimentalTextApi::class)
val DetrapayFontFamily = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    FontFamily(
        listOf(400, 500, 600, 700, 800, 900).map { weight ->
            Font(
                resId = R.font.inter,
                weight = FontWeight(weight),
                variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
            )
        },
    )
} else {
    FontFamily(Font(R.font.inter, FontWeight.Normal))
}

object DetrapayColors {
    val Primary = Color(0xFF0F64B3)
    val PrimaryDark = Color(0xFF0A4E8E)
    val PrimarySoft = Color(0xFFE8F1FB)
    val Background = Color(0xFFF6F9FD)
    val Surface = Color.White
    val SurfaceMuted = Color(0xFFEEF2F6)
    val Ink = Color(0xFF1A212D)
    val Muted = Color(0xFF58687E)
    val Quiet = Color(0xFF64748B)
    val Border = Color(0xFFCED5DE)
    val Success = Color(0xFF23813B)
    val SuccessSoft = Color(0xFFEAF7EC)
    val Error = Color(0xFFC92D32)
    val ErrorSoft = Color(0xFFFBEAEC)
    val Warning = Color(0xFFDB9101)
}

private val DetrapayColorScheme = lightColorScheme(
    primary = DetrapayColors.Primary,
    onPrimary = Color.White,
    primaryContainer = DetrapayColors.PrimarySoft,
    onPrimaryContainer = DetrapayColors.PrimaryDark,
    secondary = DetrapayColors.PrimaryDark,
    onSecondary = Color.White,
    background = DetrapayColors.Background,
    onBackground = DetrapayColors.Ink,
    surface = DetrapayColors.Surface,
    onSurface = DetrapayColors.Ink,
    surfaceVariant = DetrapayColors.SurfaceMuted,
    onSurfaceVariant = DetrapayColors.Muted,
    outline = DetrapayColors.Border,
    error = DetrapayColors.Error,
    onError = Color.White,
    errorContainer = DetrapayColors.ErrorSoft,
    onErrorContainer = DetrapayColors.Error,
)

private val DetrapayTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = DetrapayFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = DetrapayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = DetrapayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = DetrapayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = DetrapayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = DetrapayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = DetrapayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = DetrapayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = DetrapayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = DetrapayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = DetrapayFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
)

private val DetrapayShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun DetrapayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DetrapayColorScheme,
        typography = DetrapayTypography,
        shapes = DetrapayShapes,
        content = content,
    )
}
