package com.detrapay.ui.home.orders.screens

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.detrapay.data.model.PaymentData
import com.detrapay.ui.home.orders.OrderPresentation
import com.detrapay.ui.home.orders.components.NavBar
import com.detrapay.ui.home.orders.components.OrderFlowColors
import com.detrapay.ui.home.orders.components.OrderFlowFintechTheme
import com.detrapay.ui.payment.CardPaymentStage
import com.detrapay.ui.payment.CardPaymentErrorPresenter
import com.detrapay.ui.payment.CardPaymentStageResolver
import com.detrapay.ui.state.UIState
import com.detrapay.ui.util.PaymentTypeRules
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun WaitingScreen(
    total: Double,
    installments: Int,
    paymentType: String,
    paymentState: UIState<PaymentData>,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onDone: () -> Unit,
    onCopyPixCode: (String) -> Unit,
) {
    CompositionLocalProvider(
        LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = OrderFlowFintechTheme.Inter),
    ) {
        val isPix = PaymentTypeRules.normalize(paymentType) == "pix"
        val stage = CardPaymentStageResolver.resolve(paymentState)
        val paymentFinished = !isPix && stage == CardPaymentStage.APPROVED
        Column(modifier = Modifier.fillMaxSize().background(OrderFlowFintechTheme.Canvas)) {
            NavBar(
                title = "Receber pagamento",
                onBack = if (paymentFinished) onDone else onBack,
                onClose = if (paymentFinished) onDone else onClose,
            )
            PaymentSummaryCard(total = total, installments = installments)
            if (isPix) {
                PixPaymentContent(
                    paymentState = paymentState,
                    paymentData = (paymentState as? UIState.Success)?.data,
                    onCopyPixCode = onCopyPixCode,
                    onRetry = onRetry,
                    onDone = onDone,
                )
            } else {
                CardPaymentContent(
                    stage = stage,
                    errorMessage = (paymentState as? UIState.Error)?.message,
                    onRetry = onRetry,
                    onCancel = onClose,
                    onDone = onDone,
                )
            }
        }
    }
}

@Composable
private fun PaymentSummaryCard(total: Double, installments: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "VALOR DO PAGAMENTO",
                color = OrderFlowFintechTheme.Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
            )
            Text(
                OrderPresentation.formatCurrency(total),
                modifier = Modifier.padding(top = 2.dp),
                color = OrderFlowFintechTheme.Ink,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                "Parcelas: ${installments.coerceAtLeast(1)}x",
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(OrderFlowFintechTheme.CardMuted)
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                color = OrderFlowFintechTheme.Muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CardPaymentContent(
    stage: CardPaymentStage,
    errorMessage: String?,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit,
) {
    if (stage == CardPaymentStage.ENTER_PIN) {
        PinContent()
        return
    }

    val copy = cardStageCopy(stage)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (stage) {
            CardPaymentStage.PRESENT_CARD,
            CardPaymentStage.REMOVE_CARD,
            CardPaymentStage.APPROVED,
            CardPaymentStage.DECLINED -> PaymentCardVisual(stage)

            CardPaymentStage.STARTING,
            CardPaymentStage.PROCESSING -> AnimatedStageVisual(stage)

            CardPaymentStage.ENTER_PIN -> Unit
        }
        Text(
            if (stage == CardPaymentStage.DECLINED) {
                CardPaymentErrorPresenter.title(errorMessage)
            } else {
                copy.title
            },
            modifier = Modifier.padding(top = 20.dp),
            color = OrderFlowFintechTheme.Ink,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            if (stage == CardPaymentStage.DECLINED && !errorMessage.isNullOrBlank()) {
                errorMessage
            } else {
                copy.subtitle
            },
            modifier = Modifier.padding(top = 8.dp).widthIn(max = 340.dp),
            color = OrderFlowFintechTheme.Muted,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
        )

        when (stage) {
            CardPaymentStage.STARTING -> PaymentStatusPill("Conectando ao terminal...", Icons.Default.Wifi)
            CardPaymentStage.PRESENT_CARD -> PaymentActionBanner(
                message = "INSIRA OU APROXIME O CARTÃO",
                support = "Faça a leitura agora no terminal.",
                icon = Icons.Default.KeyboardArrowDown,
                backgroundColor = OrderFlowColors.BlueSoft,
                borderColor = OrderFlowColors.Blue,
                contentColor = OrderFlowColors.Blue,
            )
            CardPaymentStage.PROCESSING -> PaymentActionBanner(
                message = "NÃO RETIRE O CARTÃO",
                support = "Aguarde a autorização da transação.",
                icon = Icons.Default.Lock,
                backgroundColor = OrderFlowColors.BlueSoft,
                borderColor = OrderFlowColors.BlueBorder,
                contentColor = OrderFlowColors.Blue,
            )
            CardPaymentStage.REMOVE_CARD -> PaymentActionBanner(
                message = "RETIRE O CARTÃO AGORA",
                support = "Depois da retirada, aguarde o resultado final.",
                icon = Icons.Default.KeyboardArrowUp,
                backgroundColor = OrderFlowColors.WarningSoft,
                borderColor = OrderFlowColors.Warning,
                contentColor = OrderFlowColors.WarningText,
            )
            CardPaymentStage.APPROVED -> Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp).height(54.dp),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrderFlowColors.Blue),
            ) {
                Text("Concluir", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            CardPaymentStage.DECLINED -> {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp).height(54.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrderFlowColors.Blue),
                ) {
                    Text("Tentar novamente", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(54.dp),
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text("Cancelar", color = OrderFlowFintechTheme.Ink, fontWeight = FontWeight.Bold)
                }
            }
            CardPaymentStage.ENTER_PIN -> Unit
        }
    }
}

