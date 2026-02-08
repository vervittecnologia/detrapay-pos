package com.detrapay.data.model.remote

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class PaginatedOrderResponse(
    val data: List<OrderResponse>,
    val meta: MetaResponse
)

@Serializable
data class OrderResponse(
    val id: Int,
    val attributes: OrderAttributesResponse
)

@Serializable
data class OrderAttributesResponse(
    val status: String,
    val originalAmount: Double,
    val currentAmount: Double,
    val billingDate: String,
    val createdAt: String,
    val customers: CustomerDataWrapper,
    val companies: CompanyDataWrapper
)

@Serializable
data class CustomerDataWrapper(
    val data: CustomerResponseData
)

@Serializable
data class CustomerResponseData(
    val id: Int,
    val attributes: CustomerAttributesResponse
)

@Serializable
data class CustomerAttributesResponse(
    val name: String,
    @SerializedName("cpfCnpj")
    val cpfCnpj: String
)

@Serializable
data class CompanyDataWrapper(
    val data: CompanyResponse
)

@Serializable
data class CompanyResponse(
    val id: Int,
    val attributes: CompanyAttributesResponse
)

@Serializable
data class CompanyAttributesResponse(
    @SerializedName("trade_name")
    val tradeName: String
)
