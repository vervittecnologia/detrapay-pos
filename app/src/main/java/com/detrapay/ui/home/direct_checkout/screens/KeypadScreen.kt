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
fun KeypadScreen(
    order: Order,
    displayAmount: String,
    onBack: () -> Unit,
    onKey: (String) -> Unit,
    onPay: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
            NavBar("Novo pagamento", onBack)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(DirectCheckoutColors.BlueSoft, DirectCheckoutColors.IndigoSoft)))
                    .border(1.dp, DirectCheckoutColors.BlueBorder, RoundedCornerShape(18.dp))
                    .padding(vertical = 18.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("DIGITE O VALOR DO PAGAMENTO", color = DirectCheckoutColors.BlueLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(displayAmount, color = DirectCheckoutColors.Ink, fontSize = 34.sp, fontWeight = FontWeight.Black)
                    Text("Pendente do Pedido #${order.id}", color = DirectCheckoutColors.Blue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text("Teclado da maquininha", color = DirectCheckoutColors.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("Informe o valor e toque em Pagar.", color = DirectCheckoutColors.Muted, fontSize = 14.sp)
            }
            Keypad(modifier = Modifier.weight(1f), keyHeight = 58.dp, onKey = onKey)
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .height(58.dp),
                onClick = onPay,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DirectCheckoutColors.Blue),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Pagar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Escolher crÃ©dito, dÃ©bito, Pix ou outras formas", color = DirectCheckoutColors.BlueOnSoft, fontSize = 12.sp)
                }
            }
    }
}

@Composable
private fun Keypad(
    modifier: Modifier = Modifier,
    keyHeight: androidx.compose.ui.unit.Dp = 82.dp,
    onKey: (String) -> Unit,
) {
    val keys = listOf(
        "1" to "",
        "2" to "ABC",
        "3" to "DEF",
        "4" to "GHI",
        "5" to "JKL",
        "6" to "MNO",
        "7" to "PQRS",
        "8" to "TUV",
        "9" to "WXYZ",
        "," to "",
        "0" to "",
        "DEL" to "",
    )
    Column(
        modifier = modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (key, sub) ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(keyHeight)
                            .clickable { onKey(key) },
                        shape = RoundedCornerShape(18.dp),
                        color = DirectCheckoutColors.Key,
                        border = BorderStroke(1.dp, DirectCheckoutColors.Border),
                        tonalElevation = 1.dp,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            if (key == "DEL") {
                                Icon(Icons.Default.Delete, contentDescription = "Apagar", tint = DirectCheckoutColors.Muted)
                            } else {
                                Text(key, color = DirectCheckoutColors.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                if (sub.isNotBlank()) {
                                    Text(sub, color = DirectCheckoutColors.Faint, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
