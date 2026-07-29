package com.detrapay.ui.home.orders.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.detrapay.R
import com.detrapay.data.model.Order
import com.detrapay.ui.home.orders.OrderPresentation
import com.detrapay.ui.home.orders.components.EmptyBlock
import com.detrapay.ui.home.orders.components.LoadingBlock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SellerCanvas = Color(0xFFF6F9FD)
private val SellerCard = Color(0xFFFFFFFF)
private val SellerInk = Color(0xFF1A212D)
private val SellerMuted = Color(0xFF58687E)
private val SellerBorder = Color(0xFFCED5DE)
private val SellerPrimary = Color(0xFF0F64B3)
private val SellerSearchSurface = Color(0x99EEF2F6)
private val SellerWarningSurface = Color(0xFFFEF6E7)
private val SellerWarningText = Color(0xFF73510D)
private val SellerSuccess = Color(0xFF35A748)
private val SellerDanger = Color(0xFFC92D32)
private val SellerExactFontFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold),
    Font(R.font.inter, FontWeight.ExtraBold),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("UNUSED_PARAMETER")
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
    val filteredOrders = remember(query, orders) {
        val digits = query.filter(Char::isDigit)
        OrderPresentation.allOrders(orders).filter { order ->
            query.isBlank() ||
                order.id.toString().contains(query, ignoreCase = true) ||
                order.customer.name.contains(query, ignoreCase = true) ||
                (digits.isNotBlank() && order.customer.cpfCnpj.contains(digits))
        }
    }

    CompositionLocalProvider(
        LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = SellerExactFontFamily),
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .background(SellerCanvas),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 176.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    SellerHeader(
                        showSearch = showSearch,
                        query = query,
                        onQueryChange = { query = it },
                        onSearchClick = { showSearch = !showSearch },
                    )
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
                    filteredOrders.isEmpty() -> item {
                        EmptyBlock(
                            title = "Nenhum pedido encontrado",
                            subtitle = if (query.isBlank()) {
                                "Quando houver pedidos, eles aparecerão aqui."
                            } else {
                                "Tente buscar com outros termos."
                            },
                            actionText = if (query.isBlank()) "Recarregar" else "Limpar busca",
                            onAction = {
                                if (query.isBlank()) onReload() else query = ""
                            },
                        )
                    }
                    else -> items(filteredOrders, key = { it.id }) { order ->
                        SellerOrderCard(
                            order = order,
                            onClick = { onOrderDetail(order) },
                        )
                    }
                }
            }

            SellerFloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 96.dp),
                onClick = onNewOrder,
            )

            SellerBottomNavigation(
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun SellerHeader(
    showSearch: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 28.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = "Pedidos",
                color = SellerInk,
                fontSize = 20.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            SellerSearchButton(onClick = onSearchClick)
        }

        if (showSearch) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                value = query,
                onValueChange = onQueryChange,
                textStyle = LocalTextStyle.current.copy(fontFamily = SellerExactFontFamily),
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = SellerMuted,
                        modifier = Modifier.size(18.dp),
                    )
                },
                placeholder = {
                    Text(
                        text = "Buscar por cliente ou pedido",
                        color = SellerMuted,
                        fontSize = 14.sp,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SellerCard,
                    unfocusedContainerColor = SellerCard,
                    focusedBorderColor = SellerPrimary,
                    unfocusedBorderColor = SellerBorder,
                    cursorColor = SellerPrimary,
                ),
            )
        }
    }
}

@Composable
private fun SellerSearchButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(SellerSearchSurface),
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = "Buscar pedidos",
            tint = SellerInk,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SellerOrderCard(order: Order, onClick: () -> Unit) {
    val card = OrderPresentation.sellerCardSummary(order)
    val status = OrderPresentation.sellerStatusLabel(order)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 129.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = SellerCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, SellerBorder),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "#${order.id}",
                        color = SellerPrimary,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = SellerMuted,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = sellerShortDate(
                                order.creationDate.ifBlank { order.billingDate },
                            ),
                            color = SellerMuted,
                            fontSize = 11.sp,
                            lineHeight = 16.5.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                SellerStatusBadge(status)
            }

            Text(
                modifier = Modifier.padding(top = 6.dp),
                text = order.customer.name.ifBlank {
                    order.customer.cpfCnpj.ifBlank { "Cliente" }
                },
                color = SellerInk,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            SellerMetricsGrid(
                modifier = Modifier.padding(top = 6.dp),
                total = card.totalLabel,
                paid = card.paidLabel,
                balanceTitle = card.balanceTitle,
                balance = card.balanceLabel,
                isFullyPaid = card.isFullyPaid,
            )
        }
    }
}