@Composable
private fun PinContent() {
    // Deliberately compact and top-aligned: the secure terminal keyboard uses the lower half.
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(72.dp).clip(CircleShape).background(OrderFlowFintechTheme.PrimarySoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = OrderFlowColors.Blue, modifier = Modifier.size(34.dp))
        }
        Text(
            "Senha solicitada",
            modifier = Modifier.padding(top = 14.dp),
            color = OrderFlowFintechTheme.Ink,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Use o teclado seguro exibido no terminal.",
            modifier = Modifier.padding(top = 6.dp),
            color = OrderFlowFintechTheme.Muted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        PaymentActionBanner(
            message = "DIGITE SUA SENHA",
            support = "O teclado seguro será exibido no terminal.",
            icon = Icons.Default.Lock,
            backgroundColor = OrderFlowColors.BlueSoft,
            borderColor = OrderFlowColors.Blue,
            contentColor = OrderFlowColors.Blue,
            topPadding = 14,
        )
    }
}

private data class CardStageCopy(val title: String, val subtitle: String)

private fun cardStageCopy(stage: CardPaymentStage): CardStageCopy = when (stage) {
    CardPaymentStage.STARTING -> CardStageCopy(
        "Iniciando",
        "Aguarde enquanto conectamos com o terminal de pagamento.",
    )
    CardPaymentStage.PRESENT_CARD -> CardStageCopy(
        "Aguardando cartão",
        "Siga a instrução destacada abaixo.",
    )
    CardPaymentStage.PROCESSING -> CardStageCopy(
        "Processando",
        "Estamos concluindo a transação. Aguarde alguns instantes.",
    )
    CardPaymentStage.ENTER_PIN -> CardStageCopy("Digite a senha", "")
    CardPaymentStage.REMOVE_CARD -> CardStageCopy(
        "Retire o cartão",
        "A operação ainda está sendo finalizada pelo terminal.",
    )
    CardPaymentStage.APPROVED -> CardStageCopy(
        "Pagamento aprovado",
        "A transação foi concluída com sucesso.",
    )
    CardPaymentStage.DECLINED -> CardStageCopy(
        "Falha no pagamento",
        "Não foi possível concluir a transação.",
    )
}

@Composable
private fun AnimatedStageVisual(stage: CardPaymentStage) {
    val transition = rememberInfiniteTransition(label = "payment-stage")
    val scale by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "payment-stage-pulse",
    )
    Box(modifier = Modifier.size(150.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(132.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .clip(CircleShape)
                .background(OrderFlowFintechTheme.PrimarySoft.copy(alpha = 0.65f)),
        )
        Box(
            modifier = Modifier.size(92.dp).clip(CircleShape).background(OrderFlowFintechTheme.PrimarySoft),
            contentAlignment = Alignment.Center,
        ) {
            if (stage == CardPaymentStage.PROCESSING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = OrderFlowColors.Blue,
                    strokeWidth = 5.dp,
                )
            } else {
                Icon(Icons.Default.Wifi, contentDescription = null, tint = OrderFlowColors.Blue, modifier = Modifier.size(44.dp))
            }
        }
    }
}

