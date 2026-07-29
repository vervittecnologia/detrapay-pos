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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.detrapay.data.model.remote.InstallmentFee
import com.detrapay.ui.home.orders.OrderPresentation
import com.detrapay.ui.home.orders.components.AmountCard
import com.detrapay.ui.home.orders.components.InstallmentRow
import com.detrapay.ui.home.orders.components.NavBar
import com.detrapay.ui.home.orders.components.OrderFlowColors
import com.detrapay.ui.home.orders.components.OrderFlowFintechTheme

@Composable
fun InstallmentsScreen(
    amount: Double,
    installments: List<InstallmentFee>,
    selectedInstallment: Int?,
    onBack: () -> Unit,
    onSelectInstallment: (Int) -> Unit,
    onContinue: () -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize().background(OrderFlowFintechTheme.Canvas)) {
        item {
            Column(modifier = Modifier.background(OrderFlowFintechTheme.Canvas)) {
                NavBar("Parcelamento", onBack)
                AmountCard(
                    "VALOR ORIGINAL",
                    OrderPresentation.formatCurrency(amount),
                    Icons.Default.CreditCard,
                    OrderFlowColors.Blue,
                )
            }
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Escolha o parcelamento",
                    color = OrderFlowFintechTheme.Ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                installments.forEach { installment ->
                    InstallmentRow(
                        amount = amount,
                        installment = installment,
                        isSelected = selectedInstallment == installment.installmentNumber,
                        onClick = { onSelectInstallment(installment.installmentNumber) },
                    )
                }
                Button(
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    onClick = onContinue,
                    enabled = installments.any { it.installmentNumber == selectedInstallment },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrderFlowFintechTheme.Primary),
                ) {
                    Text("Revisar pagamento", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
