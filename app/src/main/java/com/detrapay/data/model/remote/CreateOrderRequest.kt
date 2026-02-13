package com.detrapay.data.model.remote

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class CreateOrderRequest(
    @SerializedName("customer")
    val customer: OrderCustomerRequest,
    @SerializedName("salesman_id")
    val salesmanId: String?,
    @SerializedName("company_id")
    val companyId: Int,
    @SerializedName("dispatcher_id")
    val dispatcherId: Int,
    @SerializedName("simulation")
    val simulation: CreateOrderSimulationRequest,
    @SerializedName("payments")
    val payments: List<CreateOrderPaymentRequest>
)

@Serializable
data class CreateOrderSimulationRequest(
    @SerializedName("billing_date")
    val billingDate: String,
    @SerializedName("vehicle_price")
    val vehiclePrice: Double,
    @SerializedName("vehicle_type_id")
    val vehicleTypeId: Int,
    @SerializedName("is_vehicle_financed")
    val isVehicleFinanced: Boolean,
    @SerializedName("is_vehicle_special_plate")
    val isVehicleSpecialPlate: Boolean
)

@Serializable
data class CreateOrderPaymentRequest(
    @SerializedName("payment_method_id")
    val paymentMethodId: Int,
    @SerializedName("amount_original")
    val amountOriginal: Double,
    @SerializedName("installments")
    val installments: Int,
    @SerializedName("payment_date")
    val paymentDate: String
)
