package com.detrapay.ui.home.orders.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.detrapay.data.model.remote.InstallmentFee
import com.detrapay.ui.home.orders.OrderPresentation
import com.detrapay.ui.home.orders.components.InstallmentRow
import com.detrapay.ui.home.orders.components.NavBar
import com.detrapay.ui.home.orders.components.OrderFlowFintechTheme

@Composable
fun InstallmentsScreen(
    amount: Double,
    installments: List<InstallmentFee>,
    selectedInstallment: Int?,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onSelectInstallment: (Int) -> Unit,
    onContinue: () -> Unit,
) {
    CompositionLocalProvider(
        LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = OrderFlowFintechTheme.Inter),
    ) {
        val canContinue = installments.any {
            it.installmentNumber == selectedInstallment
        }
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = OrderFlowFintechTheme.Canvas,
            topBar = {
                NavBar("Parcelamento", onBack, onClose)
            },
            bottomBar = {
                Surface(
                    color = OrderFlowFintechTheme.Card,
                    shadowElevation = 10.dp,
                ) {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .height(52.dp),
                        onClick = onContinue,
                        enabled = canContinue && !isLoading && errorMessage == null,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrderFlowFintechTheme.Primary,
                            disabledContainerColor = OrderFlowFintechTheme.Line,
                            disabledContentColor = OrderFlowFintechTheme.Muted,
                        ),
                    ) {
                        Text(
                            text = "Continuar",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            },
        ) { scaffoldPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(OrderFlowFintechTheme.Canvas)
                    .padding(scaffoldPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = OrderFlowFintechTheme.PrimarySoft,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "VALOR DO PAGAMENTO",
                                    color = OrderFlowFintechTheme.Muted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "Escolha a melhor condição",
                                    color = OrderFlowFintechTheme.PrimaryDark,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Text(
                                text = OrderPresentation.formatCurrency(amount),
                                color = OrderFlowFintechTheme.PrimaryDark,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }
                }
                item {
                    Text(
                        text = "Escolha o parcelamento",
                        color = OrderFlowFintechTheme.Ink,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                when {
                    isLoading -> item { InstallmentsLoadingCard() }
                    errorMessage != null -> item {
                        InstallmentsErrorCard(message = errorMessage, onRetry = onRetry)
                    }
                    else -> installments.forEach { installment ->
                        item(key = installment.installmentNumber) {
                            SellerInstallmentCard(
                                amount = amount,
                                installment = installment,
                                isSelected = selectedInstallment == installment.installmentNumber,
                                onClick = {
                                    onSelectInstallment(installment.installmentNumber)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstallmentsLoadingCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = OrderFlowFintechTheme.PrimarySoft,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = OrderFlowFintechTheme.Primary,
                strokeWidth = 3.dp,
            )
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(
                    text = "Buscando melhores condições",
                    color = OrderFlowFintechTheme.PrimaryDark,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Consultando as opções de parcelamento...",
                    color = OrderFlowFintechTheme.Muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun InstallmentsErrorCard(message: String, onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = OrderFlowFintechTheme.RedSoft,
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = message,
                color = OrderFlowFintechTheme.Red,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Button(
                modifier = Modifier.padding(top = 12.dp),
                onClick = onRetry,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrderFlowFintechTheme.Primary,
                ),
            ) {
                Text("Tentar novamente", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SellerInstallmentCard(
    amount: Double,
    installment: InstallmentFee,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    InstallmentRow(
        amount = amount,
        installment = installment,
        isSelected = isSelected,
        onClick = onClick,
    )
}
