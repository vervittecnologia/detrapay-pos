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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Payments
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.detrapay.data.model.Order
import com.detrapay.ui.home.orders.components.OrderFlowColors
import com.detrapay.ui.home.orders.components.OrderFlowFintechTheme
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
    val registeredTotal = remember(orders) {
        orders.sumOf { OrderPresentation.summary(it).registeredAmount }
    }
    val pendingTotal = remember(orders) {
        orders.sumOf { OrderPresentation.summary(it).missingAmount.coerceAtLeast(0.0) }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(OrderFlowFintechTheme.Canvas),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OrderFlowFintechTheme.Canvas)
                        .padding(horizontal = 28.dp, vertical = 28.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(OrderFlowFintechTheme.PrimarySoft)
                                .clickable(onClick = onLogout),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = companyName.trim().take(1).ifBlank { "D" },
                                color = OrderFlowFintechTheme.Primary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                                .height(42.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(OrderFlowFintechTheme.Search)
                                .clickable { showSearch = true }
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = OrderFlowFintechTheme.Quiet, modifier = Modifier.size(20.dp))
                            Text(
                                modifier = Modifier.padding(start = 8.dp),
                                text = if (query.isBlank()) "Buscar por cliente ou pedido" else query,
                                color = if (query.isBlank()) OrderFlowFintechTheme.Quiet else OrderFlowFintechTheme.Body,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(
                            onClick = onLogout,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(OrderFlowFintechTheme.Search),
                        ) {
                            Icon(
                                Icons.Default.Logout,
                                contentDescription = "Sair",
                                tint = OrderFlowFintechTheme.Quiet,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    WalletHeroBanner()

                    WalletBalanceBlock(
                        companyName = companyName,
                        companyDocument = companyDocument,
                        registeredTotal = registeredTotal,
                        pendingTotal = pendingTotal,
                        orderCount = orders.size,
                        onRefresh = onReload,
                    )

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

                    WalletServicesBlock(
                        onNewOrder = onNewOrder,
                        onOpenSimulator = onOpenSimulator,
                        onSearch = { showSearch = true },
                        onRefresh = onReload,
                    )

                    Row(
                        modifier = Modifier.padding(top = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = "Pedidos recentes",
                            color = OrderFlowFintechTheme.Ink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${filtered.size} exibido${if (filtered.size == 1) "" else "s"}",
                            color = OrderFlowFintechTheme.Primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
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
                                modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp),
                                text = "${filtered.size} resultado${if (filtered.size == 1) "" else "s"} para \"$query\"",
                                color = OrderFlowFintechTheme.Quiet,
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

            item { Spacer(modifier = Modifier.height(96.dp)) }
        }

        WalletBottomBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            onNewOrder = onNewOrder,
        )
    }
}

@Composable
private fun WalletHeroBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp)
            .height(112.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(OrderFlowFintechTheme.Teal, OrderFlowFintechTheme.Primary),
                ),
            )
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Column(modifier = Modifier.align(Alignment.CenterStart)) {
            Text("DetraPay", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Pedidos e recebimentos", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text(
                text = "Atualize, simule e receba em poucos toques",
                modifier = Modifier.padding(top = 6.dp),
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(58.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Payments, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
private fun WalletBalanceBlock(
    companyName: String,
    companyDocument: String,
    registeredTotal: Double,
    pendingTotal: Double,
    orderCount: Int,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Saldo atualizado",
                color = OrderFlowFintechTheme.Body,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = OrderPresentation.formatCurrency(registeredTotal),
                color = OrderFlowFintechTheme.Ink,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(OrderFlowFintechTheme.Primary),
                )
                Text(
                    modifier = Modifier.padding(start = 5.dp),
                    text = "Pendente ${OrderPresentation.formatCurrency(pendingTotal)} - $orderCount pedidos",
                    color = OrderFlowFintechTheme.Body,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                modifier = Modifier.padding(top = 3.dp),
                text = companyDocument.ifBlank { companyName },
                color = OrderFlowFintechTheme.Muted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(
            onClick = onRefresh,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(OrderFlowFintechTheme.Primary),
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Atualizar pedidos", tint = Color.White)
        }
    }
}

@Composable
private fun WalletServicesBlock(
    onNewOrder: () -> Unit,
    onOpenSimulator: () -> Unit,
    onSearch: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(OrderFlowFintechTheme.CardMuted)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WalletServiceTile(Modifier.weight(1f), "Novo\npedido", Icons.Default.Receipt, onNewOrder)
            WalletServiceTile(Modifier.weight(1f), "Simular\nparcelas", Icons.Default.CreditCard, onOpenSimulator)
            WalletServiceTile(Modifier.weight(1f), "Buscar\npedido", Icons.Default.Search, onSearch)
            WalletServiceTile(Modifier.weight(1f), "Atualizar\nlista", Icons.Default.Refresh, onRefresh)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(OrderFlowFintechTheme.PrimarySoft)
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Receipt, contentDescription = null, tint = OrderFlowFintechTheme.Primary, modifier = Modifier.size(18.dp))
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = "Fluxos e campos originais do DetraPay preservados",
                color = OrderFlowFintechTheme.Primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun WalletServiceTile(modifier: Modifier, label: String, icon: ImageVector, onClick: () -> Unit) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(OrderFlowFintechTheme.PrimarySoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = OrderFlowFintechTheme.Primary, modifier = Modifier.size(22.dp))
        }
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = label,
            color = OrderFlowFintechTheme.Ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 14.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WalletBottomBar(modifier: Modifier, onNewOrder: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .background(OrderFlowFintechTheme.BottomBar)
            .padding(horizontal = 20.dp, vertical = 9.dp)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        WalletBottomItem("Home", Icons.Default.Receipt, true)
        WalletBottomItem("Pedidos", Icons.Default.CreditCard, false)
        Button(
            onClick = onNewOrder,
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = OrderFlowFintechTheme.Primary),
            contentPadding = PaddingValues(0.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Novo pedido", tint = Color.White, modifier = Modifier.size(30.dp))
        }
        WalletBottomItem("Histórico", Icons.Default.CalendarToday, false)
        WalletBottomItem("Perfil", Icons.Default.Logout, false)
    }
}

