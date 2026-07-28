package com.detrapay.ui.home.orders.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.detrapay.data.model.Order
import com.detrapay.data.model.PaymentMethod
import com.detrapay.ui.home.orders.components.OrderFlowColors
import com.detrapay.ui.util.PaymentTypeRules
import com.detrapay.ui.home.orders.components.*

@Composable
fun MethodScreen(
    order: Order,
    paymentMethods: List<PaymentMethod>,
    onBack: () -> Unit,
    onSelectPaymentMethod: (PaymentMethod) -> Unit,
) {
    val online = paymentMethods.filter { it.isOnlinePayment }
    val recordOnly = paymentMethods.filterNot { it.isOnlinePayment }
    fun find(methods: List<PaymentMethod>, type: String): PaymentMethod? =
        methods.firstOrNull { PaymentTypeRules.normalize(it.paymentType) == type }
    val credit = find(online, "credito")
    val debit = find(online, "debito")
    val pix = find(online, "pix")
    val pixTransfer = find(recordOnly, "pix_manual")
    val storeCredit = find(recordOnly, "store_credit")
    val cash = recordOnly.firstOrNull {
        PaymentTypeRules.normalize(it.paymentType) in setOf("dinheiro", "cash")
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Column(modifier = Modifier.background(Color.White)) {
                NavBar("Pedido #${order.id}", onBack)
            }
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Escolha a forma de pagamento", color = OrderFlowColors.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("Cobrar na maquininha", color = OrderFlowColors.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                PaymentMethodRow("Crédito", "Parcelado ou à vista", Icons.Default.CreditCard, OrderFlowColors.Blue, credit != null) {
                    credit?.let(onSelectPaymentMethod)
                }
                PaymentMethodRow("Débito", "Pagamento imediato; revise o total", Icons.Default.CreditCard, OrderFlowColors.Green, debit != null) {
                    debit?.let(onSelectPaymentMethod)
                }
                PaymentMethodRow("Pix", "Recebimento rápido", Icons.Default.Bolt, OrderFlowColors.Teal, pix != null) {
                    pix?.let(onSelectPaymentMethod)
                }
                Text("Apenas registrar no pedido", modifier = Modifier.padding(top = 12.dp), color = OrderFlowColors.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmallMethod(Modifier.weight(1f), "Transferência\nPix", Icons.Default.Bolt, OrderFlowColors.Teal, pixTransfer != null) {
                        pixTransfer?.let(onSelectPaymentMethod)
                    }
                    SmallMethod(Modifier.weight(1f), "Crédito Loja", Icons.Default.AccountBalance, OrderFlowColors.Purple, storeCredit != null) {
                        storeCredit?.let(onSelectPaymentMethod)
                    }
                    SmallMethod(Modifier.weight(1f), "Dinheiro", Icons.Default.Payments, OrderFlowColors.Green, cash != null) {
                        cash?.let(onSelectPaymentMethod)
                    }
                }
            }
        }
    }
}
