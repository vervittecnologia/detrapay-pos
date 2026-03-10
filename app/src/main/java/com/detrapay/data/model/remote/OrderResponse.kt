package com.detrapay.data.model.remote

import com.detrapay.data.model.Salesman
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class PaginatedOrderResponse(
    val data: List<OrderResponse>,
    val meta: MetaResponse? = null
)

@Serializable
data class CreateOrderResponse(
    val data: OrderResponse
)

@Serializable
data class OrderResponse(
    val id: Int,
    val attributes: OrderAttributesResponse? = null,
    val status: String? = null,
    val originalAmount: Double? = null,
    val currentAmount: Double? = null,
    val billingDate: String? = null,
    val createdAt: String? = null,
    val vehiclePrice: Double? = null,
    @SerializedName(value = "isVehicleFinanced", alternate = ["is_vehicle_financed"])
    val isVehicleFinanced: Boolean? = null,
    @SerializedName(value = "isSpecialPlate", alternate = ["is_vehicle_special_plate"])
    val isSpecialPlate: Boolean? = null,
    val customer: FlatCustomerResponse? = null,
    val company: FlatCompanyResponse? = null,
    val vehicleType: FlatVehicleTypeResponse? = null,
    val items: List<FlatOrderItemResponse>? = null,
    val receivables: List<FlatOrderReceivableResponse>? = null,
    val salesman: FlatSalesmanResponse? = null,
    val customerName: String? = null,
    @SerializedName(value = "customerCpfCnpj", alternate = ["customer_cpf_cnpj", "cpfCnpj"])
    val customerCpfCnpj: String? = null,
    val vehicleTypeName: String? = null,
    val salesmanName: String? = null,
    val paymentStatusSummary: String? = null
)

@Serializable
data class OrderAttributesResponse(
    val status: String? = null,
    val originalAmount: Double? = null,
    val currentAmount: Double? = null,
    val billingDate: String? = null,
    val createdAt: String? = null,
    val customers: CustomerDataWrapper? = null,
    val companies: CompanyDataWrapper? = null,
    val receivables: OrderReceivableDataWrapper? = null,
    val vehiclePrice: Double? = null,
    val isVehicleFinanced: Boolean? = null,
    val isSpecialPlate: Boolean? = null,
    val vehicle_types: VehicleTypeDataWrapper? = null,
    val sales_order_items: SalesOrderItemsDataWrapper? = null,
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
    val phoneNumber: String? = null,
    val email: String? = null
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
    val authorizationCode: String?,
    @SerializedName(value = "pixTxIdCode", alternate = ["pix_tx_id_code", "txid"])
    val pixTxIdCode: String? = null
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
    @SerializedName(value = "installments", alternate = ["max_installments"])
    val installments: Int,
    @SerializedName(value = "interestTax", alternate = ["interest_tax", "tax"])
    val interestTax: Double? = null,
    @SerializedName(value = "paymentType", alternate = ["payment_type"])
    val paymentType: String? = null
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
    val id: Int,
    val attributes: SalesmanAttributesResponse
)

@Serializable
data class SalesmanAttributesResponse(
    val name: String
)

@Serializable
data class FlatCustomerResponse(
    val id: Int,
    val name: String,
    @SerializedName("cpfCnpj")
    val cpfCnpj: String,
    val phoneNumber: String? = null,
    val email: String? = null
)

@Serializable
data class FlatCompanyResponse(
    val id: Int,
    @SerializedName(value = "tradeName", alternate = ["trade_name"])
    val tradeName: String
)

@Serializable
data class FlatVehicleTypeResponse(
    val id: Int,
    val name: String
)

@Serializable
data class FlatSalesmanResponse(
    val id: Int,
    val name: String
)

@Serializable
data class FlatOrderItemResponse(
    val id: Int,
    val salesItemId: Int? = null,
    val name: String? = null,
    val unitPrice: Double? = null,
    val discount: Double? = null,
    val totalPrice: Double? = null
)

@Serializable
data class FlatOrderReceivableResponse(
    val id: Int,
    val documentId: String,
    val status: String,
    val installments: Int,
    val paymentDate: String? = null,
    val amountOriginal: Double,
    val amountFinal: Double,
    val tax: Double? = null,
    @SerializedName(value = "cardLast4", alternate = ["card_last4"])
    val cardLast4: String? = null,
    val cardHolder: String? = null,
    val cardBrand: String? = null,
    val authorizationCode: String? = null,
    @SerializedName(value = "pixTxIdCode", alternate = ["pix_tx_id_code", "txid"])
    val pixTxIdCode: String? = null,
    val paymentMethod: FlatPaymentMethodResponse? = null
)

@Serializable
data class FlatPaymentMethodResponse(
    val id: Int,
    val name: String,
    @SerializedName(value = "installments", alternate = ["maxInstallments", "max_installments"])
    val installments: Int? = null,
    @SerializedName(value = "interestTax", alternate = ["interest_tax"])
    val interestTax: Double? = null,
    @SerializedName(value = "paymentType", alternate = ["payment_type"])
    val paymentType: String? = null
)
