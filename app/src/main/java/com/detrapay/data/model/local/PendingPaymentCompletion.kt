package com.detrapay.data.model.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pending_payment_completions",
    indices = [Index(value = ["session_user_id", "status"])],
)
data class PendingPaymentCompletion(
    @PrimaryKey
    @ColumnInfo(name = "attempt_id")
    val attemptId: String,
    @ColumnInfo(name = "session_user_id")
    val sessionUserId: String,
    @ColumnInfo(name = "idempotency_key")
    val idempotencyKey: String,
    @ColumnInfo(name = "terminal_reference")
    val terminalReference: String,
    @ColumnInfo(name = "order_id")
    val orderId: Int,
    @ColumnInfo(name = "transaction_id")
    val transactionId: String,
    @ColumnInfo(name = "transaction_code")
    val transactionCode: String?,
    val date: String?,
    val time: String?,
    val result: Int?,
    @ColumnInfo(name = "payment_type")
    val paymentType: Int,
    val installments: Int,
    @ColumnInfo(name = "card_brand")
    val cardBrand: String?,
    @ColumnInfo(name = "card_last4")
    val cardLast4: String?,
    @ColumnInfo(name = "card_holder")
    val cardHolder: String?,
    @ColumnInfo(name = "pix_tx_id_code")
    val pixTxIdCode: String?,
    @ColumnInfo(name = "transaction_log")
    val transactionLog: String?,
    @ColumnInfo(name = "amount_original")
    val amountOriginal: Double,
    @ColumnInfo(name = "amount_final")
    val amountFinal: Double,
    val status: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
) {
    companion object {
        const val STATUS_APPROVED = "approved_pending_server"
    }
}
