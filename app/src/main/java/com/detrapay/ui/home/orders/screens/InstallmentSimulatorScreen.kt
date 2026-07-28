package com.detrapay.ui.home.orders.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.detrapay.data.model.remote.InstallmentFee
import com.detrapay.ui.home.orders.components.OrderFlowColors
import com.detrapay.ui.home.orders.components.*
import com.detrapay.ui.home.orders.OrderPresentation

@Composable
fun InstallmentSimulatorScreen(
    amountDigits: String,
    installments: List<InstallmentFee>,
    selectedInstallment: Int?,
    isLoading: Boolean,
    errorMessage: String?,
    onClose: () -> Unit,
    onAmountChange: (String) -> Unit,
    onConsult: () -> Unit,
    onSelectInstallment: (Int) -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
) {
    val amount = OrderPresentation.currencyInputAmount(amountDigits)
    val formattedAmount = OrderPresentation.formatCurrencyInput(amountDigits)
    val shareText = simulatorShareText(amount, installments)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, OrderFlowColors.Border)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = OrderFlowColors.Ink)
            }
            Text(
                modifier = Modifier.weight(1f),
                text = "Simulação de Crédito",
                color = OrderFlowColors.Ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onClose, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = OrderFlowColors.Text)
            }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OrderFlowColors.MutedSurface.copy(alpha = 0.55f))
                        .border(0.5.dp, OrderFlowColors.Border)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MethodIcon(Icons.Default.CreditCard, OrderFlowColors.Blue)
                    Column(modifier = Modifier.padding(start = 14.dp)) {
                        Text("Crédito", color = OrderFlowColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Simular parcelamento em até 18x", color = OrderFlowColors.Muted, fontSize = 13.sp)
                    }
                }

                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("Valor", color = OrderFlowColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = formattedAmount,
                        onValueChange = onAmountChange,
                        leadingIcon = { Text("R$", color = OrderFlowColors.Muted, fontWeight = FontWeight.Medium) },
                        placeholder = { Text("0,00") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                    )

                    when {
                        amount > 0.0 && installments.isEmpty() && isLoading -> LoadingBlock("Consultando parcelamentos...")
                        errorMessage != null -> Text(errorMessage, color = OrderFlowColors.Red, fontWeight = FontWeight.SemiBold)
                        amount > 0.0 && installments.isEmpty() -> Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            onClick = onConsult,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OrderFlowColors.Blue.copy(alpha = 0.10f),
                                contentColor = OrderFlowColors.Blue,
                            ),
                        ) {
                            Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Consultar Parcelas", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (installments.isNotEmpty()) {
                        Text("Parcelas", color = OrderFlowColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        installments.forEach { installment ->
                            SimulatorInstallmentRow(
                                installment = installment,
                                isSelected = selectedInstallment == installment.installmentNumber,
                                onClick = { onSelectInstallment(installment.installmentNumber) },
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, OrderFlowColors.Border)
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (installments.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        onClick = { onCopy(shareText) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrderFlowColors.MutedSurface,
                            contentColor = OrderFlowColors.Ink,
                        ),
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copiar", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        onClick = { onShare(shareText) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrderFlowColors.WhatsappGreen),
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("WhatsApp", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Simulação - nenhum dado será salvo",
                color = OrderFlowColors.Muted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
@Composable
private fun SimulatorInstallmentRow(installment: InstallmentFee, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) OrderFlowColors.Blue.copy(alpha = 0.05f) else Color.White)
            .border(1.dp, if (isSelected) OrderFlowColors.Blue else OrderFlowColors.Border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = "${installment.installmentNumber}x de ${currencyText(installment.installmentValue)}${if (installment.noInterest) " sem juros" else ""}",
            color = if (isSelected) OrderFlowColors.Blue else OrderFlowColors.Ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "Total ${currencyText(installment.totalValue)}",
            color = if (isSelected) OrderFlowColors.Blue.copy(alpha = 0.72f) else OrderFlowColors.Muted,
            fontSize = 12.sp,
        )
    }
}
