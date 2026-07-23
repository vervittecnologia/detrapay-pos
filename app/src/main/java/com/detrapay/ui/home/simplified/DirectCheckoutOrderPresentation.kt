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

    fun sellerCardSummary(order: Order): DirectCheckoutSellerCardSummary {
        val summary = summary(order)
        return DirectCheckoutSellerCardSummary(
            totalLabel = formatCurrency(order.originalAmount),
            paidLabel = formatCurrency(summary.registeredAmount),
            balanceTitle = if (summary.hasPendingBalance) "Falta" else "Status",
            balanceLabel = if (summary.hasPendingBalance) {
                formatCurrency(summary.missingAmount)
            } else {
                "Quitado"
            },
            isFullyPaid = !summary.hasPendingBalance,
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

    fun statusLabel(order: Order): String {
        return sellerStatusLabel(order)
    }

    fun paidPercent(order: Order): Int {
        val summary = summary(order)
        return if (order.originalAmount > 0.0) {
            ((summary.registeredAmount / order.originalAmount).coerceIn(0.0, 1.0) * 100).roundToInt()
        } else {
            0
        }
    }

    fun shouldStartPayment(order: Order): Boolean {
        return order.status != OrderStatus.CANCELLED &&
            order.status != OrderStatus.COMPLETED &&
            summary(order).hasPendingBalance
    }

    fun primaryActionLabel(order: Order): String {
        return if (shouldStartPayment(order)) "Receber" else "Ver detalhes"
    }

    fun receivableStatusLabel(receivable: OrderReceivableItem): String {
        return when (receivable.status) {
            OrderReceivableItemStatus.PENDING -> "Pendente"
            OrderReceivableItemStatus.PAID -> "Quitado"
            OrderReceivableItemStatus.CANCELLED -> "Cancelado"
            OrderReceivableItemStatus.REFUNDED -> "Estornado"
        }
    }

    fun sellerDateLabel(rawDate: String): String {
        if (rawDate.length < 10) return rawDate.ifBlank { "-" }
        val year = rawDate.substring(0, 4)
        val month = rawDate.substring(5, 7)
        val day = rawDate.substring(8, 10)
        return "$day/$month/$year"
    }

    fun nextPaymentDigits(current: String, key: String): String {
        return when (key) {
            "backspace" -> current.dropLast(1)
            "clear" -> ""
            else -> if (key.all(Char::isDigit)) (current + key).trimStart('0') else current
        }
    }

    fun currencyInputAmount(digits: String): Double {
        return digits.filter(Char::isDigit).toLongOrNull()?.let { it / 100.0 } ?: 0.0
    }

    fun paymentAmount(digits: String, fallbackAmount: Double): Double {
        return digits.takeIf { it.isNotBlank() }?.let(::currencyInputAmount) ?: fallbackAmount
    }

    fun paymentDisplayAmount(digits: String, fallbackAmount: Double): String {
        return formatCurrency(paymentAmount(digits, fallbackAmount))
    }

    fun formatCurrencyInput(digits: String): String {
        return formatCurrency(currencyInputAmount(digits))
    }

    fun debitFee(amount: Double): Double {
        return 0.0
    }

    fun debitTotal(amount: Double): Double {
        return amount + debitFee(amount)
    }

    fun waitingPresentation(paymentType: String): DirectCheckoutWaitingPresentation {
        return when (PaymentTypeRules.normalize(paymentType)) {
            "pix" -> DirectCheckoutWaitingPresentation(
                amountLabel = "VALOR DO PIX",
                title = "Gerando PIX",
                subtitle = "Aguarde enquanto preparamos o pagamento.",
                status = "Conectando",
            )
            "debito" -> DirectCheckoutWaitingPresentation(
                amountLabel = "VALOR DO DÉBITO",
                title = "Pagamento no débito",
                subtitle = "Use a maquininha para concluir o pagamento.",
                status = "Aguardando cartão",
            )
            "dinheiro", "cash", "store_credit" -> DirectCheckoutWaitingPresentation(
                amountLabel = "VALOR RECEBIDO",
                title = "Confirmando pagamento",
                subtitle = "Aguarde enquanto registramos o recebimento.",
                status = "Registrando",
            )
            else -> DirectCheckoutWaitingPresentation(
                amountLabel = "VALOR DO PAGAMENTO",
                title = "Pagamento em andamento",
                subtitle = "Aguarde enquanto processamos a operação.",
                status = "Processando",
            )
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
}
