package com.detrapay.ui.home.orders.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.detrapay.data.model.remote.InstallmentFee
import com.detrapay.ui.home.orders.components.OrderFlowColors
import com.detrapay.ui.home.orders.components.*
import com.detrapay.ui.home.orders.OrderPresentation
import com.detrapay.ui.util.InstallmentQuotePresenter

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
    val selected = installments.firstOrNull { it.installmentNumber == selectedInstallment }
    val shareText = selected?.let { simulatorShareText(amount, it) }.orEmpty()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

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
            Spacer(modifier = Modifier.size(44.dp))
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
                        placeholder = { Text("0,00") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            },
                        ),
                    )

                    when {
                        amount > 0.0 && installments.isEmpty() && isLoading -> LoadingBlock("Consultando parcelamentos...")
                        errorMessage != null -> Text(errorMessage, color = OrderFlowColors.Red, fontWeight = FontWeight.SemiBold)
                        amount > 0.0 && installments.isEmpty() -> Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                onConsult()
                            },
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
                                amount = amount,
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
                        enabled = selected != null,
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
                        enabled = selected != null,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrderFlowColors.WhatsappGreen),
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("WhatsApp", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (selected == null) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Selecione uma opção para compartilhar",
                        color = OrderFlowColors.Muted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
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
private fun SimulatorInstallmentRow(
    amount: Double,
    installment: InstallmentFee,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val presentation = InstallmentQuotePresenter.present(amount, installment)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) OrderFlowColors.Blue.copy(alpha = 0.05f) else Color.White)
            .border(1.dp, if (isSelected) OrderFlowColors.Blue else OrderFlowColors.Border, RoundedCornerShape(12.dp))
            .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics {
                stateDescription = if (isSelected) "Selecionado" else "Não selecionado"
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = presentation.installmentLabel,
                color = if (isSelected) OrderFlowColors.Blue else OrderFlowColors.Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(presentation.originalLabel, color = OrderFlowColors.Muted, fontSize = 12.sp)
            Text(presentation.totalLabel, color = OrderFlowColors.Muted, fontSize = 12.sp)
        }
    }
}
