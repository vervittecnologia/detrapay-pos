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
                PaymentMethodRow("CrÃ©dito", "Parcelado ou Ã  vista", Icons.Default.CreditCard, DirectCheckoutColors.Blue, "credito" in available) {
                    onSelectPaymentType(OrderDetailsPaymentMethodPickerBottomSheet.TYPE_CREDIT)
                }
                PaymentMethodRow("DÃ©bito", "Pagamento imediato com taxa", Icons.Default.CreditCard, DirectCheckoutColors.Green, "debito" in available) {
                    onSelectPaymentType(OrderDetailsPaymentMethodPickerBottomSheet.TYPE_DEBIT)
                }
                PaymentMethodRow("Pix", "Recebimento rÃ¡pido", Icons.Default.Bolt, DirectCheckoutColors.Teal, "pix" in available) {
                    onSelectPaymentType(OrderDetailsPaymentMethodPickerBottomSheet.TYPE_PIX)
                }
                Text("Outras formas de pagamento", modifier = Modifier.padding(top = 12.dp), color = DirectCheckoutColors.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmallMethod(Modifier.weight(1f), "TransferÃªncia\nPix", Icons.Default.Bolt, DirectCheckoutColors.Teal, "pix" in available) {
                        onSelectPaymentType(OrderDetailsPaymentMethodPickerBottomSheet.TYPE_PIX)
                    }
                    SmallMethod(Modifier.weight(1f), "CrÃ©dito Loja", Icons.Default.AccountBalance, DirectCheckoutColors.Purple, "store_credit" in available) {
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
