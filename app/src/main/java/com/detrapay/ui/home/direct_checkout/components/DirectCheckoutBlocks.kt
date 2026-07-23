package com.detrapay.ui.home.direct_checkout.components

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
import com.detrapay.ui.home.direct_checkout.components.DirectCheckoutColors
import com.detrapay.ui.home.simplified.DirectCheckoutOrderPresentation

@Composable
fun NavBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(DirectCheckoutColors.Key),
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = DirectCheckoutColors.Text)
        }
        Text(
            title,
            modifier = Modifier.padding(start = 10.dp),
            color = DirectCheckoutColors.Ink,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun AmountCard(label: String, amount: String, icon: ImageVector, iconColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(DirectCheckoutColors.Key)
            .border(1.dp, DirectCheckoutColors.Border, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iconColor.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconColor)
        }
        Column(modifier = Modifier.padding(start = 14.dp)) {
            Text(label, color = DirectCheckoutColors.Faint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(amount, color = DirectCheckoutColors.Ink, fontSize = 25.sp, fontWeight = FontWeight.Black)
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
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(
                if (title == "Crédito" && enabled) 2.dp else 1.dp,
                if (title == "Crédito" && enabled) DirectCheckoutColors.Blue else DirectCheckoutColors.Border,
                RoundedCornerShape(18.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MethodIcon(icon, color)
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(title, color = DirectCheckoutColors.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = DirectCheckoutColors.Muted, fontSize = 12.sp)
        }
        Text("›", color = DirectCheckoutColors.Faint, fontSize = 28.sp)
    }
}

@Composable
fun SmallMethod(modifier: Modifier, title: String, icon: ImageVector, color: Color, enabled: Boolean, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, DirectCheckoutColors.Border, RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MethodIcon(icon, color)
        Text(
            title,
            modifier = Modifier.padding(top = 8.dp),
            color = DirectCheckoutColors.Text,
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
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun InstallmentRow(installment: InstallmentFee, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) DirectCheckoutColors.Blue else DirectCheckoutColors.Border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${installment.installmentNumber}x",
            modifier = Modifier.width(42.dp),
            color = if (isSelected) DirectCheckoutColors.Blue else DirectCheckoutColors.Faint,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(currencyText(installment.installmentValue), color = DirectCheckoutColors.Ink, fontWeight = FontWeight.Bold)
            Text("Total ${currencyText(installment.totalValue)}", color = DirectCheckoutColors.Faint, fontSize = 12.sp)
        }
        Text(
            if (installment.noInterest) "sem juros" else "taxa incl.",
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(if (installment.noInterest) DirectCheckoutColors.GreenSoft else DirectCheckoutColors.Key)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            color = if (installment.noInterest) DirectCheckoutColors.Green else DirectCheckoutColors.Muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun Metric(label: String, value: String, color: Color, alignEnd: Boolean = false) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(label, color = DirectCheckoutColors.Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DetailCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .border(0.5.dp, DirectCheckoutColors.Border)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Text(label.uppercase(), color = DirectCheckoutColors.Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(
            value,
            modifier = Modifier.padding(top = 6.dp),
            color = DirectCheckoutColors.Text,
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
        Text(label, color = DirectCheckoutColors.Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
fun SummaryRow(label: String, value: String, strong: Boolean = false, subtitle: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (strong) 14.dp else 0.dp))
            .background(if (strong) DirectCheckoutColors.Key else Color.White)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = if (strong) DirectCheckoutColors.Ink else DirectCheckoutColors.Muted, fontSize = 14.sp, fontWeight = if (strong) FontWeight.Bold else FontWeight.Normal)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    modifier = Modifier.padding(top = 3.dp),
                    color = DirectCheckoutColors.Blue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Text(value, color = DirectCheckoutColors.Ink, fontSize = 14.sp, fontWeight = if (strong) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
fun LoadingBlock(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = DirectCheckoutColors.Blue)
        Text(text, modifier = Modifier.padding(top = 12.dp), color = DirectCheckoutColors.Muted)
    }
}

@Composable
fun EmptyBlock(title: String, subtitle: String, actionText: String, onAction: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, color = DirectCheckoutColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(subtitle, modifier = Modifier.padding(top = 8.dp), color = DirectCheckoutColors.Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
        Button(
            modifier = Modifier.padding(top = 16.dp),
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(containerColor = DirectCheckoutColors.Blue),
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
        "${installment.installmentNumber}x de ${currencyText(installment.installmentValue)} - Total ${currencyText(installment.totalValue)}"
    }
    return "Simulação de Crédito - ${DirectCheckoutOrderPresentation.formatCurrency(amount)}\n\n$lines"
}
