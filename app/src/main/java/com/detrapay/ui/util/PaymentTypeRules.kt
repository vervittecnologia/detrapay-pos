package com.detrapay.ui.util

import com.detrapay.data.model.remote.BrandFeesData
import com.detrapay.data.model.remote.CalculateFeesResponse
import com.detrapay.data.model.remote.InstallmentFee
import java.util.Locale

object PaymentTypeRules {

    fun normalize(rawType: String?): String {
        return when (rawType.orEmpty().trim().lowercase()) {
            "credito", "credit", "cartao_credito", "cartao de credito" -> "credito"
            "debito", "debit", "cartao_debito", "cartao de debito" -> "debito"
            "pix" -> "pix"
            "dinheiro", "cash" -> "dinheiro"
            "store_credit", "credito_loja", "credito loja", "storecredit" -> "store_credit"
            else -> rawType.orEmpty().trim().lowercase()
        }
    }

    fun isDirectNoFeePaymentType(rawType: String?): Boolean {
        return when (normalize(rawType)) {
            "dinheiro", "store_credit" -> true
            else -> false
        }
    }

    fun isPix(rawType: String?): Boolean = normalize(rawType) == "pix"

    fun zeroFeeQuote(amount: Double, brand: String = "Sem taxa"): CalculateFeesResponse {
        val formattedAmount = String.format(Locale("pt", "BR"), "%,.2f", amount)
        return CalculateFeesResponse(
            data = listOf(
                BrandFeesData(
                    brand = brand,
                    installments = listOf(
                        InstallmentFee(
                            installmentNumber = 1,
                            installmentValue = formattedAmount,
                            totalValue = formattedAmount,
                            interestValue = "0,00",
                            noInterest = true
                        )
                    )
                )
            )
        )
    }
}
