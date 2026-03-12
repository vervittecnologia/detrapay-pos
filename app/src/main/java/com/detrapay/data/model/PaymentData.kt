package com.detrapay.data.model

import java.io.Serializable

data class PaymentData(
    val transactionId: String? = null,
    val transactionCode: String? = null,
    val date: String? = null,
    val time: String? = null,
    val cardBrand: String? = null,
    val cardLast4: String? = null,
    val cardHolder: String? = null,
    val pixTxIdCode: String? = null,
    val transactionLog: String? = null,
    val pendingConfirmation: Boolean = false,
    val pixQrCodeContent: String? = null,
    val pixCopyPasteCode: String? = null,
    val pixQrCodeBase64: String? = null,
    val pixExpiresAt: String? = null,
    val amountOriginal: Double? = null,
    val amountFinal: Double? = null,
): Serializable
