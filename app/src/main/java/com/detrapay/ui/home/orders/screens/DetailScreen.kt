package com.detrapay.ui.home.orders.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.detrapay.R
import com.detrapay.data.model.Order
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.OrderReceivableItemStatus
import com.detrapay.data.model.canBeDeleted
import com.detrapay.ui.home.orders.OrderPresentation
import com.detrapay.ui.util.InstallmentQuotePresenter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DetailCanvas = Color(0xFFF6F9FD)
private val DetailCard = Color.White
private val DetailInk = Color(0xFF1A212D)
private val DetailMuted = Color(0xFF58687E)
private val DetailBorder = Color(0xFFCED5DE)
private val DetailPrimary = Color(0xFF0F64B3)
private val DetailPrimarySoft = Color(0xFFE8F1FB)
private val DetailGreen = Color(0xFF35A748)
private val DetailGreenSoft = Color(0xFFEAF7EC)
private val DetailRed = Color(0xFFC92D32)
private val DetailPendingSoft = Color(0xFFF2F4F7)
private val DetailSellerFont = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold),
    Font(R.font.inter, FontWeight.ExtraBold),
)

@Composable
fun DetailScreen(
    order: Order,
    onBack: () -> Unit,
    onPay: () -> Unit,
    onDeletePayment: (OrderReceivableItem) -> Unit,
) {
    var pendingDeletion by remember { mutableStateOf<OrderReceivableItem?>(null) }

    CompositionLocalProvider(
        LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = DetailSellerFont),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DetailCanvas),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                SellerDetailHeader(
                    orderId = order.id,
                    onBack = onBack,
                )
            }
            item {
                SellerOrderTotalsCard(
                    order = order,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item {
                SellerPaymentsCard(
                    order = order,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onAddPayment = onPay,
                    onRequestDelete = { pendingDeletion = it },
                )
            }
        }

        pendingDeletion?.let { receivable ->
            AlertDialog(
                onDismissRequest = { pendingDeletion = null },
                title = { Text("Excluir pagamento", fontWeight = FontWeight.Bold) },
                text = { Text("Deseja excluir este pagamento registrado manualmente?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pendingDeletion = null
                            onDeletePayment(receivable)
                        },
                    ) {
                        Text("Excluir", color = DetailRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeletion = null }) {
                        Text("Cancelar", color = DetailPrimary)
                    }
                },
            )
        }
    }
}

@Composable
private fun SellerDetailHeader(
    orderId: Int,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DetailCard)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Voltar",
                tint = DetailInk,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            text = "Pedido #$orderId",
            color = DetailInk,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = DetailCard,
            border = BorderStroke(1.dp, DetailBorder),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    tint = DetailInk,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "VER DETALHES",
                    color = DetailInk,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SellerOrderTotalsCard(
    order: Order,
    modifier: Modifier = Modifier,
) {
    val summary = OrderPresentation.summary(order)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = DetailCard,
        border = BorderStroke(1.dp, DetailBorder),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DetailPrimarySoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.AttachMoney,
                        contentDescription = null,
                        tint = DetailPrimary,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                ) {
                    Text(
                        text = "VALOR TOTAL DO PEDIDO",
                        color = DetailMuted,
                        fontSize = 10.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = OrderPresentation.formatCurrency(order.originalAmount),
                        color = DetailInk,
                        fontSize = 23.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copiar valor",
                    tint = DetailMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .height(1.dp)
                    .background(DetailBorder),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                SellerTotalMetric(
                    modifier = Modifier.weight(1f),
                    label = "RECEBIDO",
                    value = OrderPresentation.formatCurrency(summary.registeredAmount),
                    iconUp = true,
                    color = DetailInk,
                )
                SellerTotalMetric(
                    modifier = Modifier.weight(1f),
                    label = "FALTA",
                    value = OrderPresentation.formatCurrency(summary.missingAmount),
                    iconUp = false,
                    color = DetailRed,
                )
            }
        }
    }
}

@Composable
private fun SellerTotalMetric(
    modifier: Modifier,
    label: String,
    value: String,
    iconUp: Boolean,
    color: Color,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                if (iconUp) {
                    Icons.AutoMirrored.Filled.TrendingUp
                } else {
                    Icons.AutoMirrored.Filled.TrendingDown
                },
                contentDescription = null,
                tint = if (iconUp) DetailGreen else DetailRed,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                color = DetailMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Text(
            modifier = Modifier.padding(top = 3.dp),
            text = value,
            color = color,
            fontSize = 16.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SellerPaymentsCard(
    order: Order,
    modifier: Modifier = Modifier,
    onAddPayment: () -> Unit,
    onRequestDelete: (OrderReceivableItem) -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = DetailCard,
        border = BorderStroke(1.dp, DetailBorder),
        shadowElevation = 1.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = "Pagamentos",
                    color = DetailInk,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onAddPayment)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = DetailPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Adicionar",
                        color = DetailPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            if (order.receivables.isEmpty()) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                    text = "Nenhum pagamento registrado.",
                    color = DetailMuted,
                    fontSize = 14.sp,
                )
            } else {
                order.receivables.forEachIndexed { index, receivable ->
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(DetailBorder),
                        )
                    }
                    SellerPaymentRow(
                        receivable = receivable,
                        fallbackDate = order.creationDate.ifBlank { order.billingDate },
                        onRequestDelete = onRequestDelete,
                    )
                }
            }
        }
    }
}

@Composable
private fun SellerPaymentRow(
    receivable: OrderReceivableItem,
    fallbackDate: String,
    onRequestDelete: (OrderReceivableItem) -> Unit,
) {
    val statusLabel = OrderPresentation.receivableStatusLabel(receivable)
    val isPaid = receivable.status == OrderReceivableItemStatus.PAID
    val installment = receivable.takeIf { it.installments > 1 }?.let {
        InstallmentQuotePresenter.present(
            amountOriginal = it.amountOriginal,
            amountFinal = it.amountFinal,
            installments = it.installments,
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DetailPrimarySoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.CreditCard,
                contentDescription = null,
                tint = DetailPrimary,
                modifier = Modifier.size(25.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                text = receivable.paymentMethod.name.uppercase(Locale("pt", "BR")),
                color = DetailInk,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = sellerLongDate(receivable.paymentDate ?: fallbackDate),
                color = DetailMuted,
                fontSize = 12.sp,
            )
            Text(
                modifier = Modifier.padding(top = 2.dp),
                text = if (isPaid) {
                    "Pagamento confirmado"
                } else {
                    "Aguardando pagamento na maquininha"
                },
                color = DetailMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
            installment?.let {
                Text(
                    modifier = Modifier.padding(top = 3.dp),
                    text = it.installmentLabel,
                    color = DetailMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = OrderPresentation.formatCurrency(receivable.amountFinal),
                color = DetailInk,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
            )
            Surface(
                modifier = Modifier.padding(top = 6.dp),
                shape = CircleShape,
                color = if (isPaid) DetailGreenSoft else DetailPendingSoft,
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    text = statusLabel,
                    color = if (isPaid) DetailGreen else DetailMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (receivable.canBeDeleted()) {
                IconButton(
                    onClick = { onRequestDelete(receivable) },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Excluir pagamento",
                        tint = DetailRed,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }
    }
}

private fun sellerLongDate(value: String): String {
    return runCatching {
        LocalDate.parse(value.take(10), DateTimeFormatter.ISO_LOCAL_DATE)
            .format(DateTimeFormatter.ofPattern("dd 'de' MMM", Locale("pt", "BR")))
    }.getOrElse {
        OrderPresentation.sellerDateLabel(value)
    }
}
