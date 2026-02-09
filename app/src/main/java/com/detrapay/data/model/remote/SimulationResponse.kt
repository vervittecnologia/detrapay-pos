package com.detrapay.data.model.remote

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class SimulationResponse(
    @SerializedName("data")
    val data: SimulationData
)

@Serializable
data class SimulationData(
    @SerializedName("attributes")
    val attributes: SimulationAttributes
)

@Serializable
data class SimulationAttributes(
    @SerializedName("billing_date")
    val billingDate: String,
    @SerializedName("cpf_cnpj")
    val cpfCnpj: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("phone_number")
    val whatsapp: String,
    @SerializedName("vehicle_price")
    val vehiclePrice: String,
    @SerializedName("is_vehicle_financed")
    val vehicleDisposal: Boolean,
    @SerializedName("is_vehicle_special_plate")
    val vehicleSpecialPlate: Boolean,
    @SerializedName("current_amount")
    val totalPrice: Double,
    @SerializedName("vehicle_type_id")
    val vehicleTypeId: Int,
    @SerializedName("items")
    val items: List<SimulationItemResponse>
)

@Serializable
data class SimulationItemResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("attributes")
    val attributes: SimulationItemAttributes
)

@Serializable
data class SimulationItemAttributes(
    @SerializedName("name")
    val name: String,
    @SerializedName("is_discount_allowed")
    val discountAllowed: Boolean,
    @SerializedName("price")
    val price: Double
)
