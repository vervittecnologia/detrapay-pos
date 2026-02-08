package com.detrapay.data.model.remote

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class OrderRequest(
    @SerializedName("customer")
    val customer: OrderCustomerRequest,
    @SerializedName("simulation")
    val simulation: OrderSimulationRequest,
    @SerializedName("items")
    val simulationItems: List<OrderSimulationItemRequest>,
    @SerializedName("receivables")
    val receivables: List<OrderReceivableRequest>,
    @SerializedName("created_by_id")
    val createdById: String? = null,
    @SerializedName("user_id")
    val userId: String? = null,
)

@Serializable
data class OrderCustomerRequest(
    @SerializedName("name")
    val name: String,
    @SerializedName("cpf_cnpj")
    val cpfCnpj: String,
    @SerializedName("phone_number")
    val phoneNumber: String
)

@Serializable
data class OrderSimulationRequest(
    @SerializedName("billing_date")
    val billingDate: String,
    @SerializedName("vehicle_price")
    val vehiclePrice: String,
    @SerializedName("is_vehicle_financed")
    val vehicleFinanced: Boolean,
    @SerializedName("is_vehicle_special_plate")
    val vehicleSpecialPlate: Boolean,
    @SerializedName("total_price")
    val totalPrice: String,
    @SerializedName("vehicle_type_id")
    val vehicleTypeId: Int
)

@Serializable
data class OrderSimulationItemRequest(
    @SerializedName("id")
    val id: Int,
    @SerializedName("price")
    val price: String
)

@Serializable
data class OrderReceivableRequest(
    @SerializedName("payment_method_id")
    val paymentMethodId: Int,
    @SerializedName("amount_final")
    val amountFinal: String,
    @SerializedName("amount_original")
    val amountOriginal: String,
    @SerializedName("tax")
    val tax: Double?,
    @SerializedName("payment_date")
    val paymentDate: String,
    @SerializedName("installments")
    val installments: Int,
    @SerializedName("cpf_cnpj_cliente")
    val cpfCnpjCliente: String? = null
)
