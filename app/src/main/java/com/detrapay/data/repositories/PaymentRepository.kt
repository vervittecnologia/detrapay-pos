package com.detrapay.data.repositories

import com.detrapay.data.datasources.local.PaymentDAO
import com.detrapay.data.model.local.Payment
import javax.inject.Inject
import javax.inject.Singleton
import com.detrapay.data.Result

@Singleton
class PaymentRepository @Inject constructor(
    private var paymentLocalDataSource: PaymentDAO,
) {

    suspend fun loadPaymentHistory(): Result<List<Payment>> {
        return Result.Success(paymentLocalDataSource.getPayments())
    }

    suspend fun saveTransaction(
        orderId: Int,
        amount: Double,
        installments: Int,
        paymentType: String,
        transactionId: String? = null,
        transactionCode: String? = null,
        date: String? = null,
        result: Int? = null,
        cardBrand: String? = null,
        cardLast4: String? = null,
        cardHolder: String? = null,
        pixTxIdCode: String? = null,
        message: String? = null,
        errorCode: String? = null
    ) {
       val payment = Payment(
           id = 0,
           orderId = orderId,
           amount= amount.toString(),
           installments= installments,
           paymentType= paymentType,
           transactionId= transactionId,
           transactionCode= transactionCode,
           date= date,
           result= result,
           cardBrand= cardBrand,
           cardLast4= cardLast4,
           cardHolder= cardHolder,
           pixTxIdCode= pixTxIdCode,
           message= message,
           errorCode= errorCode
       )
        paymentLocalDataSource.insertPayment(payment)
    }
}