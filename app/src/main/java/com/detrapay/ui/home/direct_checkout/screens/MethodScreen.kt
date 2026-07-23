package com.detrapay.ui.home.direct_checkout.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
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
import com.detrapay.ui.home.direct_checkout.components.DirectCheckoutColors
import com.detrapay.ui.order_details.OrderDetailsPaymentMethodPickerBottomSheet
import com.detrapay.ui.util.PaymentTypeRules
import com.detrapay.ui.home.direct_checkout.components.*
import com.detrapay.ui.home.simplified.DirectCheckoutOrderPresentation

@Composable
fun MethodScreen(
    order: Order,
    amount: Double,
    availablePaymentTypes: List<String>,
    onBack: () -> Unit,
    onSelectPaymentType: (String) -> Unit,
) {
    val available = availablePaymentTypes.map(PaymentTypeRules::normalize).toSet()
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Column(modifier = Modifier.background(Color.White)) {
                NavBar("Pedido #${order.id}", onBack)
                AmountCard("VALOR DO PAGAMENTO", DirectCheckoutOrderPresentation.formatCurrency(amount), Icons.Default.AttachMoney, DirectCheckoutColors.Blue)
            }
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Escolha a forma de pagamento", color = DirectCheckoutColors.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                PaymentMethodRow("Crédito", "Parcelado ou à vista", Icons.Default.CreditCard, DirectCheckoutColors.Blue, "credito" in available) {
                    onSelectPaymentType(OrderDetailsPaymentMethodPickerBottomSheet.TYPE_CREDIT)
                }
                PaymentMethodRow("Débito", "Pagamento imediato com taxa", Icons.Default.CreditCard, DirectCheckoutColors.Green, "debito" in available) {
                    onSelectPaymentType(OrderDetailsPaymentMethodPickerBottomSheet.TYPE_DEBIT)
                }
                PaymentMethodRow("Pix", "Recebimento rápido", Icons.Default.Bolt, DirectCheckoutColors.Teal, "pix" in available) {
                    onSelectPaymentType(OrderDetailsPaymentMethodPickerBottomSheet.TYPE_PIX)
                }
                Text("Outras formas de pagamento", modifier = Modifier.padding(top = 12.dp), color = DirectCheckoutColors.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmallMethod(Modifier.weight(1f), "Transferência\nPix", Icons.Default.Bolt, DirectCheckoutColors.Teal, "pix" in available) {
                        onSelectPaymentType(OrderDetailsPaymentMethodPickerBottomSheet.TYPE_PIX)
                    }
                    SmallMethod(Modifier.weight(1f), "Crédito Loja", Icons.Default.AccountBalance, DirectCheckoutColors.Purple, "store_credit" in available) {
                        onSelectPaymentType(OrderDetailsPaymentMethodPickerBottomSheet.TYPE_STORE_CREDIT)
                    }
                    SmallMethod(Modifier.weight(1f), "Dinheiro", Icons.Default.Payments, DirectCheckoutColors.Green, "dinheiro" in available) {
                        onSelectPaymentType(OrderDetailsPaymentMethodPickerBottomSheet.TYPE_CASH)
                    }
                }
            }
        }
    }
}
