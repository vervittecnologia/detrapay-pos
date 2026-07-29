package com.detrapay.ui.home.orders.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.detrapay.data.model.PaymentMethod
import com.detrapay.ui.home.orders.OrderPaymentReview
import com.detrapay.ui.home.orders.OrderPresentation
import com.detrapay.ui.home.orders.components.AmountCard
import com.detrapay.ui.home.orders.components.NavBar
import com.detrapay.ui.home.orders.components.OrderFlowColors
import com.detrapay.ui.home.orders.components.OrderFlowFintechTheme
import com.detrapay.ui.home.orders.components.SummaryRow

@Composable
fun ReviewScreen(
    paymentMethod: PaymentMethod,
    review: OrderPaymentReview,
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
) {
    val hasInterest = review.feeAmount > 0.0
    LazyColumn(modifier = Modifier.fillMaxSize().background(OrderFlowFintechTheme.Canvas)) {
        item {
            Column(modifier = Modifier.background(OrderFlowFintechTheme.Canvas)) {
                NavBar("Confirmar pagamento", onBack)
            }
            Column(
                modifier = Modifier.padding(horizontal = 30.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                Text("Tudo certo?", color = OrderFlowFintechTheme.Primary, fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Confirme os dados antes de registrar o pagamento",
                    color = OrderFlowFintechTheme.Muted,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp,
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(OrderFlowFintechTheme.CardMuted)
                        .padding(horizontal = 36.dp, vertical = 34.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(OrderFlowFintechTheme.Primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, tint = Color.White, modifier = Modifier.size(38.dp))
                    }
                    Text(paymentMethod.name, color = OrderFlowFintechTheme.Ink, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Status: aguardando confirmacao",
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(OrderFlowFintechTheme.RedSoft)
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        color = OrderFlowFintechTheme.Red,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        OrderPresentation.formatCurrency(review.amountFinal),
                        color = OrderFlowFintechTheme.Ink,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        SummaryRow("Valor original", OrderPresentation.formatCurrency(review.amountOriginal))
                        SummaryRow("Taxas/Juros", OrderPresentation.formatCurrency(review.feeAmount))
                        if (review.installments > 1) {
                            SummaryRow(
                                "Parcelamento",
                                "${review.installments}x de ${OrderPresentation.formatCurrency(review.installmentValue)} ${
                                    if (hasInterest) "com juros" else "sem juros"
                                }",
                            )
                        }
                    }
                }
                Button(
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    onClick = onConfirm,
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrderFlowFintechTheme.Primary),
                ) {
                    Text(
                        if (isSubmitting) "Registrando..." else "Confirmar pagamento",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}
