package com.detrapay.ui.home.simplified

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

enum class DirectCheckoutStep {
    Orders,
    Detail,
    Keypad,
    Method,
    Credit,
    Debit,
    Waiting,
}

@Composable
fun DirectCheckoutFlowScreen(
    companyName: String,
    companyDocument: String,
    orders: List<Order>,
    isLoading: Boolean,
    errorMessage: String?,
    availablePaymentTypes: List<String>,
    step: DirectCheckoutStep,
    selectedOrder: Order?,
    paymentDigits: String,
    selectedPaymentType: String,
    creditInstallments: List<InstallmentFee>,
    selectedInstallment: Int,
    isFeesLoading: Boolean,
    feesError: String?,
    showSimulator: Boolean,
    simulatorAmountDigits: String,
    simulatorInstallments: List<InstallmentFee>,
    simulatorSelectedInstallment: Int?,
    isSimulatorLoading: Boolean,
    simulatorError: String?,
    onLogout: () -> Unit,
    onReload: () -> Unit,
    onNewOrder: () -> Unit,
    onOrderPay: (Order) -> Unit,
    onOrderDetail: (Order) -> Unit,
    onBack: () -> Unit,
    onKey: (String) -> Unit,
    onOpenMethods: () -> Unit,
    onSelectPaymentType: (String) -> Unit,
    onSelectInstallment: (Int) -> Unit,
    onContinueCredit: () -> Unit,
    onContinueDebit: () -> Unit,
    onOpenSimulator: () -> Unit,
    onCloseSimulator: () -> Unit,
    onSimulatorAmountChange: (String) -> Unit,
    onConsultSimulator: () -> Unit,
    onSelectSimulatorInstallment: (Int) -> Unit,
    onCopySimulator: (String) -> Unit,
    onShareSimulator: (String) -> Unit,
) {
    val currentOrder = selectedOrder
    val pendingAmount = currentOrder?.let { DirectCheckoutOrderPresentation.summary(it).missingAmount } ?: 0.0
    val amount = DirectCheckoutOrderPresentation.paymentAmount(paymentDigits, pendingAmount)

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DirectCheckoutColors.Background,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (step) {
                    DirectCheckoutStep.Orders -> SellerOrdersScreen(
                        orders = orders,
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        onReload = onReload,
                        onNewOrder = onNewOrder,
                        onOpenSimulator = onOpenSimulator,
                        onOrderPay = onOrderPay,
                        onOrderDetail = onOrderDetail,
                    )
                    DirectCheckoutStep.Detail -> if (currentOrder != null) {
                        DetailScreen(
                            order = currentOrder,
                            onBack = onBack,
                            onPay = { onOrderPay(currentOrder) },
                        )
                    }
                    DirectCheckoutStep.Keypad -> if (currentOrder != null) {
                        KeypadScreen(
                            order = currentOrder,
                            displayAmount = DirectCheckoutOrderPresentation.paymentDisplayAmount(
                                paymentDigits,
                                pendingAmount,
                            ),
                            onBack = onBack,
                            onKey = onKey,
                            onPay = onOpenMethods,
                        )
                    }
                    DirectCheckoutStep.Method -> if (currentOrder != null) {
                        MethodScreen(
                            order = currentOrder,
                            amount = amount,
                            availablePaymentTypes = availablePaymentTypes,
                            onBack = onBack,
                            onSelectPaymentType = onSelectPaymentType,
                        )
                    }
                    DirectCheckoutStep.Credit -> CreditScreen(
                        amount = amount,
                        installments = creditInstallments,
                        selectedInstallment = selectedInstallment,
                        isLoading = isFeesLoading,
                        errorMessage = feesError,
                        onBack = onBack,
                        onSelectInstallment = onSelectInstallment,
                        onContinue = onContinueCredit,
                    )
                    DirectCheckoutStep.Debit -> DebitScreen(
                        amount = amount,
                        onBack = onBack,
                        onContinue = onContinueDebit,
                    )
                    DirectCheckoutStep.Waiting -> WaitingScreen(
                        total = amount,
                        paymentType = selectedPaymentType,
                        onBack = onBack,
                    )
                }
                if (showSimulator) {
                    InstallmentSimulatorScreen(
                        amountDigits = simulatorAmountDigits,
                        installments = simulatorInstallments,
                        selectedInstallment = simulatorSelectedInstallment,
                        isLoading = isSimulatorLoading,
                        errorMessage = simulatorError,
                        onClose = onCloseSimulator,
                        onAmountChange = onSimulatorAmountChange,
                        onConsult = onConsultSimulator,
                        onSelectInstallment = onSelectSimulatorInstallment,
                        onCopy = onCopySimulator,
                        onShare = onShareSimulator,
                    )
                }
            }
        }
    }
}

