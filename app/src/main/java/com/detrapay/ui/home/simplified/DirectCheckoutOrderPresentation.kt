package com.detrapay.ui.home.simplified

import com.detrapay.data.model.Order
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.OrderReceivableItemStatus
import com.detrapay.data.model.OrderStatus
import com.detrapay.data.model.PaymentMethod
import com.detrapay.ui.order_details.OrderPaymentTotals
import com.detrapay.ui.util.PaymentTypeRules
import java.util.Locale
import kotlin.math.roundToInt

data class DirectCheckoutSellerCardSummary(
    val totalLabel: String,
    val paidLabel: String,
    val balanceTitle: String,
    val balanceLabel: String,
    val isFullyPaid: Boolean,
    val progressPercent: Int,
)

data class DirectCheckoutOrderSummary(
    val registeredAmount: Double,
    val missingAmount: Double,
    val progress: Int,
    val registeredLabel: String,
    val pendingValueLabel: String,
    val missingLabel: String,
    val hasPendingBalance: Boolean,
)

data class DirectCheckoutWaitingPresentation(
    val amountLabel: String,
    val title: String,
    val subtitle: String,
    val status: String,
)

data class SellerCardSummary(
    val totalLabel: String,
    val paidLabel: String,
    val balanceLabel: String,
    val balanceTitle: String,
    val progressPercent: Int,
    val isFullyPaid: Boolean,
)

data class WaitingPresentation(
    val amountLabel: String,
    val title: String,
    val subtitle: String,
    val status: String,
)

object DirectCheckoutOrderPresentation {
    private const val PROGRESS_MAX = 1000
    private val locale = Locale("pt", "BR")

    fun pendingOrders(orders: List<Order>): List<Order> {
        return orders
            .filter { it.status != OrderStatus.CANCELLED && it.status != OrderStatus.COMPLETED }
            .filter { summary(it).hasPendingBalance }
            .sortedByDescending { it.id }
    }

    fun summary(order: Order): DirectCheckoutOrderSummary {
        val totals = OrderPaymentTotals.from(order.receivables)
        val registered = totals.declaredAmount.coerceAtLeast(0.0)
        val missing = (order.originalAmount - registered).coerceAtLeast(0.0)
        val progress = if (order.originalAmount > 0.0) {
            ((registered / order.originalAmount).coerceIn(0.0, 1.0) * PROGRESS_MAX).roundToInt()
        } else {
            0
        }

        return DirectCheckoutOrderSummary(
            registeredAmount = registered,
            missingAmount = missing,
            progress = progress,
            registeredLabel = "Registrado ${formatCurrency(registered)}",
            pendingValueLabel = "Valor pendente ${formatCurrency(missing)}",
            missingLabel = if (missing <= 0.0) "Fechado" else "Falta ${formatCurrency(missing)}",
            hasPendingBalance = missing > 0.0,
        )
    }

    fun sellerStatusLabel(order: Order): String {
        return when (order.status) {
            OrderStatus.CANCELLED -> "Cancelado"
            OrderStatus.COMPLETED -> "Concluído"
            OrderStatus.PAID,
            OrderStatus.AUTHORIZED -> "Quitado"
            OrderStatus.PENDING -> if (summary(order).hasPendingBalance) "Pendente" else "Quitado"
        }
    }

    fun exactPaymentMethod(
        paymentMethods: List<PaymentMethod>,
        type: String,
        installments: Int,
    ): PaymentMethod? {
        val normalizedType = PaymentTypeRules.normalize(type)
        return paymentMethods.firstOrNull { method ->
            PaymentTypeRules.normalize(method.paymentType) == normalizedType &&
                method.installments == installments
        }
    }

    fun createdReceivable(
        oldOrder: Order,
        newOrder: Order,
        paymentMethod: PaymentMethod,
        amount: Double,
    ): OrderReceivableItem? {
        val previousIds = oldOrder.receivables.map { it.id to it.documentId }.toSet()
        return newOrder.receivables.firstOrNull { receivable ->
            (receivable.id to receivable.documentId) !in previousIds &&
                receivable.status == OrderReceivableItemStatus.PENDING &&
                receivable.paymentMethod.id == paymentMethod.id &&
                kotlin.math.abs(receivable.amountOriginal - amount) < 0.01
        }
    }

