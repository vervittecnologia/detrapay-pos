package com.detrapay.ui.home.simplified

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.detrapay.R
import com.detrapay.data.model.Order
import com.detrapay.data.model.OrderReceivable
import com.detrapay.ui.home.direct_checkout.components.DirectCheckoutColors
import com.detrapay.ui.home.direct_checkout.components.LoadingBlock
import com.detrapay.ui.home.direct_checkout.components.EmptyBlock

@Composable
fun SimplifiedReceivableScreen(
    companyName: String,
    companyDocument: String,
    receivables: List<OrderReceivable>,
    isLoading: Boolean,
    errorMessage: String?,
    onLogout: () -> Unit,
    onReload: () -> Unit,
    onPay: (OrderReceivable) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, receivables) {
        receivables.filter {
            query.isBlank() || 
            it.order.id.toString().contains(query, ignoreCase = true) ||
            it.order.customer.name.contains(query, ignoreCase = true) ||
            it.order.customer.cpfCnpj.contains(query)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DirectCheckoutColors.Background)
    ) {
        SimplifiedHeader(onLogout = onLogout)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                OperatorInfo(companyName, companyDocument)
                
                SimplifiedSearchInput(
                    query = query,
                    onQueryChange = { query = it }
                )
            }

            when {
                isLoading -> item { LoadingBlock("Carregando pagamentos...") }
                errorMessage != null -> item {
                    EmptyBlock(
                        title = "Ops! Algo deu errado",
                        subtitle = errorMessage,
                        actionText = "Tentar novamente",
                        onAction = onReload
                    )
                }
                filtered.isEmpty() -> item {
                    EmptyBlock(
                        title = if (query.isEmpty()) "Nenhum pagamento pendente" else "Nenhum resultado encontrado",
                        subtitle = if (query.isEmpty()) "Quando houver pagamentos a serem realizados, eles aparecerão aqui." else "Tente buscar com outros termos.",
                        actionText = "Recarregar",
                        onAction = onReload
                    )
                }
                else -> {
                    items(filtered) { item ->
                        ReceivableCard(receivable = item, onPay = { onPay(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SimplifiedHeader(onLogout: () -> Unit) {
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
            text = "Pagamentos",
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
private fun OperatorInfo(companyName: String, companyDocument: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
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
            text = "CNPJ: $companyDocument",
            color = DirectCheckoutColors.Muted,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun SimplifiedSearchInput(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        placeholder = { Text("Buscar por cliente, CPF ou nº pedido...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DirectCheckoutColors.Muted) },
        trailingIcon = if (query.isNotEmpty()) {
            { IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Close, contentDescription = null) } }
        } else null,
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DirectCheckoutColors.Blue,
            unfocusedBorderColor = DirectCheckoutColors.Border
        )
    )
}

@Composable
private fun ReceivableCard(receivable: OrderReceivable, onPay: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clickable(onClick = onPay),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, DirectCheckoutColors.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "#${receivable.order.id} - ${receivable.order.customer.name.ifBlank { "Cliente" }}",
                    color = DirectCheckoutColors.Ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Método: ${receivable.receivable.paymentMethod.name}",
                    color = DirectCheckoutColors.Muted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "Valor: ${DirectCheckoutOrderPresentation.formatCurrency(receivable.receivable.amountOriginal)}",
                    color = DirectCheckoutColors.Blue,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Button(
                onClick = onPay,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DirectCheckoutColors.Blue)
            ) {
                Text("PAGAR", fontWeight = FontWeight.Bold)
            }
        }
    }
}
