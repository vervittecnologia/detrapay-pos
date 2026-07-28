package com.detrapay.ui.home.direct_checkout.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.detrapay.data.model.PaymentData
import com.detrapay.ui.home.direct_checkout.components.DirectCheckoutColors
import com.detrapay.ui.home.direct_checkout.components.*
import com.detrapay.ui.home.simplified.DirectCheckoutOrderPresentation
import com.detrapay.ui.state.UIState
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun WaitingScreen(
    total: Double,
    paymentType: String,
    paymentState: UIState<PaymentData>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onDone: () -> Unit,
    onCopyPixCode: (String) -> Unit,
) {
    val presentation = DirectCheckoutOrderPresentation.waitingPresentation(paymentType)
    val paymentData = (paymentState as? UIState.Success)?.data
    val errorMessage = (paymentState as? UIState.Error)?.message
    val loadingMessage = (paymentState as? UIState.Loading)?.message
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
            when {
                paymentData?.pendingConfirmation == true -> {
                    PixGeneratedContent(paymentData, onCopyPixCode, onDone)
                }
                errorMessage != null -> {
                    PaymentErrorContent(errorMessage, onRetry)
                }
                else -> {
                    PaymentStatusPill(loadingMessage ?: presentation.status)
                }
            }
        }
    }
}

@Composable
private fun PaymentStatusPill(status: String) {
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
        Text(status, color = DirectCheckoutColors.Blue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PaymentErrorContent(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(top = 28.dp)
            .padding(horizontal = 24.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DirectCheckoutColors.RedSoft),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = DirectCheckoutColors.Red)
            Text(message, color = DirectCheckoutColors.Red, fontSize = 14.sp, textAlign = TextAlign.Center)
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = DirectCheckoutColors.Blue),
            ) {
                Text("Tentar novamente", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PixGeneratedContent(
    paymentData: PaymentData,
    onCopyPixCode: (String) -> Unit,
    onDone: () -> Unit,
) {
    val pixCode = paymentData.pixCopyPasteCode?.takeIf { it.isNotBlank() }
        ?: paymentData.pixQrCodeContent.orEmpty()
    val qrBitmap = remember(paymentData.pixQrCodeBase64, pixCode) {
        paymentData.pixQrCodeBase64?.let(::decodeBase64Bitmap)
            ?: pixCode.takeIf { it.isNotBlank() }?.let(::generateQrBitmap)
    }

    Card(
        modifier = Modifier
            .padding(top = 24.dp)
            .padding(horizontal = 20.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Codigo Pix gerado", color = DirectCheckoutColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            qrBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(196.dp),
                )
            }
            if (pixCode.isNotBlank()) {
                Text(
                    pixCode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DirectCheckoutColors.MutedSurface)
                        .padding(12.dp),
                    color = DirectCheckoutColors.Text,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = { onCopyPixCode(pixCode) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DirectCheckoutColors.Blue),
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copiar codigo Pix", fontWeight = FontWeight.Bold)
                }
            }
            OutlinedButton(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text("Voltar para pedidos", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun decodeBase64Bitmap(base64Content: String): Bitmap? {
    return runCatching {
        val normalized = base64Content.substringAfter("base64,", base64Content)
        val decodedBytes = Base64.decode(normalized, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }.getOrNull()
}

private fun generateQrBitmap(content: String): Bitmap? {
    return runCatching {
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 640, 640)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                pixels[y * width + x] = if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
        }

        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }.getOrNull()
}
