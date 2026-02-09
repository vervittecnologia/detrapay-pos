package com.detrapay.data.model

import java.io.Serializable

data class OrderReceivableItem(
    val id: Int,
    val documentId: String,
    val amountOriginal: Double,
    val amountFinal: Double,
    val max_installments: Int,
    val status: OrderReceivableItemStatus,
    val paymentMethod: PaymentMethod,
    val paymentDate: String?,
    val refundDate: String?,
    val cardLast4: String?,
    val cardHolder: String?,
    val tax: Double?,
    val cardBrand: String?,
    val authorizationId: String?,
    val authorizationCode: String?,
    val pixTxIdCode: String?
) : Serializable
