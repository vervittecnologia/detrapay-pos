package com.detrapay.ui.home.orders.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.detrapay.R

internal object OrderFlowFintechTheme {
    val Inter = FontFamily(
        Font(R.font.inter, FontWeight.Normal),
        Font(R.font.inter, FontWeight.Medium),
        Font(R.font.inter, FontWeight.SemiBold),
        Font(R.font.inter, FontWeight.Bold),
        Font(R.font.inter, FontWeight.ExtraBold),
    )
    val Canvas = Color(0xFFF6F9FD)
    val Primary = Color(0xFF0F64B3)
    val PrimaryDark = Color(0xFF0A4E8E)
    val PrimarySoft = Color(0xFFE8F1FB)
    val Ink = Color(0xFF1A212D)
    val Body = Color(0xFF1A212D)
    val Muted = Color(0xFF58687E)
    val Quiet = Color(0xFF7A8798)
    val Card = Color(0xFFFFFFFF)
    val CardMuted = Color(0xFFEEF2F6)
    val Line = Color(0xFFCED5DE)
    val Green = Color(0xFF35A748)
    val GreenSoft = Color(0xFFEAF7EC)
    val Red = Color(0xFFC92D32)
    val RedSoft = Color(0xFFFBEAEC)
    val Teal = Color(0xFF168C92)
    val Yellow = Color(0xFFFFC145)
    val Purple = Color(0xFF7357B5)
    val Search = Color(0xFFEEF2F6)
    val BottomBar = Color(0xFFFFFFFF)
}

internal object OrderFlowColors {
    val Background = OrderFlowFintechTheme.Canvas
    val Ink = OrderFlowFintechTheme.Ink
    val Text = OrderFlowFintechTheme.Body
    val Muted = OrderFlowFintechTheme.Quiet
    val Faint = OrderFlowFintechTheme.Muted
    val Pale = Color(0xFFC5CBD1)
    val Border = OrderFlowFintechTheme.Line
    val Track = OrderFlowFintechTheme.CardMuted
    val Key = OrderFlowFintechTheme.CardMuted
    val MutedSurface = OrderFlowFintechTheme.CardMuted
    val Blue = OrderFlowFintechTheme.Primary
    val BlueLight = Color(0xFF2D7EC5)
    val BlueSoft = OrderFlowFintechTheme.PrimarySoft
    val BlueBorder = Color(0xFFCADDEF)
    val BlueOnSoft = Color(0xFFEAF3FB)
    val IndigoSoft = Color(0xFFF4F8FC)
    val Amber = Color(0xFFFBBF24)
    val AmberSoft = Color(0xFFFFFBEB)
    val AmberText = Color(0xFFD97706)
    val Warning = Color(0xFFDB9101)
    val WarningSoft = Color(0xFFFEF6E7)
    val WarningText = Color(0xFF73510D)
    val Green = OrderFlowFintechTheme.Green
    val GreenSoft = OrderFlowFintechTheme.GreenSoft
    val Red = OrderFlowFintechTheme.Red
    val RedSoft = OrderFlowFintechTheme.RedSoft
    val Teal = OrderFlowFintechTheme.Teal
    val Purple = OrderFlowFintechTheme.Purple
    val WhatsappGreen = Color(0xFF25D366)
}
