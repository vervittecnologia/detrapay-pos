package com.detrapay.data.model

import java.io.Serializable

data class PaymentData(
    val transactionId: String,
    val transactionCode: String,
    val date: String,
    val time: String,
    val cardBrand: String? = null,
    val cardLast4: String? = null,
    val cardHolder: String? = null,
    val pixTxIdCode: String? = null
): Serializable