@Composable
private fun PaymentCardVisual(stage: CardPaymentStage) {
    Box(modifier = Modifier.width(286.dp).height(164.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF0D68BC), Color(0xFF00458D)),
                    ),
                )
                .padding(24.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(46.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF66A5E4)),
            )
            Text(
                "••••  ••••  ••••  4242",
                modifier = Modifier.align(Alignment.BottomStart),
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        if (stage == CardPaymentStage.APPROVED || stage == CardPaymentStage.DECLINED) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (stage == CardPaymentStage.APPROVED) OrderFlowColors.Green else Color(0xFFFF4B55)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (stage == CardPaymentStage.APPROVED) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
    }
}

@Composable
private fun PaymentStatusPill(status: String, icon: ImageVector, topPadding: Int = 22) {
    Row(
        modifier = Modifier
            .padding(top = topPadding.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(OrderFlowColors.BlueSoft)
            .border(1.dp, OrderFlowColors.BlueBorder, RoundedCornerShape(999.dp))
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = OrderFlowColors.Blue, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(status, color = OrderFlowColors.Blue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PaymentActionBanner(
    message: String,
    support: String,
    icon: ImageVector,
    backgroundColor: Color,
    borderColor: Color,
    contentColor: Color,
    topPadding: Int = 22,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(contentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                message,
                color = contentColor,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                support,
                modifier = Modifier.padding(top = 3.dp),
                color = contentColor,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun PixPaymentContent(
    paymentState: UIState<PaymentData>,
    paymentData: PaymentData?,
    onCopyPixCode: (String) -> Unit,
    onRetry: () -> Unit,
    onDone: () -> Unit,
) {
    val presentation = OrderPresentation.waitingPresentation("pix")
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.Receipt,
            contentDescription = null,
            tint = OrderFlowColors.Blue,
            modifier = Modifier.size(64.dp),
        )
        Text(
            presentation.title,
            modifier = Modifier.padding(top = 14.dp),
            color = OrderFlowFintechTheme.Ink,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
        )
        when {
            paymentData?.pendingConfirmation == true -> PixGeneratedContent(paymentData, onCopyPixCode, onDone)
            paymentState is UIState.Error -> PaymentErrorContent(paymentState.message.orEmpty(), onRetry)
            else -> PaymentStatusPill(
                (paymentState as? UIState.Loading)?.message ?: presentation.status,
                Icons.Default.Wifi,
            )
        }
    }
}

@Composable
private fun PaymentErrorContent(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.padding(top = 24.dp).fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = OrderFlowColors.RedSoft),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = OrderFlowColors.Red)
            Text(message, color = OrderFlowColors.Red, fontSize = 14.sp, textAlign = TextAlign.Center)
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = OrderFlowColors.Blue)) {
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
        modifier = Modifier.padding(top = 18.dp).fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Código Pix gerado", color = OrderFlowColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            qrBitmap?.let {
                Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = Modifier.size(196.dp))
            }
            if (pixCode.isNotBlank()) {
                Text(
                    pixCode,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(OrderFlowColors.MutedSurface).padding(12.dp),
                    color = OrderFlowColors.Text,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = { onCopyPixCode(pixCode) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrderFlowColors.Blue),
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copiar código Pix", fontWeight = FontWeight.Bold)
                }
            }
            OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text("Voltar para pedidos", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun decodeBase64Bitmap(base64Content: String): Bitmap? = runCatching {
    val normalized = base64Content.substringAfter("base64,", base64Content)
    val decodedBytes = Base64.decode(normalized, Base64.DEFAULT)
    BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
}.getOrNull()

private fun generateQrBitmap(content: String): Bitmap? = runCatching {
    val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 640, 640)
    val pixels = IntArray(bitMatrix.width * bitMatrix.height)
    for (y in 0 until bitMatrix.height) {
        for (x in 0 until bitMatrix.width) {
            pixels[y * bitMatrix.width + x] =
                if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        }
    }
    Bitmap.createBitmap(bitMatrix.width, bitMatrix.height, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, bitMatrix.width, 0, 0, bitMatrix.width, bitMatrix.height)
    }
}.getOrNull()
