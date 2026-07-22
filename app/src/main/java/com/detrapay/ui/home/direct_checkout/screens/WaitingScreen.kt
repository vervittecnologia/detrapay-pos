package com.detrapay.ui.home.direct_checkout.screens

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
import com.detrapay.ui.home.direct_checkout.components.*
import com.detrapay.ui.home.simplified.DirectCheckoutOrderPresentation

@Composable
fun WaitingScreen(total: Double, paymentType: String, onBack: () -> Unit) {
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
