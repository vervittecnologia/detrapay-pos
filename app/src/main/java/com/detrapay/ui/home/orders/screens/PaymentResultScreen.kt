package com.detrapay.ui.home.orders.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.detrapay.ui.home.orders.OrderPresentation
import com.detrapay.ui.home.orders.components.OrderFlowColors
import com.detrapay.ui.home.orders.components.SummaryRow

@Composable
fun PaymentResultScreen(
    total: Double,
    paymentMethod: String,
    installments: Int,
    transactionId: String?,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(OrderFlowColors.GreenSoft, RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Pagamento aprovado",
                tint = OrderFlowColors.GreenText,
                modifier = Modifier.size(54.dp),
            )
        }
        Text(
            text = "Pagamento aprovado",
            modifier = Modifier.padding(top = 20.dp),
            color = OrderFlowColors.Ink,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Text(
            text = OrderPresentation.formatCurrency(total),
            modifier = Modifier.padding(top = 8.dp),
            color = OrderFlowColors.Blue,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SummaryRow("Forma de pagamento", paymentMethod)
            SummaryRow("Parcelamento", if (installments > 1) "${installments}x" else "À vista")
            transactionId?.takeIf { it.isNotBlank() }?.let {
                SummaryRow("Identificador", it)
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrderFlowColors.Blue),
        ) {
            Text("Voltar para pedidos", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