    fun formatCurrency(value: Double): String {
        return "R$ %,.2f".format(locale, value)
    }

    fun paymentAmount(digits: String, pendingAmount: Double): Double {
        return if (digits.isBlank()) pendingAmount else digits.toDouble() / 100.0
    }

    fun paymentDisplayAmount(digits: String, pendingAmount: Double): String {
        return formatCurrency(paymentAmount(digits, pendingAmount))
    }

    fun currencyInputAmount(digits: String): Double {
        return digits.toDoubleOrNull()?.let { it / 100.0 } ?: 0.0
    }

    fun formatCurrencyInput(digits: String): String {
        return formatCurrency(currencyInputAmount(digits))
    }

    fun nextPaymentDigits(digits: String, key: String): String {
        return when (key) {
            "DEL" -> if (digits.isNotEmpty()) digits.dropLast(1) else ""
            else -> if (digits.length < 10) digits + key else digits
        }
    }

    fun debitFee(amount: Double): Double {
        return amount * 0.0199
    }

    fun debitTotal(amount: Double): Double {
        return amount + debitFee(amount)
    }

    fun statusLabel(order: Order): String {
        return statusLabel(order.status)
    }

    fun statusLabel(status: OrderStatus): String {
        return when (status) {
            OrderStatus.PAID, OrderStatus.AUTHORIZED -> "Quitado"
            OrderStatus.COMPLETED -> "Concluído"
            OrderStatus.CANCELLED -> "Cancelado"
            else -> "Pendente"
        }
    }

    fun paidPercent(order: Order): Int {
        return (summary(order).progress / 10.0).roundToInt()
    }

    fun receivableStatusLabel(receivable: OrderReceivableItem): String {
        return receivableStatusLabel(receivable.status)
    }

    fun receivableStatusLabel(status: OrderReceivableItemStatus): String {
        return when (status) {
            OrderReceivableItemStatus.PAID -> "Quitado"
            OrderReceivableItemStatus.CANCELLED -> "Cancelado"
            OrderReceivableItemStatus.PENDING -> "Pendente"
            OrderReceivableItemStatus.REFUNDED -> "Estornado"
            else -> status.name.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    fun sellerCardSummary(order: Order): SellerCardSummary {
        val summary = summary(order)
        return SellerCardSummary(
            totalLabel = formatCurrency(order.originalAmount),
            paidLabel = formatCurrency(summary.registeredAmount),
            balanceLabel = if (summary.hasPendingBalance) formatCurrency(summary.missingAmount) else "Quitado",
            balanceTitle = if (summary.hasPendingBalance) "Falta" else "Status",
            progressPercent = paidPercent(order),
            isFullyPaid = !summary.hasPendingBalance,
        )
    }

    fun sellerDateLabel(date: String): String {
        val parts = date.take(10).split("-")
        return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else date
    }

    fun shouldStartPayment(order: Order): Boolean {
        return order.status != OrderStatus.CANCELLED &&
            order.status != OrderStatus.COMPLETED &&
            summary(order).hasPendingBalance
    }

    fun primaryActionLabel(order: Order): String {
        return if (shouldStartPayment(order)) "Pagar agora" else "Ver detalhes"
    }

    fun waitingPresentation(paymentType: String): WaitingPresentation {
        val normalized = PaymentTypeRules.normalize(paymentType)
        return if (normalized == "pix") {
            WaitingPresentation(
                amountLabel = "VALOR DO PIX",
                title = "Aguardando Pix",
                subtitle = "Peça ao cliente para escanear o QR Code gerado na maquininha.",
                status = "Aguardando pagamento..."
            )
        } else {
            WaitingPresentation(
                amountLabel = "VALOR DO PAGAMENTO",
                title = "Processando",
                subtitle = "Siga as instruções na maquininha para concluir o pagamento.",
                status = "Comunicando..."
            )
        }
    }
}
