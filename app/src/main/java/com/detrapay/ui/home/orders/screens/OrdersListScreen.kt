package com.detrapay.ui.home.orders.screens

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.detrapay.data.model.Order
import com.detrapay.ui.home.orders.components.OrderFlowColors
import com.detrapay.ui.home.orders.components.*
import com.detrapay.ui.home.orders.OrderPresentation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersListScreen(
    companyName: String,
    companyDocument: String,
    orders: List<Order>,
    isLoading: Boolean,
    isRefreshing: Boolean = false,
    errorMessage: String?,
    onLogout: () -> Unit,
    onReload: () -> Unit,
    onRefresh: () -> Unit = {},
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

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
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
                        Column(
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = "Pedidos",
                                color = OrderFlowColors.Ink,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                            )
                            Text(
                                text = companyName,
                                color = OrderFlowColors.Muted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (companyDocument.isNotBlank()) {
                                Text(
                                    text = companyDocument,
                                    color = OrderFlowColors.Faint,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        IconButton(
                            onClick = onLogout,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(OrderFlowColors.MutedSurface),
                        ) {
                            Icon(
                                Icons.Default.Logout,
                                contentDescription = "Sair",
                                tint = OrderFlowColors.Ink,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        IconButton(
                            onClick = {
                                showSearch = !showSearch
                                if (!showSearch) query = ""
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(OrderFlowColors.MutedSurface),
                        ) {
                            Icon(
                                if (showSearch) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = if (showSearch) "Fechar busca" else "Buscar pedidos",
                                tint = OrderFlowColors.Ink,
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
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = OrderFlowColors.Muted) },
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
                                color = OrderFlowColors.Muted,
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
            colors = ButtonDefaults.buttonColors(containerColor = OrderFlowColors.Blue),
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
            .border(1.dp, OrderFlowColors.Border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(OrderFlowColors.Blue.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = OrderFlowColors.Blue, modifier = Modifier.size(18.dp))
        }
        Text(
            modifier = Modifier.padding(start = 12.dp),
            text = label,
            color = OrderFlowColors.Ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SellerOrderCard(order: Order, onClick: () -> Unit) {
    val card = OrderPresentation.sellerCardSummary(order)
    val progress = card.progressPercent.coerceIn(0, 100) / 100f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, OrderFlowColors.Border),
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
                        color = OrderFlowColors.Blue,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Row(
                        modifier = Modifier.padding(start = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = OrderFlowColors.Muted, modifier = Modifier.size(18.dp))
                        Text(
                            modifier = Modifier.padding(start = 5.dp),
                            text = OrderPresentation.sellerDateLabel(order.creationDate.ifBlank { order.billingDate }),
                            color = OrderFlowColors.Muted,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                SellerStatusBadge(OrderPresentation.sellerStatusLabel(order))
            }

            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = order.customer.name.ifBlank { order.customer.cpfCnpj.ifBlank { "-" } },
                color = OrderFlowColors.Ink,
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
                    .background(OrderFlowColors.Border),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                SellerMetric("Total", card.totalLabel, OrderFlowColors.Ink, Modifier.weight(1f))
                VerticalMetricDivider()
                SellerMetric("Pago", card.paidLabel, if (card.isFullyPaid) OrderFlowColors.Green else OrderFlowColors.Ink, Modifier.weight(1f))
                VerticalMetricDivider()
                SellerMetric(
                    card.balanceTitle,
                    card.balanceLabel,
                    if (card.balanceTitle == "Falta" && !card.isFullyPaid) OrderFlowColors.Red else OrderFlowColors.Ink,
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
                    .background(OrderFlowColors.Track),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (card.isFullyPaid) OrderFlowColors.Green else OrderFlowColors.Warning),
                )
            }
        }
    }
}

@Composable
private fun SellerStatusBadge(label: String) {
    val bg = when (label) {
        "Quitado", "Concluído" -> OrderFlowColors.GreenSoft
        "Cancelado" -> OrderFlowColors.RedSoft
        else -> OrderFlowColors.WarningSoft
    }
    val fg = when (label) {
        "Quitado", "Concluído" -> OrderFlowColors.Green
        "Cancelado" -> OrderFlowColors.Red
        else -> OrderFlowColors.WarningText
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
                    tint = OrderFlowColors.Red,
                    modifier = Modifier
                        .padding(end = 2.dp)
                        .size(14.dp),
                )
            }
            Text(
                text = label.uppercase(),
                color = OrderFlowColors.Muted.copy(alpha = 0.92f),
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
            .background(OrderFlowColors.Border),
    )
}

@Composable
private fun LegacyOrdersScreen(
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
                    color = OrderFlowColors.Ink,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(OrderFlowColors.BlueSoft)
                        .border(1.dp, OrderFlowColors.BlueBorder, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    text = "Abertos",
                    color = OrderFlowColors.Blue,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = onNewOrder,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrderFlowColors.Blue),
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
            color = OrderFlowColors.Faint,
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
            Icon(Icons.Default.Logout, contentDescription = null, tint = OrderFlowColors.Pale, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Sair", color = OrderFlowColors.Pale, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun DealershipHeader(companyName: String, companyDocument: String, onLogout: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrderFlowColors.Blue)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "CONCESSIONÁRIA",
                color = OrderFlowColors.BlueOnSoft,
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
                color = OrderFlowColors.BlueOnSoft,
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
    val summary = OrderPresentation.summary(order)
    val status = OrderPresentation.statusLabel(order)
    val percent = OrderPresentation.paidPercent(order)
    val isPending = status == "Pendente"
    val startsPayment = OrderPresentation.shouldStartPayment(order)
    val primaryActionLabel = OrderPresentation.primaryActionLabel(order)

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
                .background(if (isPending) OrderFlowColors.Amber else OrderFlowColors.BlueLight),
        )
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pedido #${order.id}", color = OrderFlowColors.Ink, fontSize = 23.sp, fontWeight = FontWeight.Black)
                    Text(order.customer.name.ifBlank { "Cliente" }, color = OrderFlowColors.Text, fontSize = 18.sp)
                    Text(displayDate(order.billingDate), color = OrderFlowColors.Muted, fontSize = 15.sp)
                }
                Text(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isPending) OrderFlowColors.AmberSoft else OrderFlowColors.BlueSoft)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    text = status,
                    color = if (isPending) OrderFlowColors.AmberText else OrderFlowColors.Blue,
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
                Metric("TOTAL", OrderPresentation.formatCurrency(order.originalAmount), OrderFlowColors.Text)
                Metric(
                    "PENDENTE",
                    OrderPresentation.formatCurrency(summary.missingAmount),
                    if (isPending) OrderFlowColors.AmberText else OrderFlowColors.Blue,
                )
                Spacer(modifier = Modifier.weight(1f))
                Metric("PAGO", "$percent%", OrderFlowColors.Green, alignEnd = true)
            }
            if (percent > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(OrderFlowColors.Track),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(percent / 100f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Brush.horizontalGradient(listOf(OrderFlowColors.BlueLight, OrderFlowColors.Blue))),
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
                    colors = ButtonDefaults.buttonColors(containerColor = OrderFlowColors.Blue),
                ) {
                    Text(primaryActionLabel, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                if (startsPayment) {
                    Button(
                        modifier = Modifier.height(58.dp),
                        onClick = onDetail,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrderFlowColors.BlueSoft,
                            contentColor = OrderFlowColors.Blue,
                        ),
                    ) {
                        Text("Ver detalhes", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
