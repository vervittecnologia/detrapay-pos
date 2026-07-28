package com.detrapay.data.model

fun OrderReceivableItem.canBeDeleted(): Boolean {
    return !paymentMethod.isOnlinePayment &&
        status != OrderReceivableItemStatus.REFUNDED &&
        status != OrderReceivableItemStatus.CANCELLED
}
