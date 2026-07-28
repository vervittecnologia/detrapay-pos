package com.detrapay.ui.home.orders.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.detrapay.data.model.Order
import com.detrapay.ui.home.orders.components.OrderFlowColors
import com.detrapay.ui.home.orders.components.*
import com.detrapay.ui.home.orders.OrderPresentation

@Composable
fun DetailScreen(order: Order, onBack: () -> Unit, onPay: () -> Unit) {
    val summary = OrderPresentation.summary(order)
    val status = OrderPresentation.statusLabel(order)
    val percent = OrderPresentation.paidPercent(order)
    val isPending = status == "Pendente"

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            NavBar("Pedido #${order.id}", onBack)

            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Resumo financeiro", color = OrderFlowColors.Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, OrderFlowColors.Border),
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        MetricCell("TOTAL", OrderPresentation.formatCurrency(order.originalAmount), OrderFlowColors.Ink, Modifier.weight(1f))
                        MetricCell("PAGO", OrderPresentation.formatCurrency(summary.registeredAmount), OrderFlowColors.Green, Modifier.weight(1f))
                        MetricCell("PENDENTE", OrderPresentation.formatCurrency(summary.missingAmount), if (isPending) OrderFlowColors.AmberText else OrderFlowColors.Blue, Modifier.weight(1f))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(OrderFlowColors.Track),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(percent / 100f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFF34D399), OrderFlowColors.Green))),
                        )
                    }
                    Text(
                        "$percent% quitado",
                        modifier = Modifier.padding(start = 18.dp, top = 8.dp, bottom = 16.dp),
                        color = OrderFlowColors.Muted,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pagamentos registrados", modifier = Modifier.weight(1f), color = OrderFlowColors.Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text(
                        order.receivables.size.toString(),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(OrderFlowColors.Track)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        color = OrderFlowColors.Muted,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (order.receivables.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White)
                            .border(1.dp, OrderFlowColors.Border, RoundedCornerShape(18.dp))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = OrderFlowColors.Pale, modifier = Modifier.size(30.dp))
                        Text("Nenhum pagamento registrado", modifier = Modifier.padding(top = 8.dp), color = OrderFlowColors.Muted, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    order.receivables.forEach { receivable ->
                        SummaryRow(
                            receivable.paymentMethod.name,
                            OrderPresentation.formatCurrency(receivable.amountFinal),
                            subtitle = OrderPresentation.receivableStatusLabel(receivable),
                        )
                    }
                }
                if (summary.hasPendingBalance) {
                    Button(
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        onClick = onPay,
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrderFlowColors.Blue),
                    ) {
                        Text("Pagar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
