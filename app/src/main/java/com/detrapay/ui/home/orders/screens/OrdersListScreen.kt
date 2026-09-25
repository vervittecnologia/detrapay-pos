package com.detrapay.ui.home.orders.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.detrapay.ui.theme.DetrapayFontFamily
import com.detrapay.data.model.Order
import com.detrapay.ui.home.orders.OrderPresentation
import com.detrapay.ui.home.orders.SellerHomeSection
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
private val SellerSuccess = Color(0xFF23813B)
private val SellerDanger = Color(0xFFC92D32)
private val SellerExactFontFamily = DetrapayFontFamily

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
    onSectionSelected: (SellerHomeSection) -> Unit = {},
    initialShowSearch: Boolean = false,
    initialQuery: String = "",
    initialShowFabMenu: Boolean = false,
) {
    var query by remember { mutableStateOf(initialQuery) }
    var showSearch by remember { mutableStateOf(initialShowSearch || initialQuery.isNotBlank()) }
    val filteredOrders = remember(query, orders) {
        val digits = query.filter(Char::isDigit)
        orders.filter { order ->
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
                    top = 12.dp,
                    end = 16.dp,
                    bottom = 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    SellerHeader(
                        showSearch = showSearch,
                        query = query,
                        onQueryChange = { query = it },
                        onSearchClick = { showSearch = !showSearch },
                        onProfileClick = { onSectionSelected(SellerHomeSection.Profile) },
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

            SellerNewOrderButton(
                modifier = Modifier.align(Alignment.BottomCenter),
                onClick = onNewOrder,
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
    onProfileClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
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
            IconButton(onClick = onProfileClick) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Abrir perfil",
                    tint = SellerPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        if (showSearch) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .heightIn(min = 56.dp),
                value = query,
                onValueChange = onQueryChange,
                textStyle = LocalTextStyle.current.copy(fontFamily = SellerExactFontFamily, fontSize = 20.sp),
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
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
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
            .size(48.dp)
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
            .heightIn(min = 120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = SellerCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, SellerBorder),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
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
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = sellerShortDate(
                                order.creationDate.ifBlank { order.billingDate },
                            ),
                            color = SellerMuted,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                SellerStatusBadge(status)
            }

            Text(
                modifier = Modifier.padding(top = 5.dp),
                text = order.customer.name.ifBlank {
                    order.customer.cpfCnpj.ifBlank { "Cliente" }
                },
                color = SellerInk,
                fontSize = 16.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "PAGAMENTO",
                    color = SellerMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                    text = card.paymentStatusLabel,
                    color = if (card.isFullyPaid) SellerSuccess else SellerWarningText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    fontWeight = FontWeight.Bold,
                )
            }

            SellerMetricsGrid(
                modifier = Modifier.padding(top = 5.dp),
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
    val expanded = LocalDensity.current.fontScale > 1.1f ||
        LocalConfiguration.current.screenWidthDp < 400

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
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalAlignment = Alignment.Top,
        ) {
            SellerMetric(Modifier.weight(1f), "Total", total, SellerInk)
            SellerMetricDivider()
            SellerMetric(Modifier.weight(1f), "Pago", paid, SellerInk, SellerTrend.Up)
            if (!expanded) {
                SellerMetricDivider()
                SellerMetric(
                    modifier = Modifier.weight(1f),
                    label = balanceTitle,
                    value = balance,
                    valueColor = if (isFullyPaid) SellerSuccess else SellerWarningText,
                    trend = if (isFullyPaid) null else SellerTrend.Down,
                )
            }
        }
        if (expanded) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    .height(1.dp).background(SellerBorder),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = balanceTitle.uppercase(),
                    modifier = Modifier.weight(1f),
                    color = SellerMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = balance,
                    color = if (isFullyPaid) SellerSuccess else SellerWarningText,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SellerStatusBadge(status: String) {
    val (background, foreground) = when (status) {
        "Pendente" -> SellerWarningSurface to SellerWarningText
        "Em Progresso" -> Color(0xFFE8F3FD) to SellerPrimary
        "Concluído" -> Color(0xFFEAF7EC) to SellerSuccess
        else -> Color(0xFFFDECEE) to SellerDanger
    }

    Row(
        modifier = Modifier
            .widthIn(min = 112.dp)
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            if (status == "Pendente") Icons.Default.Storefront else Icons.Default.Receipt,
            contentDescription = null,
            tint = foreground,
            modifier = Modifier.size(14.dp),
        )
        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = status.uppercase(),
            color = foreground,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
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
        modifier = modifier.padding(horizontal = 2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            trend?.let {
                Icon(
                    if (it == SellerTrend.Up) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = if (it == SellerTrend.Up) SellerSuccess else SellerWarningText,
                    modifier = Modifier.size(12.dp),
                )
            }
            Text(
                text = label.uppercase(),
                color = SellerMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Text(
            text = value,
            color = valueColor,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            softWrap = true,
        )
    }
}

@Composable
private fun SellerMetricDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(34.dp)
            .background(SellerBorder),
    )
}

@Composable
private fun SellerNewOrderButton(
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp,
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SellerPrimary),
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = "Novo pedido",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 20.sp,
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
