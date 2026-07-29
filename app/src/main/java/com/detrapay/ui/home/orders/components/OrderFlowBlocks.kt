package com.detrapay.ui.home.orders.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.detrapay.data.model.remote.InstallmentFee
import com.detrapay.ui.home.orders.components.OrderFlowColors
import com.detrapay.ui.home.orders.OrderPresentation
import com.detrapay.ui.util.InstallmentQuotePresenter

@Composable
fun NavBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrderFlowFintechTheme.Canvas)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(47.dp)
                .clip(CircleShape)
                .background(OrderFlowFintechTheme.Muted.copy(alpha = 0.25f)),
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = OrderFlowFintechTheme.Quiet)
        }
        Text(
            title,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            color = OrderFlowFintechTheme.Ink,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun AmountCard(label: String, amount: String, icon: ImageVector, iconColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(OrderFlowFintechTheme.Primary)
            .padding(horizontal = 28.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White)
        }
        Column(modifier = Modifier.padding(start = 14.dp)) {
            Text(label, color = Color.White.copy(alpha = 0.68f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(amount, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun PaymentMethodRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(10.dp))
            .background(OrderFlowFintechTheme.Card)
            .border(
                if (title == "Crédito" && enabled) 2.dp else 1.dp,
                if (title == "Crédito" && enabled) OrderFlowColors.Blue else OrderFlowColors.Border,
                RoundedCornerShape(10.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MethodIcon(icon, color)
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(title, color = OrderFlowFintechTheme.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = OrderFlowFintechTheme.Muted, fontSize = 12.sp)
        }
        Text("›", color = OrderFlowFintechTheme.Muted, fontSize = 28.sp)
    }
}

@Composable
fun SmallMethod(modifier: Modifier, title: String, icon: ImageVector, color: Color, enabled: Boolean, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(10.dp))
            .background(OrderFlowFintechTheme.Card)
            .border(1.dp, OrderFlowColors.Border, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MethodIcon(icon, color)
        Text(
            title,
            modifier = Modifier.padding(top = 8.dp),
            color = OrderFlowFintechTheme.Ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun MethodIcon(icon: ImageVector, color: Color) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun InstallmentRow(
    amount: Double,
    installment: InstallmentFee,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val presentation = InstallmentQuotePresenter.present(amount, installment)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(OrderFlowFintechTheme.Card)
            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) OrderFlowColors.Blue else OrderFlowColors.Border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${installment.installmentNumber}x",
            modifier = Modifier.width(42.dp),
            color = if (isSelected) OrderFlowFintechTheme.Primary else OrderFlowFintechTheme.Muted,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(presentation.installmentLabel, color = OrderFlowFintechTheme.Ink, fontWeight = FontWeight.Bold)
            Text(presentation.originalLabel, color = OrderFlowFintechTheme.Muted, fontSize = 12.sp)
            Text(presentation.totalLabel, color = OrderFlowFintechTheme.Muted, fontSize = 12.sp)
        }
    }
}

@Composable
fun Metric(label: String, value: String, color: Color, alignEnd: Boolean = false) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(label, color = OrderFlowColors.Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DetailCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .border(0.5.dp, OrderFlowColors.Border)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Text(label.uppercase(), color = OrderFlowColors.Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(
            value,
            modifier = Modifier.padding(top = 6.dp),
            color = OrderFlowColors.Text,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun MetricCell(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 10.dp, vertical = 16.dp),
    ) {
        Text(label, color = OrderFlowColors.Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
fun SummaryRow(label: String, value: String, strong: Boolean = false, subtitle: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (strong) 10.dp else 0.dp))
            .background(if (strong) OrderFlowFintechTheme.CardMuted else OrderFlowFintechTheme.Card)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = if (strong) OrderFlowColors.Ink else OrderFlowColors.Muted, fontSize = 14.sp, fontWeight = if (strong) FontWeight.Bold else FontWeight.Normal)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    modifier = Modifier.padding(top = 3.dp),
                    color = OrderFlowColors.Blue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Text(value, color = OrderFlowColors.Ink, fontSize = 14.sp, fontWeight = if (strong) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
fun LoadingBlock(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = OrderFlowColors.Blue)
        Text(text, modifier = Modifier.padding(top = 12.dp), color = OrderFlowColors.Muted)
    }
}

@Composable
fun EmptyBlock(title: String, subtitle: String, actionText: String, onAction: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, color = OrderFlowColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(subtitle, modifier = Modifier.padding(top = 8.dp), color = OrderFlowColors.Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
        Button(
            modifier = Modifier.padding(top = 16.dp),
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(containerColor = OrderFlowColors.Blue),
        ) {
            Text(actionText)
        }
    }
}

fun displayDate(raw: String): String {
    val parts = raw.take(10).split("-")
    return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else raw
}

fun currencyText(value: String): String {
    return if (value.trim().startsWith("R$")) value.trim() else "R$ ${value.trim()}"
}

fun simulatorShareText(amount: Double, installments: List<InstallmentFee>): String {
    val lines = installments.joinToString("\n") { installment ->
        val presentation = InstallmentQuotePresenter.present(amount, installment)
        listOf(
            presentation.originalLabel,
            presentation.installmentLabel,
            presentation.totalLabel,
        ).joinToString(" | ")
    }
    return "Simulação de Crédito - ${OrderPresentation.formatCurrency(amount)}\n\n$lines"
}
