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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.detrapay.ui.home.orders.components.OrderFlowColors
import com.detrapay.ui.home.orders.components.OrderFlowFintechTheme
import com.detrapay.ui.home.orders.components.*

@Composable
fun KeypadScreen(
    displayAmount: String,
    canPay: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onKey: (String) -> Unit,
    onContinue: () -> Unit,
) {
    CompositionLocalProvider(
        LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = OrderFlowFintechTheme.Inter),
    ) {
        Column(modifier = Modifier.fillMaxSize().background(OrderFlowFintechTheme.Canvas)) {
            NavBar("Novo pagamento", onBack, onClose)
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "VALOR DO PAGAMENTO",
                    color = OrderFlowFintechTheme.Quiet,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                )
                Text(
                    displayAmount,
                    modifier = Modifier.padding(top = 12.dp),
                    color = OrderFlowFintechTheme.Ink,
                    fontSize = 40.sp,
                    lineHeight = 48.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                if (errorMessage != null) {
                    Text(errorMessage, color = OrderFlowColors.AmberText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            SellerNativeKeypad(
                modifier = Modifier.weight(1f),
                enabled = !isLoading,
                onKey = onKey,
            )
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .height(52.dp),
                onClick = onContinue,
                enabled = canPay && !isLoading,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrderFlowFintechTheme.Primary,
                    disabledContainerColor = OrderFlowFintechTheme.Line,
                    disabledContentColor = OrderFlowFintechTheme.Muted,
                ),
            ) {
                Text(
                    text = "CONTINUAR",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                )
            }
        }
    }
}

@Composable
private fun SellerNativeKeypad(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onKey: (String) -> Unit,
) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "00", "0", "DEL")
    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        keys.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (key == "DEL") {
                                    OrderFlowFintechTheme.PrimarySoft
                                } else Color.White,
                            )
                            .clickable(enabled = enabled) { onKey(key) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (key == "DEL") {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Apagar",
                                tint = OrderFlowFintechTheme.Primary,
                                modifier = Modifier.size(24.dp),
                            )
                        } else {
                            Text(
                                key,
                                color = OrderFlowColors.Text,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}
