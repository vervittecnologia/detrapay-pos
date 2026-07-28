package com.detrapay.ui.home.orders.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.detrapay.data.model.PaymentMethod
import com.detrapay.ui.home.orders.OrderPaymentReview
import com.detrapay.ui.home.orders.OrderPresentation
import com.detrapay.ui.home.orders.components.AmountCard
import com.detrapay.ui.home.orders.components.NavBar
import com.detrapay.ui.home.orders.components.OrderFlowColors
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
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Column(modifier = Modifier.background(Color.White)) {
                NavBar("Confirmar pagamento", onBack)
                AmountCard(
                    "TOTAL A SER COBRADO",
                    OrderPresentation.formatCurrency(review.amountFinal),
                    Icons.Default.Payments,
                    OrderFlowColors.Blue,
                )
            }
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Revise antes de confirmar",
                    color = OrderFlowColors.Ink,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                SummaryRow("Forma de pagamento", paymentMethod.name)
                SummaryRow("Valor original", OrderPresentation.formatCurrency(review.amountOriginal))
                SummaryRow("Juros", OrderPresentation.formatCurrency(review.feeAmount))
                if (review.installments > 1) {
                    SummaryRow(
                        "Parcelamento",
                        "${review.installments}x de ${OrderPresentation.formatCurrency(review.installmentValue)} ${
                            if (hasInterest) "com juros" else "sem juros"
                        }",
                    )
                }
                SummaryRow(
                    if (hasInterest) "Total com juros" else "Total",
                    OrderPresentation.formatCurrency(review.amountFinal),
                    strong = true,
                )
                Button(
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    onClick = onConfirm,
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrderFlowColors.Blue),
                ) {
                    Text(
                        if (isSubmitting) "Registrando..." else "Confirmar pagamento",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
