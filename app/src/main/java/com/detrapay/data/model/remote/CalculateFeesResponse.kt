package com.detrapay.data.model.remote

import com.google.gson.annotations.SerializedName

data class CalculateFeesResponse(
    @SerializedName("data") val data: List<BrandFeesData>
)

data class BrandFeesData(
    @SerializedName("bandeira") val brand: String,
    @SerializedName("parcelas") val installments: List<InstallmentFee>
)

data class InstallmentFee(
    @SerializedName("parcela") val installmentNumber: Int,
    @SerializedName("valor_parcela") val installmentValue: String,
    @SerializedName("valor_total") val totalValue: String,
    @SerializedName("juros") val interestValue: String,
    @SerializedName("sem_juros") val noInterest: Boolean
)
