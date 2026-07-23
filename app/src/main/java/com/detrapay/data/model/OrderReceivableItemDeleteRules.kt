package com.detrapay.data.model

fun OrderReceivableItem.canBeDeleted(): Boolean {
    val paymentType = paymentMethod.paymentType.orEmpty().trim().lowercase()
    val paymentName = paymentMethod.name.trim().lowercase()

    val isCard = paymentType.contains("credit") ||
        paymentType.contains("credito") ||
        paymentType.contains("debit") ||
        paymentType.contains("debito") ||
        paymentName.contains("credito") ||
        paymentName.contains("debito")

    return !(status == OrderReceivableItemStatus.PAID && isCard) &&
            status != OrderReceivableItemStatus.REFUNDED
}
