package com.detrapay.ui.home.orders.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.detrapay.data.model.Order
import com.detrapay.data.model.PaymentMethod
import com.detrapay.ui.home.orders.OrderPresentation
import com.detrapay.ui.home.orders.components.OrderFlowColors
import com.detrapay.ui.home.orders.components.OrderFlowFintechTheme
import com.detrapay.ui.home.orders.components.*

@Composable
fun KeypadScreen(
    order: Order,
    paymentMethod: PaymentMethod,
    displayAmount: String,
    pendingAmountLabel: String,
    canPay: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onKey: (String) -> Unit,
    onUsePendingAmount: () -> Unit,
    onContinue: () -> Unit,
) {
    CompositionLocalProvider(
        LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = OrderFlowFintechTheme.Inter),
    ) {
        Column(modifier = Modifier.fillMaxSize().background(OrderFlowFintechTheme.Canvas)) {
            NavBar("Novo pagamento", onBack, onClose)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                shape = RoundedCornerShape(22.dp),
                color = OrderFlowFintechTheme.Card,
                shadowElevation = 2.dp,
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(OrderFlowFintechTheme.PrimarySoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = OrderFlowFintechTheme.Primary,
                                modifier = Modifier.size(23.dp),
                            )
                        }
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                order.customer.name.ifBlank { "Cliente" },
                                color = OrderFlowFintechTheme.Ink,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Pedido #${order.id}",
                                color = OrderFlowFintechTheme.Muted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            OrderPresentation.paymentMethodTypeLabel(paymentMethod),
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(OrderFlowFintechTheme.PrimarySoft)
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            color = OrderFlowFintechTheme.PrimaryDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        "VALOR DO PAGAMENTO",
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 14.dp),
                        color = OrderFlowFintechTheme.Quiet,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                    Text(
                        displayAmount,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = OrderFlowFintechTheme.Ink,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("VALOR PENDENTE", color = OrderFlowFintechTheme.Quiet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(pendingAmountLabel, color = OrderFlowFintechTheme.Muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            "USAR VALOR",
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(OrderFlowFintechTheme.PrimarySoft)
                                .clickable(enabled = !isLoading, onClick = onUsePendingAmount)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            color = OrderFlowFintechTheme.Primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                if (errorMessage != null) {
                    Text(errorMessage, color = OrderFlowColors.AmberText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            SellerNativeKeypad(
                modifier = Modifier.weight(1f),
                keyHeight = 44.dp,
                enabled = !isLoading,
                onKey = onKey,
            )
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .height(52.dp),
                onClick = onContinue,
                enabled = canPay && !isLoading,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrderFlowFintechTheme.Primary,
                    disabledContainerColor = OrderFlowFintechTheme.Line,
                    disabledContentColor = OrderFlowFintechTheme.Muted,
                ),
            ) {
                Text(
                    text = "CONTINUAR",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                )
            }
        }
    }
}

@Composable
private fun SellerNativeKeypad(
    modifier: Modifier = Modifier,
    keyHeight: androidx.compose.ui.unit.Dp = 82.dp,
    enabled: Boolean = true,
    onKey: (String) -> Unit,
) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ",", "0", "DEL")
    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(keyHeight.coerceAtLeast(56.dp))
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (key == "DEL") {
                                    OrderFlowFintechTheme.PrimarySoft
                                } else {
                                    Color.Transparent
                                },
                            )
                            .clickable(enabled = enabled) { onKey(key) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (key == "DEL") {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Apagar",
                                tint = OrderFlowFintechTheme.Primary,
                                modifier = Modifier.size(24.dp),
                            )
                        } else {
                            Text(
                                key,
                                color = OrderFlowColors.Text,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}
