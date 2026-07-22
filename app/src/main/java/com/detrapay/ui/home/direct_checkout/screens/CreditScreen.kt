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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.detrapay.data.model.remote.InstallmentFee
import com.detrapay.ui.home.direct_checkout.components.DirectCheckoutColors
import com.detrapay.ui.home.direct_checkout.components.*
import com.detrapay.ui.home.simplified.DirectCheckoutOrderPresentation

@Composable
fun CreditScreen(
    amount: Double,
    installments: List<InstallmentFee>,
    selectedInstallment: Int,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onSelectInstallment: (Int) -> Unit,
    onContinue: () -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Column(modifier = Modifier.background(Color.White)) {
                NavBar("Crédito", onBack)
                AmountCard("VALOR DO PAGAMENTO", DirectCheckoutOrderPresentation.formatCurrency(amount), Icons.Default.CreditCard, DirectCheckoutColors.Blue)
            }
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Escolha o parcelamento", color = DirectCheckoutColors.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                when {
                    isLoading -> LoadingBlock("Calculando parcelas...")
                    errorMessage != null -> Text(errorMessage, color = DirectCheckoutColors.AmberText, fontWeight = FontWeight.Bold)
                    installments.isEmpty() -> Text("Nenhuma parcela disponível.", color = DirectCheckoutColors.Muted)
                    else -> installments.forEach { installment ->
                        InstallmentRow(
                            installment = installment,
                            isSelected = selectedInstallment == installment.installmentNumber,
                            onClick = { onSelectInstallment(installment.installmentNumber) },
                        )
                    }
                }
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    onClick = onContinue,
                    enabled = installments.isNotEmpty() && !isLoading,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DirectCheckoutColors.Blue),
                ) {
                    Text("Continuar no crédito", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
