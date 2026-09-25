package com.detrapay.ui.home.orders.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.detrapay.R
import com.detrapay.data.model.Salesman
import com.detrapay.ui.home.orders.SellerHomeSection
import com.detrapay.ui.util.ImageUtils

private val ProfileCanvas = Color(0xFFF6F9FD)
private val ProfileInk = Color(0xFF1A212D)
private val ProfileMuted = Color(0xFF58687E)
private val ProfileBorder = Color(0xFFCED5DE)
private val ProfilePrimary = Color(0xFF0F64B3)

@Composable
fun SellerProfileScreen(
    companyName: String,
    companyDocument: String,
    dispatcherName: String,
    companyLogoKey: String?,
    salesmen: List<Salesman>,
    onLogout: () -> Unit,
    onSectionSelected: (SellerHomeSection) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ProfileCanvas),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                top = 20.dp,
                end = 16.dp,
                bottom = 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onSectionSelected(SellerHomeSection.Orders) }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Voltar para pedidos",
                            tint = ProfileInk,
                        )
                    }
                    Text(
                        text = "Perfil",
                        color = ProfileInk,
                        fontSize = 20.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            item {
                CompanyProfileCard(
                    companyName = companyName,
                    companyDocument = companyDocument,
                    dispatcherName = dispatcherName,
                    companyLogoKey = companyLogoKey,
                )
            }

            item {
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = "Equipe",
                    color = ProfileInk,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (salesmen.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ProfileBorder),
                    ) {
                        Text(
                            modifier = Modifier.padding(16.dp),
                            text = "Nenhum vendedor cadastrado.",
                            color = ProfileMuted,
                            fontSize = 14.sp,
                        )
                    }
                }
            } else {
                items(salesmen, key = { it.id ?: it.name }) { salesman ->
                    SalesmanProfileCard(salesman)
                }
            }

            item {
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(
                        Icons.Default.Logout,
                        contentDescription = null,
                        tint = ProfilePrimary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = "Sair",
                        color = ProfilePrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompanyProfileCard(
    companyName: String,
    companyDocument: String,
    dispatcherName: String,
    companyLogoKey: String?,
) {
    val context = LocalContext.current
    val logoPath = remember(companyLogoKey) {
        companyLogoKey?.let { ImageUtils.getImagePath(context, it) }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, ProfileBorder),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEAF3FC)),
                contentAlignment = Alignment.Center,
            ) {
                if (logoPath != null) {
                    AsyncImage(
                        model = logoPath,
                        contentDescription = "Logo da concessionária",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.icon),
                    )
                } else {
                    Icon(
                        Icons.Default.Storefront,
                        contentDescription = null,
                        tint = ProfilePrimary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            ) {
                Text(
                    text = companyName.ifBlank { "Concessionária" },
                    color = ProfileInk,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (companyDocument.isNotBlank()) {
                    Text(
                        modifier = Modifier.padding(top = 3.dp),
                        text = companyDocument,
                        color = ProfileMuted,
                        fontSize = 14.sp,
                    )
                }
                if (dispatcherName.isNotBlank()) {
                    Text(
                        modifier = Modifier.padding(top = 3.dp),
                        text = dispatcherName,
                        color = ProfileMuted,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun SalesmanProfileCard(salesman: Salesman) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, ProfileBorder),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEAF3FC)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials(salesman.name),
                    color = ProfilePrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    text = salesman.name,
                    color = ProfileInk,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                salesman.phoneNumber?.takeIf { it.isNotBlank() }?.let { phone ->
                    ContactLine(Icons.Default.Phone, phone)
                }
                salesman.email?.takeIf { it.isNotBlank() }?.let { email ->
                    ContactLine(Icons.Default.Email, email)
                }
            }
        }
    }
}

@Composable
private fun ContactLine(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String) {
    Row(
        modifier = Modifier.padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = ProfileMuted,
            modifier = Modifier.size(14.dp),
        )
        Text(
            modifier = Modifier.padding(start = 5.dp),
            text = value,
            color = ProfileMuted,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun initials(name: String): String {
    return name.trim()
        .split(Regex("\\s+"))
        .filter(String::isNotBlank)
        .take(2)
        .joinToString("") { it.take(1).uppercase() }
        .ifBlank { "--" }
}
