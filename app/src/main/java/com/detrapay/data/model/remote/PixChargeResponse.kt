package com.detrapay.data.model.remote

import com.google.gson.annotations.SerializedName

data class PixChargeRequest(
    @SerializedName("amount")
    val amount: Double,
    @SerializedName("payment_date")
    val paymentDate: String? = null
)

data class PixChargeResponse(
    @SerializedName(
        value = "qr_code",
        alternate = ["qrCode", "payload", "emv", "pix_qr_code"]
    )
    val qrCode: String? = null,
    @SerializedName(
        value = "copy_paste",
        alternate = ["copyPaste", "copiaecola", "pixCopyPasteCode", "payload"]
    )
    val copyPaste: String? = null,
    @SerializedName(
        value = "qr_code_base64",
        alternate = ["qrCodeBase64", "qrcode_base64", "base64"]
    )
    val qrCodeBase64: String? = null,
    @SerializedName(
        value = "txid",
        alternate = ["pixTxIdCode", "pix_tx_id_code", "transactionId"]
    )
    val txId: String? = null,
    @SerializedName(
        value = "expires_at",
        alternate = ["expiresAt", "expirationDate", "dueDate"]
    )
    val expiresAt: String? = null
)