@Composable
internal fun SellerOrdersScreen(
    orders: List<Order>,
    isLoading: Boolean,
    errorMessage: String?,
    onReload: () -> Unit,
    onNewOrder: () -> Unit,
    onOpenSimulator: () -> Unit,
    onOrderPay: (Order) -> Unit,
    onOrderDetail: (Order) -> Unit,
    initialShowSearch: Boolean = false,
    initialQuery: String = "",
    initialShowFabMenu: Boolean = false,
) {
    var query by remember { mutableStateOf(initialQuery) }
    var showSearch by remember { mutableStateOf(initialShowSearch || initialQuery.isNotBlank()) }
    var showFabMenu by remember { mutableStateOf(initialShowFabMenu) }
    val filtered = remember(query, orders) {
        val digits = query.filter(Char::isDigit)
        orders.filter { order ->
            query.isBlank() ||
                order.id.toString().contains(query, ignoreCase = true) ||
                order.customer.name.contains(query, ignoreCase = true) ||
                (digits.isNotBlank() && order.customer.cpfCnpj.contains(digits))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = "Pedidos",
                            color = DirectCheckoutColors.Ink,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                        )
                        IconButton(
                            onClick = {
                                showSearch = !showSearch
                                if (!showSearch) query = ""
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(DirectCheckoutColors.MutedSurface),
                        ) {
                            Icon(
                                if (showSearch) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = if (showSearch) "Fechar busca" else "Buscar pedidos",
                                tint = DirectCheckoutColors.Ink,
                                modifier = Modifier.size(23.dp),
                            )
                        }
                    }

                    if (showSearch) {
                        OutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            value = query,
                            onValueChange = { query = it },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DirectCheckoutColors.Muted) },
                            trailingIcon = if (query.isNotBlank()) {
                                {
                                    IconButton(onClick = { query = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Limpar busca", modifier = Modifier.size(14.dp))
                                    }
                                }
                            } else {
                                null
                            },
                            placeholder = { Text("Buscar por cliente, CPF ou nº pedido...") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                        )
                    }
                }
            }

            when {
                isLoading -> item { LoadingBlock("Carregando pedidos...") }
                errorMessage != null -> item {
                    EmptyBlock(
                        title = "Não foi possível carregar os pedidos.",
                        subtitle = errorMessage,
                        actionText = "Recarregar",
                        onAction = onReload,
                    )
                }
                filtered.isEmpty() -> item {
                    EmptyBlock(
                        title = "Nenhum pedido encontrado",
                        subtitle = if (query.isBlank()) "Nenhum pedido ainda" else "Tente buscar com outros termos",
                        actionText = if (query.isBlank()) "Recarregar" else "Limpar filtros",
                        onAction = {
                            if (query.isBlank()) onReload() else query = ""
                        },
                    )
                }
                else -> {
                    if (query.isNotBlank()) {
                        item {
                            Text(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                text = "${filtered.size} resultado${if (filtered.size == 1) "" else "s"} para \"$query\"",
                                color = DirectCheckoutColors.Muted,
                                fontSize = 12.sp,
                            )
                        }
                    }
                    items(filtered, key = { it.id }) { order ->
                        SellerOrderCard(
                            order = order,
                            onClick = { onOrderDetail(order) },
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(112.dp)) }
        }

        if (showFabMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.20f))
                    .clickable { showFabMenu = false },
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 152.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End,
            ) {
                FabMenuButton("Novo Pedido", Icons.Default.Receipt) {
                    showFabMenu = false
                    onNewOrder()
                }
                FabMenuButton("Simular Parcelas", Icons.Default.CreditCard) {
                    showFabMenu = false
                    onOpenSimulator()
                }
            }
        }

        Button(
            onClick = { showFabMenu = !showFabMenu },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 22.dp, bottom = 96.dp)
                .size(72.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = DirectCheckoutColors.Blue),
            contentPadding = PaddingValues(0.dp),
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Abrir ações",
                modifier = Modifier
                    .size(36.dp)
                    .graphicsLayer(rotationZ = if (showFabMenu) 45f else 0f),
            )
        }
    }
}

