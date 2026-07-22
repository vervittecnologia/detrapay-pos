package com.detrapay.ui.home.direct_checkout.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.detrapay.data.model.Order
import com.detrapay.data.model.remote.InstallmentFee
import com.detrapay.ui.home.direct_checkout.components.DirectCheckoutColors
import com.detrapay.ui.order_details.OrderDetailsPaymentMethodPickerBottomSheet
import com.detrapay.ui.util.PaymentTypeRules
import com.detrapay.ui.home.direct_checkout.components.*
import com.detrapay.ui.home.simplified.DirectCheckoutOrderPresentation

@Composable
fun DetailScreen(order: Order, onBack: () -> Unit, onPay: () -> Unit) {
    val summary = DirectCheckoutOrderPresentation.summary(order)
    val status = DirectCheckoutOrderPresentation.statusLabel(order)
    val percent = DirectCheckoutOrderPresentation.paidPercent(order)
    val isPending = status == "Pendente"

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Column(modifier = Modifier.background(Color.White)) {
                NavBar("Pedido #${order.id}", onBack)
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DirectCheckoutColors.Key)
                        .border(1.dp, DirectCheckoutColors.Border, RoundedCornerShape(14.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(if (isPending) DirectCheckoutColors.Amber else DirectCheckoutColors.BlueLight),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (isPending) DirectCheckoutColors.AmberSoft else DirectCheckoutColors.BlueSoft)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            text = status,
                            color = if (isPending) DirectCheckoutColors.AmberText else DirectCheckoutColors.Blue,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(displayDate(order.billingDate), color = DirectCheckoutColors.Muted, fontSize = 16.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        DetailCell("Cliente", order.customer.name.ifBlank { "Cliente" }, Modifier.weight(1f))
                        DetailCell("Vendedor", order.salesman?.name ?: "-", Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        DetailCell("WhatsApp", order.customer.phoneNumber.ifBlank { "-" }, Modifier.weight(1f))
                        DetailCell("Faturamento", displayDate(order.billingDate), Modifier.weight(1f))
                    }
                    DetailCell("CPF / CNPJ", order.customer.cpfCnpj.ifBlank { "-" }, Modifier.fillMaxWidth())
                }
            }

            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Resumo financeiro", color = DirectCheckoutColors.Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, DirectCheckoutColors.Border),
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        MetricCell("TOTAL", DirectCheckoutOrderPresentation.formatCurrency(order.originalAmount), DirectCheckoutColors.Ink, Modifier.weight(1f))
                        MetricCell("PAGO", DirectCheckoutOrderPresentation.formatCurrency(summary.registeredAmount), DirectCheckoutColors.Green, Modifier.weight(1f))
                        MetricCell("PENDENTE", DirectCheckoutOrderPresentation.formatCurrency(summary.missingAmount), if (isPending) DirectCheckoutColors.AmberText else DirectCheckoutColors.Blue, Modifier.weight(1f))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(DirectCheckoutColors.Track),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(percent / 100f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFF34D399), DirectCheckoutColors.Green))),
                        )
                    }
                    Text(
                        "$percent% quitado",
                        modifier = Modifier.padding(start = 18.dp, top = 8.dp, bottom = 16.dp),
                        color = DirectCheckoutColors.Muted,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pagamentos registrados", modifier = Modifier.weight(1f), color = DirectCheckoutColors.Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text(
                        order.receivables.size.toString(),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(DirectCheckoutColors.Track)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        color = DirectCheckoutColors.Muted,
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
                            .border(1.dp, DirectCheckoutColors.Border, RoundedCornerShape(18.dp))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = DirectCheckoutColors.Pale, modifier = Modifier.size(30.dp))
                        Text("Nenhum pagamento registrado", modifier = Modifier.padding(top = 8.dp), color = DirectCheckoutColors.Muted, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    order.receivables.forEach { receivable ->
                        SummaryRow(
                            receivable.paymentMethod.name,
                            DirectCheckoutOrderPresentation.formatCurrency(receivable.amountFinal),
                            subtitle = DirectCheckoutOrderPresentation.receivableStatusLabel(receivable),
                        )
                    }
                }
                if (summary.hasPendingBalance) {
                    Button(
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        onClick = onPay,
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DirectCheckoutColors.Blue),
                    ) {
                        Text("Pagar ${DirectCheckoutOrderPresentation.formatCurrency(summary.missingAmount)}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
