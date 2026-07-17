package com.detrapay.data.model.remote

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class CreateOrderRequest(
    @SerializedName("customer")
    val customer: OrderCustomerRequest,
    @SerializedName("salesman_id")
    val salesmanId: Int?,
    @SerializedName("company_id")
    val companyId: Int,
    @SerializedName("dispatcher_id")
    val dispatcherId: Int,
    @SerializedName("simulation")
    val simulation: CreateOrderSimulationRequest,
    @SerializedName("receivables")
    val receivables: List<CreateOrderPaymentRequest>,
    @SerializedName("items")
    val items: List<OrderSimulationItemRequest>? = null
)

@Serializable
data class CreateOrderSimulationRequest(
    @SerializedName("billing_date")
    val billingDate: String,
    @SerializedName("vehicle_price")
    val vehiclePrice: String,
    @SerializedName("vehicle_type_id")
    val vehicleTypeId: Int,
    @SerializedName("is_vehicle_financed")
    val isVehicleFinanced: Boolean,
    @SerializedName("is_vehicle_special_plate")
    val isVehicleSpecialPlate: Boolean,
    @SerializedName("total_price")
    val totalPrice: String? = null
)

@Serializable
data class CreateOrderPaymentRequest(
    @SerializedName("payment_method_id")
    val paymentMethodId: Int,
    @SerializedName("amount_original")
    val amountOriginal: String,
    @SerializedName("amount_final")
    val amountFinal: String? = null,
    @SerializedName("tax")
    val tax: Double? = null,
    @SerializedName("installments")
    val installments: Int,
    @SerializedName("payment_date")
    val paymentDate: String
)
