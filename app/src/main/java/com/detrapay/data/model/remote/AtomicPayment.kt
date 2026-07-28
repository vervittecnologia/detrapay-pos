package com.detrapay.data.model.remote

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class PrepareOnlinePaymentRequest(
    @SerializedName("payment_method_id") val paymentMethodId: Int,
    @SerializedName("amount_original") val amountOriginal: Double,
    val installments: Int,
    @SerializedName("idempotency_key") val idempotencyKey: String,
)

data class PaymentAttemptResponse(
    val data: PaymentAttempt,
)

data class PaymentAttempt(
    val id: String,
    val status: String,
    @SerializedName("sales_order_id") val orderId: Int,
    @SerializedName("payment_method_id") val paymentMethodId: Int,
    @SerializedName("amount_original") val amountOriginal: Double,
    @SerializedName("amount_final") val amountFinal: Double,
    val installments: Int,
    @SerializedName("expires_at") val expiresAt: String,
)

data class CompleteOnlinePaymentRequest(
    @SerializedName("transaction_id") val transactionId: String,
    @SerializedName("authorization_code") val authorizationCode: String? = null,
    @SerializedName("card_brand") val cardBrand: String? = null,
    @SerializedName("card_last4") val cardLast4: String? = null,
    @SerializedName("card_holder") val cardHolder: String? = null,
    @SerializedName("transaction_log") val transactionLog: JsonElement? = null,
)

data class RecordManualPaymentRequest(
    @SerializedName("payment_method_id") val paymentMethodId: Int,
    @SerializedName("amount_original") val amountOriginal: Double,
    val installments: Int,
    @SerializedName("idempotency_key") val idempotencyKey: String,
    @SerializedName("transaction_log") val transactionLog: JsonElement? = null,
)

data class AtomicPaymentMutationResponse(
    val data: AtomicPaymentMutation,
    val updatedOrder: CreateOrderResponse,
)

data class AtomicPaymentMutation(
    @SerializedName("receivable_id") val receivableId: Int,
    @SerializedName("order_id") val orderId: Int,
    val created: Boolean,
)
