package com.detrapay.ui.home.direct_checkout.screens

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
import com.detrapay.ui.home.direct_checkout.components.DirectCheckoutColors
import com.detrapay.ui.home.direct_checkout.components.*
import com.detrapay.ui.home.simplified.DirectCheckoutOrderPresentation

@Composable
fun DebitScreen(amount: Double, onBack: () -> Unit, onContinue: () -> Unit) {
    val fee = DirectCheckoutOrderPresentation.debitFee(amount)
    val total = DirectCheckoutOrderPresentation.debitTotal(amount)
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Column(modifier = Modifier.background(Color.White)) {
                NavBar("Débito", onBack)
                AmountCard("VALOR DO PAGAMENTO", DirectCheckoutOrderPresentation.formatCurrency(amount), Icons.Default.CreditCard, DirectCheckoutColors.Green)
            }
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Resumo do débito", color = DirectCheckoutColors.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                PaymentMethodRow("Débito", "1x no cartão", Icons.Default.CreditCard, DirectCheckoutColors.Green, true, onClick = {})
                SummaryRow("Valor original", DirectCheckoutOrderPresentation.formatCurrency(amount))
                SummaryRow("Taxa de débito", DirectCheckoutOrderPresentation.formatCurrency(fee))
                SummaryRow("Total no cartão", DirectCheckoutOrderPresentation.formatCurrency(total), strong = true)
                Button(
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    onClick = onContinue,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DirectCheckoutColors.Blue),
                ) {
                    Text("Continuar no débito", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "A taxa já está incluída no valor final exibido ao cliente.",
                    modifier = Modifier.fillMaxWidth(),
                    color = DirectCheckoutColors.Faint,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