@Composable
private fun WalletBottomItem(label: String, icon: ImageVector, selected: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) OrderFlowFintechTheme.Primary else OrderFlowFintechTheme.Muted,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            color = if (selected) OrderFlowFintechTheme.Primary else OrderFlowFintechTheme.Muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
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
            .padding(horizontal = 28.dp, vertical = 7.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(51.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (card.isFullyPaid) OrderFlowFintechTheme.GreenSoft else OrderFlowFintechTheme.PrimarySoft,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Receipt,
                    contentDescription = null,
                    tint = if (card.isFullyPaid) OrderFlowFintechTheme.Green else OrderFlowFintechTheme.Primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text(
                    text = order.customer.name.ifBlank { order.customer.cpfCnpj.ifBlank { "Pedido #${order.id}" } },
                    color = OrderFlowFintechTheme.Body,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#${order.id}",
                        color = OrderFlowFintechTheme.Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = OrderPresentation.sellerDateLabel(order.creationDate.ifBlank { order.billingDate }),
                        color = OrderFlowFintechTheme.Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(OrderFlowFintechTheme.CardMuted),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (card.isFullyPaid) OrderFlowFintechTheme.Green else OrderFlowFintechTheme.Red),
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = card.totalLabel,
                    color = OrderFlowFintechTheme.Body,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    text = OrderPresentation.sellerStatusLabel(order),
                    color = if (card.isFullyPaid) OrderFlowFintechTheme.Green else OrderFlowFintechTheme.Red,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
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
