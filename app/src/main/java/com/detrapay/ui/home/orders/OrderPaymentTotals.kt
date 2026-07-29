package com.detrapay.ui.home.orders

import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.OrderReceivableItemStatus
import com.detrapay.ui.util.PaymentTypeRules

data class OrderPaymentTotals(
    val declaredAmount: Double,
    val paidAmount: Double,
    val receivedDisplayAmount: Double,
) {
    companion object {
        fun from(receivables: List<OrderReceivableItem>): OrderPaymentTotals {
            val active = receivables.filter {
                it.status != OrderReceivableItemStatus.CANCELLED &&
                    it.status != OrderReceivableItemStatus.REFUNDED
            }
            val paid = active.filter { it.status == OrderReceivableItemStatus.PAID }

            return OrderPaymentTotals(
                declaredAmount = active.sumOf { it.amountOriginal },
                paidAmount = paid.sumOf { it.amountOriginal },
                receivedDisplayAmount = paid.sumOf(::receivedAmountForSummary),
            )
        }

        private fun receivedAmountForSummary(receivable: OrderReceivableItem): Double {
            return when (PaymentTypeRules.normalize(receivable.paymentMethod.paymentType ?: receivable.paymentMethod.name)) {
                "dinheiro", "store_credit" -> receivable.amountFinal
                else -> receivable.amountOriginal
            }
        }
    }
}
