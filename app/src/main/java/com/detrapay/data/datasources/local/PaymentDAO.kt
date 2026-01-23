package com.detrapay.data.datasources.local

import androidx.room.*
import com.detrapay.data.model.local.Payment

@Dao
interface PaymentDAO {
    //for single user insert
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment): Long

    //getting all payments
    @Query("select * from payments")
    suspend fun getPayments(): List<Payment>

    //deleting all payments from db
    @Query("DELETE FROM payments")
    suspend fun deleteAll()
}
