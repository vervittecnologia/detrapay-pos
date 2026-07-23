package com.detrapay.ui.home.registration

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.detrapay.R
import com.detrapay.data.model.Order
import com.detrapay.ui.home.direct_checkout.components.DirectCheckoutColors
import com.detrapay.ui.home.direct_checkout.components.displayDate
import com.detrapay.ui.util.ImageUtils

@Composable
fun HomeScreen(
    companyName: String,
    companyDocument: String,
    dispatcherName: String,
    logoKey: String?,
    recentOrders: List<Order>,
    onLogout: () -> Unit,
    onNewRegistration: () -> Unit,
    onViewAllSales: () -> Unit,
    onOrderClick: (Order) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DirectCheckoutColors.Background)
    ) {
        HomeHeader(onLogout = onLogout)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                OperatorSection(
                    logoKey = logoKey,
                    companyName = companyName,
                    dispatcherName = dispatcherName,
                    companyDocument = companyDocument
                )
            }

            item {
                SectionTitle("Serviços Disponíveis")
                ServiceCard(
                    title = "Novo emplacamento",
                    description = "Simule taxas, formas de pagamento e crie o pedido",
                    icon = Icons.Default.ChevronRight,
                    onClick = onNewRegistration
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                PlaceholderServiceCard()
            }

            item {
                RecentOrdersHeader(onViewAll = onViewAllSales)
            }

            if (recentOrders.isEmpty()) {
                item {
                    Text(
                        text = "Nenhuma venda recente encontrada.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        textAlign = TextAlign.Center,
                        color = DirectCheckoutColors.Muted,
                        fontSize = 14.sp
                    )
                }
            } else {
                items(recentOrders) { order ->
                    RecentOrderCard(order = order, onClick = { onOrderClick(order) })
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(onLogout: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DirectCheckoutColors.Blue)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_detrapay_logo_white),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(42.dp)
        )
        Text(
            text = "Detrapay",
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onLogout) {
            Icon(
                painter = painterResource(id = R.drawable.ic_logout),
                contentDescription = "Sair",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun OperatorSection(
    logoKey: String?,
    companyName: String,
    dispatcherName: String,
    companyDocument: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, DirectCheckoutColors.Border, CircleShape)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            val context = LocalContext.current
            val imagePath = remember(logoKey) { 
                logoKey?.let { ImageUtils.getImagePath(context, it) } 
            }
            
            if (imagePath != null) {
                AsyncImage(
                    model = imagePath,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.icon),
                    error = painterResource(id = R.drawable.icon)
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.icon),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
        ) {
            Text(
                text = companyName.ifBlank { "Concessionária" },
                color = DirectCheckoutColors.Ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = dispatcherName.ifBlank { "Despachante" },
                color = DirectCheckoutColors.Muted,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "CNPJ: $companyDocument",
                color = DirectCheckoutColors.Muted,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp)
            .padding(top = 12.dp, bottom = 22.dp),
        color = DirectCheckoutColors.Ink,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.08.sp
    )
}

@Composable
private fun ServiceCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE9EDF5)), // home_service_background
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = DirectCheckoutColors.Ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    color = DirectCheckoutColors.Muted,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DirectCheckoutColors.Pale,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun PlaceholderServiceCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(horizontal = 28.dp)
            .border(1.dp, Color(0xFFDDE1E9), RoundedCornerShape(30.dp)) // home_placeholder_stroke
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .background(Color(0xFFF1F4F9), CircleShape), // home_placeholder_icon_container
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = DirectCheckoutColors.Pale,
                    modifier = Modifier.size(34.dp)
                )
            }
            Text(
                text = "Novos serviços em breve",
                modifier = Modifier.padding(start = 20.dp),
                color = DirectCheckoutColors.Pale,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RecentOrdersHeader(onViewAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp)
            .padding(top = 40.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "PEDIDOS RECENTES",
            color = DirectCheckoutColors.Ink,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.08.sp
        )
        Text(
            text = "Ver todos",
            modifier = Modifier.clickable(onClick = onViewAll),
            color = DirectCheckoutColors.Blue,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RecentOrderCard(order: Order, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, DirectCheckoutColors.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "#${order.id} - ${order.customer.name.ifBlank { "Cliente" }}",
                    color = DirectCheckoutColors.Ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Surface(
                    color = if (order.status.name.lowercase() == "paid") DirectCheckoutColors.GreenSoft else DirectCheckoutColors.WarningSoft,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = if (order.status.name.lowercase() == "paid") "Pago" else "Pendente",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = if (order.status.name.lowercase() == "paid") DirectCheckoutColors.Green else DirectCheckoutColors.WarningText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Text(
                text = "Vendedor: ${order.salesman?.name ?: "N/A"}",
                color = DirectCheckoutColors.Muted,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "CPF/CNPJ: ${order.customer.cpfCnpj}",
                color = DirectCheckoutColors.Muted,
                fontSize = 14.sp
            )
        }
    }
}