@Composable
private fun FabMenuButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .width(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, DirectCheckoutColors.Border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(DirectCheckoutColors.Blue.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = DirectCheckoutColors.Blue, modifier = Modifier.size(18.dp))
        }
        Text(
            modifier = Modifier.padding(start = 12.dp),
            text = label,
            color = DirectCheckoutColors.Ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SellerOrderCard(order: Order, onClick: () -> Unit) {
    val card = DirectCheckoutOrderPresentation.sellerCardSummary(order)
    val progress = card.progressPercent.coerceIn(0, 100) / 100f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, DirectCheckoutColors.Border),
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "#${order.id}",
                        color = DirectCheckoutColors.Blue,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Row(
                        modifier = Modifier.padding(start = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = DirectCheckoutColors.Muted, modifier = Modifier.size(18.dp))
                        Text(
                            modifier = Modifier.padding(start = 5.dp),
                            text = DirectCheckoutOrderPresentation.sellerDateLabel(order.creationDate.ifBlank { order.billingDate }),
                            color = DirectCheckoutColors.Muted,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                SellerStatusBadge(order.status.name.lowercase())
            }

            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = order.customer.name.ifBlank { order.customer.cpfCnpj.ifBlank { "-" } },
                color = DirectCheckoutColors.Ink,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(1.dp)
                    .background(DirectCheckoutColors.Border),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                SellerMetric("Total", card.totalLabel, DirectCheckoutColors.Ink, Modifier.weight(1f))
                VerticalMetricDivider()
                SellerMetric("Pago", card.paidLabel, if (card.isFullyPaid) DirectCheckoutColors.Green else DirectCheckoutColors.Ink, Modifier.weight(1f))
                VerticalMetricDivider()
                SellerMetric(
                    card.balanceTitle,
                    card.balanceLabel,
                    if (card.balanceTitle == "Falta" && !card.isFullyPaid) DirectCheckoutColors.Red else DirectCheckoutColors.Ink,
                    Modifier.weight(1f),
                    showFallingIcon = card.balanceTitle == "Falta" && !card.isFullyPaid,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(DirectCheckoutColors.Track),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (card.isFullyPaid) DirectCheckoutColors.Green else DirectCheckoutColors.Warning),
                )
            }
        }
    }
}

