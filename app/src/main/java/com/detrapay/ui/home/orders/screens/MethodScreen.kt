package com.detrapay.ui.home.orders.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.detrapay.data.model.Order
import com.detrapay.data.model.PaymentMethod
import com.detrapay.ui.home.orders.OrderPresentation
import com.detrapay.ui.home.orders.components.NavBar
import com.detrapay.ui.home.orders.components.OrderFlowFintechTheme
import com.detrapay.ui.util.PaymentTypeRules

@Composable
fun MethodScreen(
    order: Order,
    paymentMethods: List<PaymentMethod>,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onSelectPaymentMethod: (PaymentMethod) -> Unit,
) {
    val pending = OrderPresentation.summary(order).missingAmount
    val paymentMethodTypes = OrderPresentation.paymentMethodTypes(paymentMethods)
    val online = paymentMethodTypes.filter { it.isOnlinePayment }
    val recordOnly = paymentMethodTypes.filterNot { it.isOnlinePayment }

    CompositionLocalProvider(
        LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = OrderFlowFintechTheme.Inter),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(OrderFlowFintechTheme.Canvas),
        ) {
            item {
                NavBar("Novo pagamento", onBack, onClose)
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, OrderFlowFintechTheme.Line),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "PEDIDO #${order.id}",
                                    color = OrderFlowFintechTheme.Muted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = "Valor pendente",
                                    color = OrderFlowFintechTheme.Ink,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Text(
                                text = OrderPresentation.formatCurrency(pending),
                                color = OrderFlowFintechTheme.Primary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }

                    Text(
                        text = "Escolha a forma de pagamento",
                        color = OrderFlowFintechTheme.Ink,
                        fontSize = 20.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    if (online.isNotEmpty()) {
                        Text(
                            text = "PAGAMENTOS NA MAQUININHA",
                            color = OrderFlowFintechTheme.Muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        online.forEach { method ->
                            SellerPaymentMethodCard(
                                method = method,
                                onClick = { onSelectPaymentMethod(method) },
                            )
                        }
                    }

                    if (recordOnly.isNotEmpty()) {
                        Text(
                            modifier = Modifier.padding(top = 6.dp),
                            text = "REGISTRAR PAGAMENTO",
                            color = OrderFlowFintechTheme.Muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        recordOnly.forEach { method ->
                            SellerPaymentMethodCard(
                                method = method,
                                onClick = { onSelectPaymentMethod(method) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SellerPaymentMethodCard(
    method: PaymentMethod,
    onClick: () -> Unit,
) {
    val normalized = PaymentTypeRules.normalize(method.paymentType ?: method.name)
    val icon: ImageVector = when (normalized) {
        "pix", "pix_manual" -> Icons.Default.Bolt
        "dinheiro" -> Icons.Default.Payments
        "store_credit" -> Icons.Default.AccountBalance
        else -> Icons.Default.CreditCard
    }
    val title = OrderPresentation.paymentMethodTypeLabel(method)
    val subtitle = when (normalized) {
        "credito" -> "À vista ou parcelado"
        "debito" -> "Pagamento na maquininha"
        "pix" -> "Pix pela maquininha"
        "pix_manual" -> "Registrar transferência já recebida"
        "dinheiro" -> "Registrar valor recebido"
        "store_credit" -> "Registrar crédito concedido"
        else -> "Registrar pagamento"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, OrderFlowFintechTheme.Line),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        color = OrderFlowFintechTheme.PrimarySoft,
                        shape = RoundedCornerShape(12.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = OrderFlowFintechTheme.Primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    text = title,
                    color = OrderFlowFintechTheme.Ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = subtitle,
                    color = OrderFlowFintechTheme.Muted,
                    fontSize = 12.sp,
                )
            }
            Text(
                text = "›",
                color = OrderFlowFintechTheme.Muted,
                fontSize = 26.sp,
            )
        }
    }
}