@Composable
private fun SellerMetricsGrid(
    modifier: Modifier,
    total: String,
    paid: String,
    balanceTitle: String,
    balance: String,
    isFullyPaid: Boolean,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(SellerBorder),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            SellerMetric(
                modifier = Modifier.weight(1f),
                label = "Total",
                value = total,
                valueColor = SellerInk,
            )
            SellerMetricDivider()
            SellerMetric(
                modifier = Modifier.weight(1f),
                label = "Pago",
                value = paid,
                valueColor = SellerInk,
                trend = SellerTrend.Up,
            )
            SellerMetricDivider()
            SellerMetric(
                modifier = Modifier.weight(1f),
                label = balanceTitle,
                value = balance,
                valueColor = if (isFullyPaid) SellerSuccess else SellerDanger,
                trend = if (isFullyPaid) null else SellerTrend.Down,
            )
        }
    }
}

@Composable
private fun SellerStatusBadge(status: String) {
    val isPending = status == "Pendente"
    val background = if (isPending) SellerWarningSurface else Color(0xFFEAF7EC)
    val foreground = if (isPending) SellerWarningText else SellerSuccess

    Row(
        modifier = Modifier
            .requiredWidth(148.dp)
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            if (isPending) Icons.Default.Storefront else Icons.Default.Receipt,
            contentDescription = null,
            tint = foreground,
            modifier = Modifier.size(12.dp),
        )
        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = "PENDENTE VENDEDOR",
            color = foreground,
            fontSize = 10.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private enum class SellerTrend {
    Up,
    Down,
}

@Composable
private fun SellerMetric(
    modifier: Modifier,
    label: String,
    value: String,
    valueColor: Color,
    trend: SellerTrend? = null,
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            trend?.let {
                Icon(
                    if (it == SellerTrend.Up) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = if (it == SellerTrend.Up) SellerSuccess else SellerDanger,
                    modifier = Modifier.size(10.dp),
                )
            }
            Text(
                text = label.uppercase(),
                color = SellerInk.copy(alpha = 0.60f),
                fontSize = 10.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SellerMetricDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(31.dp)
            .background(SellerBorder),
    )
}

@Composable
private fun SellerFloatingActionButton(
    modifier: Modifier,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(SellerPrimary),
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "Novo pedido",
            tint = Color.White,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun SellerBottomNavigation(modifier: Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(73.dp),
        color = Color.White.copy(alpha = 0.96f),
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SellerBottomItem("Início", Icons.Default.Home, selected = false, Modifier.weight(1f))
            SellerBottomItem("Pedidos", Icons.Default.Receipt, selected = true, Modifier.weight(1f))
            SellerBottomItem("Perfil", Icons.Default.Person, selected = false, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SellerBottomItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier,
) {
    val color = if (selected) SellerPrimary else SellerMuted
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(22.dp),
        )
        Text(
            modifier = Modifier.padding(top = 3.dp),
            text = label,
            color = color,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
        if (selected) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(22.dp)
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(SellerPrimary),
            )
        }
    }
}

private fun sellerShortDate(value: String): String {
    return runCatching {
        val date = LocalDate.parse(value.take(10), DateTimeFormatter.ISO_LOCAL_DATE)
        date.format(
            DateTimeFormatter.ofPattern("dd MMM", Locale("pt", "BR")),
        ).uppercase(Locale("pt", "BR"))
    }.getOrElse {
        OrderPresentation.sellerDateLabel(value).take(6).uppercase(Locale("pt", "BR"))
    }
}