@Composable
private fun SellerStatusBadge(status: String) {
    val label = when (status) {
        "paid" -> "Pago"
        "authorized" -> "Autorizado"
        "completed" -> "Concluído"
        "cancelled" -> "Cancelado"
        else -> "Pendente Vendedor"
    }
    val bg = when (status) {
        "paid", "authorized", "completed" -> DirectCheckoutColors.GreenSoft
        "cancelled" -> DirectCheckoutColors.RedSoft
        else -> DirectCheckoutColors.WarningSoft
    }
    val fg = when (status) {
        "paid", "authorized", "completed" -> DirectCheckoutColors.Green
        "cancelled" -> DirectCheckoutColors.Red
        else -> DirectCheckoutColors.WarningText
    }

    Row(
        modifier = Modifier
            .widthIn(min = 164.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Receipt, contentDescription = null, tint = fg, modifier = Modifier.size(15.dp))
        Text(
            modifier = Modifier.padding(start = 6.dp),
            text = label.uppercase(),
            color = fg,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SellerMetric(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    showFallingIcon: Boolean = false,
) {
    Column(modifier = modifier.padding(horizontal = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showFallingIcon) {
                Icon(
                    Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = DirectCheckoutColors.Red,
                    modifier = Modifier
                        .padding(end = 2.dp)
                        .size(14.dp),
                )
            }
            Text(
                text = label.uppercase(),
                color = DirectCheckoutColors.Muted.copy(alpha = 0.92f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = value,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun VerticalMetricDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(52.dp)
            .background(DirectCheckoutColors.Border),
    )
}

@Composable
private fun OrdersScreen(
    companyName: String,
    companyDocument: String,
    orders: List<Order>,
    isLoading: Boolean,
    errorMessage: String?,
    onLogout: () -> Unit,
    onReload: () -> Unit,
    onNewOrder: () -> Unit,
    onOrderPay: (Order) -> Unit,
    onOrderDetail: (Order) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, orders) {
        val digits = query.filter(Char::isDigit)
        orders.filter { order ->
            query.isBlank() ||
                order.id.toString().contains(query, ignoreCase = true) ||
                order.customer.name.contains(query, ignoreCase = true) ||
                (digits.isNotBlank() && order.customer.cpfCnpj.contains(digits))
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item {
            DealershipHeaderV10(
                companyName = companyName,
                onLogout = onLogout,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Pedidos",
                    color = DirectCheckoutColors.Ink,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(DirectCheckoutColors.BlueSoft)
                        .border(1.dp, DirectCheckoutColors.BlueBorder, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    text = "Abertos",
                    color = DirectCheckoutColors.Blue,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = onNewOrder,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DirectCheckoutColors.Blue),
                    contentPadding = ButtonDefaults.ContentPadding,
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Novo Pedido", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text("Buscar por número ou cliente") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
            }
        }

        if (isLoading) {
            item { LoadingBlock("Carregando pedidos abertos...") }
        } else if (errorMessage != null) {
            item {
                EmptyBlock(
                    title = "Não foi possível carregar os pedidos.",
                    subtitle = errorMessage,
                    actionText = "Recarregar",
                    onAction = onReload,
                )
            }
        } else if (filtered.isEmpty()) {
            item {
                EmptyBlock(
                    title = "Nenhum pedido aberto",
                    subtitle = "Quando houver pedidos com saldo pendente, eles aparecerão aqui.",
                    actionText = "Recarregar",
                    onAction = onReload,
                )
            }
        } else {
            items(filtered, key = { it.id }) { order ->
                OrderCard(
                    order = order,
                    onPay = { onOrderPay(order) },
                    onDetail = { onOrderDetail(order) },
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun DealershipHeaderV10(companyName: String, onLogout: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = companyName,
            color = DirectCheckoutColors.Faint,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onLogout)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, tint = DirectCheckoutColors.Pale, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Sair", color = DirectCheckoutColors.Pale, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun DealershipHeader(companyName: String, companyDocument: String, onLogout: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DirectCheckoutColors.Blue)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "CONCESSIONÁRIA",
                color = DirectCheckoutColors.BlueOnSoft,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                companyName,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                companyDocument,
                color = DirectCheckoutColors.BlueOnSoft,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.10f))
                .clickable(onClick = onLogout)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Sair", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OrderCard(order: Order, onPay: () -> Unit, onDetail: () -> Unit) {
    val summary = DirectCheckoutOrderPresentation.summary(order)
    val status = DirectCheckoutOrderPresentation.statusLabel(order)
    val percent = DirectCheckoutOrderPresentation.paidPercent(order)
    val isPending = status == "Pendente"
    val startsPayment = DirectCheckoutOrderPresentation.shouldStartPayment(order)
    val primaryActionLabel = DirectCheckoutOrderPresentation.primaryActionLabel(order)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(if (isPending) DirectCheckoutColors.Amber else DirectCheckoutColors.BlueLight),
        )
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pedido #${order.id}", color = DirectCheckoutColors.Ink, fontSize = 23.sp, fontWeight = FontWeight.Black)
                    Text(order.customer.name.ifBlank { "Cliente" }, color = DirectCheckoutColors.Text, fontSize = 18.sp)
                    Text(displayDate(order.billingDate), color = DirectCheckoutColors.Muted, fontSize = 15.sp)
                }
                Text(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isPending) DirectCheckoutColors.AmberSoft else DirectCheckoutColors.BlueSoft)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    text = status,
                    color = if (isPending) DirectCheckoutColors.AmberText else DirectCheckoutColors.Blue,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Metric("TOTAL", DirectCheckoutOrderPresentation.formatCurrency(order.originalAmount), DirectCheckoutColors.Text)
                Metric(
                    "PENDENTE",
                    DirectCheckoutOrderPresentation.formatCurrency(summary.missingAmount),
                    if (isPending) DirectCheckoutColors.AmberText else DirectCheckoutColors.Blue,
                )
                Spacer(modifier = Modifier.weight(1f))
                Metric("PAGO", "$percent%", DirectCheckoutColors.Green, alignEnd = true)
            }
            if (percent > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(DirectCheckoutColors.Track),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(percent / 100f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Brush.horizontalGradient(listOf(DirectCheckoutColors.BlueLight, DirectCheckoutColors.Blue))),
                    )
                }
            }
            Row(
                modifier = Modifier.padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp),
                    onClick = if (startsPayment) onPay else onDetail,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DirectCheckoutColors.Blue),
                ) {
                    Text(primaryActionLabel, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                if (startsPayment) {
                    Button(
                        modifier = Modifier.height(58.dp),
                        onClick = onDetail,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DirectCheckoutColors.BlueSoft,
                            contentColor = DirectCheckoutColors.Blue,
                        ),
                    ) {
                        Text("Ver detalhes", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailScreen(order: Order, onBack: () -> Unit, onPay: () -> Unit) {
    val summary = DirectCheckoutOrderPresentation.summary(order)
    val status = DirectCheckoutOrderPresentation.statusLabel(order)
    val percent = DirectCheckoutOrderPresentation.paidPercent(order)
    val isPending = status == "Pendente"

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Column(modifier = Modifier.background(Color.White)) {
                NavBar("Pedido #${order.id}", onBack)
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DirectCheckoutColors.Key)
                        .border(1.dp, DirectCheckoutColors.Border, RoundedCornerShape(14.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(if (isPending) DirectCheckoutColors.Amber else DirectCheckoutColors.BlueLight),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (isPending) DirectCheckoutColors.AmberSoft else DirectCheckoutColors.BlueSoft)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            text = status,
                            color = if (isPending) DirectCheckoutColors.AmberText else DirectCheckoutColors.Blue,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(displayDate(order.billingDate), color = DirectCheckoutColors.Muted, fontSize = 16.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        DetailCell("Cliente", order.customer.name.ifBlank { "Cliente" }, Modifier.weight(1f))
                        DetailCell("Vendedor", order.salesman?.name ?: "-", Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        DetailCell("WhatsApp", order.customer.phoneNumber.ifBlank { "-" }, Modifier.weight(1f))
                        DetailCell("Faturamento", displayDate(order.billingDate), Modifier.weight(1f))
                    }
                    DetailCell("CPF / CNPJ", order.customer.cpfCnpj.ifBlank { "-" }, Modifier.fillMaxWidth())
                }
            }

            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Resumo financeiro", color = DirectCheckoutColors.Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, DirectCheckoutColors.Border),
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        MetricCell("TOTAL", DirectCheckoutOrderPresentation.formatCurrency(order.originalAmount), DirectCheckoutColors.Ink, Modifier.weight(1f))
                        MetricCell("PAGO", DirectCheckoutOrderPresentation.formatCurrency(summary.registeredAmount), DirectCheckoutColors.Green, Modifier.weight(1f))
                        MetricCell("PENDENTE", DirectCheckoutOrderPresentation.formatCurrency(summary.missingAmount), if (isPending) DirectCheckoutColors.AmberText else DirectCheckoutColors.Blue, Modifier.weight(1f))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(DirectCheckoutColors.Track),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(percent / 100f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFF34D399), DirectCheckoutColors.Green))),
                        )
                    }
                    Text(
                        "$percent% quitado",
                        modifier = Modifier.padding(start = 18.dp, top = 8.dp, bottom = 16.dp),
                        color = DirectCheckoutColors.Muted,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pagamentos registrados", modifier = Modifier.weight(1f), color = DirectCheckoutColors.Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text(
                        order.receivables.size.toString(),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(DirectCheckoutColors.Track)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        color = DirectCheckoutColors.Muted,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (order.receivables.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White)
                            .border(1.dp, DirectCheckoutColors.Border, RoundedCornerShape(18.dp))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = DirectCheckoutColors.Pale, modifier = Modifier.size(30.dp))
                        Text("Nenhum pagamento registrado", modifier = Modifier.padding(top = 8.dp), color = DirectCheckoutColors.Muted, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    order.receivables.forEach { receivable ->
                        SummaryRow(
                            receivable.paymentMethod.name,
                            DirectCheckoutOrderPresentation.formatCurrency(receivable.amountFinal),
                            subtitle = DirectCheckoutOrderPresentation.receivableStatusLabel(receivable),
                        )
                    }
                }
                if (summary.hasPendingBalance) {
                    Button(
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        onClick = onPay,
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DirectCheckoutColors.Blue),
                    ) {
                        Text("Pagar ${DirectCheckoutOrderPresentation.formatCurrency(summary.missingAmount)}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadScreen(
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
                    Text("Escolher crédito, débito, Pix ou outras formas", color = DirectCheckoutColors.BlueOnSoft, fontSize = 12.sp)
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

@Composable
private fun MethodScreen(
    order: Order,
    amount: Double,
    availablePaymentTypes: List<String>,
    onBack: () -> Unit,
    onSelectPaymentType: (String) -> Unit,
) {
    val available = availablePaymentTypes.map(PaymentTypeRules::normalize).toSet()
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Column(modifier = Modifier.background(Color.White)) {
                NavBar("Pedido #${order.id}", onBack)
                AmountCard("VALOR DO PAGAMENTO", DirectCheckoutOrderPresentation.formatCurrency(amount), Icons.Default.AttachMoney, DirectCheckoutColors.Blue)
            }
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Escolha a forma de pagamento", color = DirectCheckoutColors.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                PaymentMethodRow("Crédito", "Parcelado ou à vista", Icons.Default.CreditCard, DirectCheckoutColors.Blue, "credito" in available) {
                    onSelectPaymentType(OrderDetailsPaymentMethodPickerBottomSheet.TYPE_CREDIT)
                }
                PaymentMethodRow("Débito", "Pagamento imediato com taxa", Icons.Default.CreditCard, DirectCheckoutColors.Green, "debito" in available) {
                    onSelectPaymentType(OrderDetailsPaymentMethodPickerBottomSheet.TYPE_DEBIT)
                }
                PaymentMethodRow("Pix", "Recebimento rápido", Icons.Default.Bolt, DirectCheckoutColors.Teal, "pix" in available) {
                    onSelectPaymentType(OrderDetailsPaymentMethodPickerBottomSheet.TYPE_PIX)
                }
                Text("Outras formas de pagamento", modifier = Modifier.padding(top = 12.dp), color = DirectCheckoutColors.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmallMethod(Modifier.weight(1f), "Transferência\nPix", Icons.Default.Bolt, DirectCheckoutColors.Teal, "pix" in available) {
                        onSelectPaymentType(OrderDetailsPaymentMethodPickerBottomSheet.TYPE_PIX)
                    }
                    SmallMethod(Modifier.weight(1f), "Crédito Loja", Icons.Default.AccountBalance, DirectCheckoutColors.Purple, "store_credit" in available) {
                        onSelectPaymentType(OrderDetailsPaymentMethodPickerBottomSheet.TYPE_STORE_CREDIT)
                    }
                    SmallMethod(Modifier.weight(1f), "Dinheiro", Icons.Default.Payments, DirectCheckoutColors.Green, "dinheiro" in available) {
                        onSelectPaymentType(OrderDetailsPaymentMethodPickerBottomSheet.TYPE_CASH)
                    }
                }
            }
        }
    }
}

