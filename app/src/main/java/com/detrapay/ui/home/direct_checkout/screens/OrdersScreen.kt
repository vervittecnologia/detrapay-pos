package com.detrapay.ui.home.direct_checkout.screens

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
import com.detrapay.ui.home.direct_checkout.components.DirectCheckoutColors
import com.detrapay.ui.home.direct_checkout.components.*
import com.detrapay.ui.home.simplified.DirectCheckoutOrderPresentation

@Composable
fun OrdersScreen(
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
