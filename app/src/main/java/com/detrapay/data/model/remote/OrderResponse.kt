package com.detrapay.data.model.remote

import com.detrapay.data.model.Salesman
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class PaginatedOrderResponse(
    val data: List<OrderResponse>,
    val meta: MetaResponse
)

@Serializable
data class CreateOrderResponse(
    val data: OrderResponse
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
    val companies: CompanyDataWrapper,
    val receivables: OrderReceivableDataWrapper? = null,
    val vehiclePrice: Double,
    val isVehicleFinanced: Boolean,
    val isSpecialPlate: Boolean,
    val vehicle_types: VehicleTypeDataWrapper,
    val sales_order_items: SalesOrderItemsDataWrapper,
    val salesman: SalesmanDataWrapper? = null
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
    val cpfCnpj: String,
    val phoneNumber: String,
    val email: String?
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

@Serializable
data class OrderReceivableDataWrapper(
    val data: List<OrderReceivableResponse>
)

@Serializable
data class OrderReceivableResponse(
    val id: Int,
    val documentId: String,
    val attributes: OrderReceivableAttributesResponse
)

@Serializable
data class OrderReceivableAttributesResponse(
    val status: String,
    val installments: Int,
    val paymentDate: String?,
    val payment_methods: PaymentMethodDataWrapper,
    val amountOriginal: Double,
    val amountFinal: Double,
    val card_last4: String?,
    val cardHolder: String?,
    val tax: Double?,
    val cardBrand: String?,
    val authorizationCode: String?
)

@Serializable
data class PaymentMethodDataWrapper(
    val data: PaymentMethodResponseData
)

@Serializable
data class PaymentMethodResponseData(
    val id: Int,
    val attributes: PaymentMethodAttributesResponse
)

@Serializable
data class PaymentMethodAttributesResponse(
    val name: String,
    @SerializedName("max_installments")
    val max_installments: Int,
    @SerializedName("interest_tax")
    val interest_tax: Double
)

@Serializable
data class VehicleTypeDataWrapper(
    val data: VehicleTypeResponse
)

@Serializable
data class VehicleTypeResponse(
    val id: Int,
    val attributes: VehicleTypeAttributesResponse
)

@Serializable
data class VehicleTypeAttributesResponse(
    val name: String
)

@Serializable
data class SalesOrderItemsDataWrapper(
    val data: List<SalesOrderItemResponse>
)

@Serializable
data class SalesOrderItemResponse(
    val id: Int,
    val attributes: SalesOrderItemAttributesResponse
)

@Serializable
data class SalesOrderItemAttributesResponse(
    val total_price: Double,
    val discount: Double,
    val sales_item_id: Int,
    val unit_price: Double,
    val sales_items: SalesItemsDataWrapper
)

@Serializable
data class SalesItemsDataWrapper(
    val data: SalesItemResponse
)

@Serializable
data class SalesItemResponse(
    val id: Int,
    val attributes: SalesItemAttributesResponse
)

@Serializable
data class SalesItemAttributesResponse(
    val name: String
)

@Serializable
data class SalesmanDataWrapper(
    val data: SalesmanResponse
)

@Serializable
data class SalesmanResponse(
    val id: String,
    val attributes: SalesmanAttributesResponse
)

@Serializable
data class SalesmanAttributesResponse(
    val name: String
)
