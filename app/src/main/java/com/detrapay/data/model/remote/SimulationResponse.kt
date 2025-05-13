package com.detrapay.data.model.remote

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class SimulationResponse(
    @SerializedName("customer")
    val customer: CustomerResponse,
    @SerializedName("simulation")
    val simulation: SimulationSimulationResponse,
    @SerializedName("items")
    val items: List<SimulationItemResponse>,
)

@Serializable
data class CustomerResponse(
    @SerializedName("cpf_cnpj")
    val cpfCnpj: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("phone_number")
    val whatsapp: String,
)

@Serializable
data class SimulationSimulationResponse(
    @SerializedName("billing_date")
    val billingDate: String,
    @SerializedName("vehicle_price")
    val vehiclePrice: String,
    @SerializedName("is_vehicle_financed")
    val vehicleDisposal: Boolean,
    @SerializedName("is_vehicle_special_plate")
    val vehicleSpecialPlate: Boolean,
    @SerializedName("total_price")
    val totalPrice: Double,
    @SerializedName("vehicle_type_id")
    val vehicleTypeId: Int,
)

@Serializable
data class SimulationItemResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("is_discount_allowed")
    val discountAllowed: Boolean,
    @SerializedName("price")
    val price: Double,
)