@Composable
private fun CreditScreen(
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

@Composable
private fun DebitScreen(amount: Double, onBack: () -> Unit, onContinue: () -> Unit) {
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

@Composable
private fun WaitingScreen(total: Double, paymentType: String, onBack: () -> Unit) {
    val presentation = DirectCheckoutOrderPresentation.waitingPresentation(paymentType)
    val transition = rememberInfiniteTransition(label = "waiting")
    val scale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "pulse",
    )
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        NavBar("Pagamento", onBack)
        AmountCard(presentation.amountLabel, DirectCheckoutOrderPresentation.formatCurrency(total), Icons.Default.AttachMoney, DirectCheckoutColors.Blue)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.size(196.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                        .clip(RoundedCornerShape(42.dp))
                        .background(DirectCheckoutColors.BlueSoft.copy(alpha = 0.55f)),
                )
                Box(
                    modifier = Modifier
                        .size(118.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(DirectCheckoutColors.BlueSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = DirectCheckoutColors.Blue, modifier = Modifier.size(54.dp))
                }
            }
            Text(presentation.title, color = DirectCheckoutColors.Ink, fontSize = 26.sp, fontWeight = FontWeight.Black)
            Text(
                presentation.subtitle,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .widthIn(max = 240.dp),
                color = DirectCheckoutColors.Muted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier
                    .padding(top = 28.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(DirectCheckoutColors.BlueSoft)
                    .border(1.dp, DirectCheckoutColors.BlueBorder, RoundedCornerShape(999.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Wifi, contentDescription = null, tint = DirectCheckoutColors.Blue, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(presentation.status, color = DirectCheckoutColors.Blue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun InstallmentSimulatorScreen(
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
    val amount = DirectCheckoutOrderPresentation.currencyInputAmount(amountDigits)
    val formattedAmount = DirectCheckoutOrderPresentation.formatCurrencyInput(amountDigits)
    val shareText = simulatorShareText(amount, installments)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, DirectCheckoutColors.Border)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = DirectCheckoutColors.Ink)
            }
            Text(
                modifier = Modifier.weight(1f),
                text = "Simulação de Crédito",
                color = DirectCheckoutColors.Ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onClose, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = DirectCheckoutColors.Text)
            }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DirectCheckoutColors.MutedSurface.copy(alpha = 0.55f))
                        .border(0.5.dp, DirectCheckoutColors.Border)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MethodIcon(Icons.Default.CreditCard, DirectCheckoutColors.Blue)
                    Column(modifier = Modifier.padding(start = 14.dp)) {
                        Text("Crédito", color = DirectCheckoutColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Simular parcelamento em até 18x", color = DirectCheckoutColors.Muted, fontSize = 13.sp)
                    }
                }

                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("Valor", color = DirectCheckoutColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = formattedAmount,
                        onValueChange = onAmountChange,
                        leadingIcon = { Text("R$", color = DirectCheckoutColors.Muted, fontWeight = FontWeight.Medium) },
                        placeholder = { Text("0,00") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                    )

                    when {
                        amount > 0.0 && installments.isEmpty() && isLoading -> LoadingBlock("Consultando parcelamentos...")
                        errorMessage != null -> Text(errorMessage, color = DirectCheckoutColors.Red, fontWeight = FontWeight.SemiBold)
                        amount > 0.0 && installments.isEmpty() -> Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            onClick = onConsult,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DirectCheckoutColors.Blue.copy(alpha = 0.10f),
                                contentColor = DirectCheckoutColors.Blue,
                            ),
                        ) {
                            Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Consultar Parcelas", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (installments.isNotEmpty()) {
                        Text("Parcelas", color = DirectCheckoutColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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
                .border(0.5.dp, DirectCheckoutColors.Border)
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
                            containerColor = DirectCheckoutColors.MutedSurface,
                            contentColor = DirectCheckoutColors.Ink,
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
                        colors = ButtonDefaults.buttonColors(containerColor = DirectCheckoutColors.WhatsappGreen),
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
                color = DirectCheckoutColors.Muted,
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
            .background(if (isSelected) DirectCheckoutColors.Blue.copy(alpha = 0.05f) else Color.White)
            .border(1.dp, if (isSelected) DirectCheckoutColors.Blue else DirectCheckoutColors.Border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = "${installment.installmentNumber}x de ${currencyText(installment.installmentValue)}${if (installment.noInterest) " sem juros" else ""}",
            color = if (isSelected) DirectCheckoutColors.Blue else DirectCheckoutColors.Ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "Total ${currencyText(installment.totalValue)}",
            color = if (isSelected) DirectCheckoutColors.Blue.copy(alpha = 0.72f) else DirectCheckoutColors.Muted,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun NavBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(DirectCheckoutColors.Key),
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = DirectCheckoutColors.Text)
        }
        Text(
            title,
            modifier = Modifier.padding(start = 10.dp),
            color = DirectCheckoutColors.Ink,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AmountCard(label: String, amount: String, icon: ImageVector, iconColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(DirectCheckoutColors.Key)
            .border(1.dp, DirectCheckoutColors.Border, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iconColor.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconColor)
        }
        Column(modifier = Modifier.padding(start = 14.dp)) {
            Text(label, color = DirectCheckoutColors.Faint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(amount, color = DirectCheckoutColors.Ink, fontSize = 25.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun PaymentMethodRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(
                if (title == "Crédito" && enabled) 2.dp else 1.dp,
                if (title == "Crédito" && enabled) DirectCheckoutColors.Blue else DirectCheckoutColors.Border,
                RoundedCornerShape(18.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MethodIcon(icon, color)
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(title, color = DirectCheckoutColors.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = DirectCheckoutColors.Muted, fontSize = 12.sp)
        }
        Text("›", color = DirectCheckoutColors.Faint, fontSize = 28.sp)
    }
}

@Composable
private fun SmallMethod(modifier: Modifier, title: String, icon: ImageVector, color: Color, enabled: Boolean, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, DirectCheckoutColors.Border, RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MethodIcon(icon, color)
        Text(
            title,
            modifier = Modifier.padding(top = 8.dp),
            color = DirectCheckoutColors.Text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MethodIcon(icon: ImageVector, color: Color) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun InstallmentRow(installment: InstallmentFee, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) DirectCheckoutColors.Blue else DirectCheckoutColors.Border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${installment.installmentNumber}x",
            modifier = Modifier.width(42.dp),
            color = if (isSelected) DirectCheckoutColors.Blue else DirectCheckoutColors.Faint,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(currencyText(installment.installmentValue), color = DirectCheckoutColors.Ink, fontWeight = FontWeight.Bold)
            Text("Total ${currencyText(installment.totalValue)}", color = DirectCheckoutColors.Faint, fontSize = 12.sp)
        }
        Text(
            if (installment.noInterest) "sem juros" else "taxa incl.",
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(if (installment.noInterest) DirectCheckoutColors.GreenSoft else DirectCheckoutColors.Key)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            color = if (installment.noInterest) DirectCheckoutColors.Green else DirectCheckoutColors.Muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun Metric(label: String, value: String, color: Color, alignEnd: Boolean = false) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(label, color = DirectCheckoutColors.Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DetailCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .border(0.5.dp, DirectCheckoutColors.Border)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Text(label.uppercase(), color = DirectCheckoutColors.Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(
            value,
            modifier = Modifier.padding(top = 6.dp),
            color = DirectCheckoutColors.Text,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MetricCell(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 10.dp, vertical = 16.dp),
    ) {
        Text(label, color = DirectCheckoutColors.Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun SummaryRow(label: String, value: String, strong: Boolean = false, subtitle: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (strong) 14.dp else 0.dp))
            .background(if (strong) DirectCheckoutColors.Key else Color.White)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = if (strong) DirectCheckoutColors.Ink else DirectCheckoutColors.Muted, fontSize = 14.sp, fontWeight = if (strong) FontWeight.Bold else FontWeight.Normal)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    modifier = Modifier.padding(top = 3.dp),
                    color = DirectCheckoutColors.Blue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Text(value, color = DirectCheckoutColors.Ink, fontSize = 14.sp, fontWeight = if (strong) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun LoadingBlock(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = DirectCheckoutColors.Blue)
        Text(text, modifier = Modifier.padding(top = 12.dp), color = DirectCheckoutColors.Muted)
    }
}

@Composable
private fun EmptyBlock(title: String, subtitle: String, actionText: String, onAction: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, color = DirectCheckoutColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(subtitle, modifier = Modifier.padding(top = 8.dp), color = DirectCheckoutColors.Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
        Button(
            modifier = Modifier.padding(top = 16.dp),
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(containerColor = DirectCheckoutColors.Blue),
        ) {
            Text(actionText)
        }
    }
}

private fun displayDate(raw: String): String {
    val parts = raw.take(10).split("-")
    return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else raw
}

private fun currencyText(value: String): String {
    return if (value.trim().startsWith("R$")) value.trim() else "R$ ${value.trim()}"
}

private fun simulatorShareText(amount: Double, installments: List<InstallmentFee>): String {
    val lines = installments.joinToString("\n") { installment ->
        "${installment.installmentNumber}x de ${currencyText(installment.installmentValue)} - Total ${currencyText(installment.totalValue)}"
    }
    return "Simulação de Crédito - ${DirectCheckoutOrderPresentation.formatCurrency(amount)}\n\n$lines"
}
