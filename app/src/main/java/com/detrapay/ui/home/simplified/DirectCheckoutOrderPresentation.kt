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

data class DirectCheckoutOrderSummary(
    val registeredAmount: Double,
    val missingAmount: Double,
    val progress: Int,
    val registeredLabel: String,
    val pendingValueLabel: String,
    val missingLabel: String,
    val hasPendingBalance: Boolean,
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
