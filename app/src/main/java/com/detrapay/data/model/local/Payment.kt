package com.detrapay.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.detrapay.data.database.DbConstant

@Entity(tableName = DbConstant.PAYMENT_TABLE)
data class Payment(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val orderId: Int,
    val amount: String,
    val installments: Int,
    val paymentType: String,
    val transactionId: String? = null,
    val transactionCode: String? = null,
    val date: String? = null,
    val result: Int? = null,
    val cardBrand: String? = null,
    val cardLast4: String? = null,
    val cardHolder: String? = null,
    val pixTxIdCode: String? = null,
    val message: String? = null,
    val errorCode: String? = null
)
