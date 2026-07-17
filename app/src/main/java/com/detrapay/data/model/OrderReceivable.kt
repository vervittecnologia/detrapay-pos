package com.detrapay.data.model

import java.io.Serializable

data class OrderReceivable(
    val order: Order,
    val receivable: OrderReceivableItem
) : Serializable
