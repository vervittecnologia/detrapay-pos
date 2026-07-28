package com.detrapay.ui.home.orders.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.detrapay.data.model.Order
import com.detrapay.ui.home.orders.components.OrderFlowColors
import com.detrapay.ui.home.orders.components.*

@Composable
fun KeypadScreen(
    order: Order,
    paymentMethodName: String,
    displayAmount: String,
    pendingAmountLabel: String,
    canPay: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onKey: (String) -> Unit,
    onUsePendingAmount: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
            NavBar("Novo pagamento", onBack)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(OrderFlowColors.BlueSoft, OrderFlowColors.IndigoSoft)))
                    .border(1.dp, OrderFlowColors.BlueBorder, RoundedCornerShape(18.dp))
                    .padding(vertical = 18.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("DIGITE O VALOR DO PAGAMENTO", color = OrderFlowColors.BlueLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(displayAmount, color = OrderFlowColors.Ink, fontSize = 34.sp, fontWeight = FontWeight.Black)
                    Text("Pedido #${order.id}", color = OrderFlowColors.Blue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Forma: $paymentMethodName", color = OrderFlowColors.Blue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Valor pendente: $pendingAmountLabel", color = OrderFlowColors.Muted, fontSize = 13.sp)
                    OutlinedButton(onClick = onUsePendingAmount) {
                        Text("Usar valor pendente")
                    }
                }
            }
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text("Teclado da maquininha", color = OrderFlowColors.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("Informe o valor e toque em Continuar.", color = OrderFlowColors.Muted, fontSize = 14.sp)
                if (isLoading) {
                    Text("Calculando valor final...", color = OrderFlowColors.Blue, fontSize = 14.sp)
                } else if (errorMessage != null) {
                    Text(errorMessage, color = OrderFlowColors.AmberText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Keypad(modifier = Modifier.weight(1f), keyHeight = 58.dp, onKey = onKey)
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .height(58.dp),
                onClick = onContinue,
                enabled = canPay && !isLoading,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrderFlowColors.Blue),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Continuar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Revisar as condições do pagamento", color = OrderFlowColors.BlueOnSoft, fontSize = 12.sp)
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
                        color = OrderFlowColors.Key,
                        border = BorderStroke(1.dp, OrderFlowColors.Border),
                        tonalElevation = 1.dp,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            if (key == "DEL") {
                                Icon(Icons.Default.Delete, contentDescription = "Apagar", tint = OrderFlowColors.Muted)
                            } else {
                                Text(key, color = OrderFlowColors.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                if (sub.isNotBlank()) {
                                    Text(sub, color = OrderFlowColors.Faint, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
