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
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.detrapay.ui.home.orders.components.OrderFlowColors
import com.detrapay.ui.home.orders.components.*
import com.detrapay.ui.home.orders.OrderPresentation

@Composable
fun DebitScreen(amount: Double, onBack: () -> Unit, onContinue: () -> Unit) {
    val fee = OrderPresentation.debitFee(amount)
    val total = OrderPresentation.debitTotal(amount)
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Column(modifier = Modifier.background(Color.White)) {
                NavBar("Débito", onBack)
                AmountCard("VALOR DO PAGAMENTO", OrderPresentation.formatCurrency(amount), Icons.Default.CreditCard, OrderFlowColors.Green)
            }
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Resumo do débito", color = OrderFlowColors.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                PaymentMethodRow("Débito", "1x no cartão", Icons.Default.CreditCard, OrderFlowColors.Green, true, onClick = {})
                SummaryRow("Valor original", OrderPresentation.formatCurrency(amount))
                SummaryRow("Taxa de débito", OrderPresentation.formatCurrency(fee))
                SummaryRow("Total no cartão", OrderPresentation.formatCurrency(total), strong = true)
                Button(
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    onClick = onContinue,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrderFlowColors.Blue),
                ) {
                    Text("Continuar no débito", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "A taxa já está incluída no valor final exibido ao cliente.",
                    modifier = Modifier.fillMaxWidth(),
                    color = OrderFlowColors.Faint,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
