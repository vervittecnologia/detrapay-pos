package com.detrapay.data.model.remote

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class OrderResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("status_in")
    val status: OrderStatusResponse,
    @SerializedName("vehicle_price")
    val vehiclePrice: Double,
    @SerializedName("billing_date")
    val billingDate: String,
    @SerializedName("original_amount")
    val originalAmount: Double,
    @SerializedName("current_amount")
    val currentAmount: Double,
    @SerializedName("is_vehicle_financed")
    val isVehicleFinanced: Boolean,
    @SerializedName("is_vehicle_special_plate")
    val isVehicleSpecialPlate: Boolean,
    @SerializedName("customer_id")
    val customer: OrderCustomerResponse,
    @SerializedName("vehicle_type_id")
    val vehicleType: VehicleTypItemResponse?,
    @SerializedName("items")
    val items: List<OrderItemResponse>,
    @SerializedName("receivables")
    val receivables: List<OrderReceivableItemResponse>,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("created_by_id")
    val createdById: Int? = null,
)

@Serializable
enum class OrderStatusResponse {
    @SerializedName("pending") PENDING,
    @SerializedName("paid") PAID,
    @SerializedName("authorized") AUTHORIZED,
    @SerializedName("completed") COMPLETED,
    @SerializedName("cancelled") CANCELLED
}

@Serializable
data class OrderCustomerResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("cpf_cnpj")
    val cpfCnpj: String,
    @SerializedName("phone_number")
    val phoneNumber: String,
    @SerializedName("email")
    val email: String?
)

@Serializable
data class OrderItemResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("total_price")
    val totalPrice: Double,
    @SerializedName("discount")
    val discount: Double,
    @SerializedName("sales_item_id")
    val salesItem: OrderSalesItemResponse?
)

@Serializable
data class OrderReceivableItemResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("documentId")
    val documentId: String,
    @SerializedName("amount_final")
    val amountFinal: Double,
    @SerializedName("amount_original")
    val amountOriginal: Double,
    @SerializedName("tax")
    val tax: Double?,
    @SerializedName("status_in")
    val status: OrderReceivableItemStatusResponse,
    @SerializedName("payment_date")
    val paymentDate: String,
    @SerializedName("refund_date")
    val refundDate: String?,
    @SerializedName("card_brand")
    val cardBrand: String?,
    @SerializedName("card_last4")
    val cardLast4: String?,
    @SerializedName("card_holder")
    val cardHolder: String?,
    @SerializedName("authorization_code")
    val authorizationCode: String?,
    @SerializedName("authorization_id")
    val authorizationId: String?,
    @SerializedName("installments")
    val installments: Int,
    @SerializedName("pix_tx_id_code")
    val pixTxIdCode: String?,
    @SerializedName("payment_method_id")
    val paymentMethod: PaymentMethodResponse,
    @SerializedName("cpf_cnpj_cliente")
    val cpfCnpjCliente: String? = null,
)

@Serializable
enum class OrderReceivableItemStatusResponse {
    @SerializedName("pending") PENDING,
    @SerializedName("paid") PAID,
    @SerializedName("cancelled") CANCELLED,
    @SerializedName("refunded") REFUNDED,
}

@Serializable
data class OrderSalesItemResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("is_discount_allowed")
    val isDiscountAllowed: Boolean,
    @SerializedName("price")
    val price: Double
)
