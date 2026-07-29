package com.detrapay.ui.home.orders.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.detrapay.data.model.PaymentMethod
import com.detrapay.ui.home.orders.OrderPaymentReview
import com.detrapay.ui.home.orders.OrderPresentation
import com.detrapay.ui.home.orders.components.NavBar
import com.detrapay.ui.home.orders.components.OrderFlowFintechTheme

@Composable
fun ReviewScreen(
    paymentMethod: PaymentMethod,
    review: OrderPaymentReview,
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onConfirm: () -> Unit,
) {
    CompositionLocalProvider(
        LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = OrderFlowFintechTheme.Inter),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(OrderFlowFintechTheme.Canvas),
        ) {
            item {
                NavBar("Confirmar pagamento", onBack, onClose)
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = "Revise antes de confirmar",
                        color = OrderFlowFintechTheme.Ink,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Confira o método, o valor e as condições do pagamento.",
                        color = OrderFlowFintechTheme.Muted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                    SellerPaymentReviewCard(
                        paymentMethod = paymentMethod,
                        review = review,
                    )
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        onClick = onConfirm,
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrderFlowFintechTheme.Primary,
                        ),
                    ) {
                        Text(
                            text = if (isSubmitting) {
                                "Registrando..."
                            } else {
                                "Confirmar pagamento"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SellerPaymentReviewCard(
    paymentMethod: PaymentMethod,
    review: OrderPaymentReview,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, OrderFlowFintechTheme.Line),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = OrderFlowFintechTheme.PrimarySoft,
                            shape = RoundedCornerShape(12.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = OrderFlowFintechTheme.Primary,
                        modifier = Modifier.size(25.dp),
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                ) {
                    Text(
                        text = OrderPresentation.paymentReviewMethodLabel(
                            paymentMethod = paymentMethod,
                            installments = review.installments,
                        ).uppercase(),
                        color = OrderFlowFintechTheme.Ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Surface(
                        modifier = Modifier.padding(top = 4.dp),
                        shape = CircleShape,
                        color = OrderFlowFintechTheme.CardMuted,
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            text = "Aguardando confirmação",
                            color = OrderFlowFintechTheme.Muted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            ReviewDivider()
            ReviewLine(
                label = "Valor original",
                value = OrderPresentation.formatCurrency(review.amountOriginal),
            )
            ReviewLine(
                label = "Taxas e juros",
                value = OrderPresentation.formatCurrency(review.feeAmount),
            )
            if (review.installments > 1) {
                ReviewLine(
                    label = "Parcelamento",
                    value = "${review.installments}x de ${
                        OrderPresentation.formatCurrency(review.installmentValue)
                    }",
                )
            }
            ReviewDivider()
            ReviewLine(
                label = "TOTAL",
                value = OrderPresentation.formatCurrency(review.amountFinal),
                strong = true,
            )
        }
    }
}

@Composable
private fun ReviewLine(
    label: String,
    value: String,
    strong: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = label,
            color = if (strong) OrderFlowFintechTheme.Ink else OrderFlowFintechTheme.Muted,
            fontSize = if (strong) 13.sp else 12.sp,
            fontWeight = if (strong) FontWeight.Bold else FontWeight.Normal,
        )
        Text(
            text = value,
            color = if (strong) OrderFlowFintechTheme.Primary else OrderFlowFintechTheme.Ink,
            fontSize = if (strong) 20.sp else 14.sp,
            fontWeight = if (strong) FontWeight.ExtraBold else FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ReviewDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(OrderFlowFintechTheme.Line),
    )
}
