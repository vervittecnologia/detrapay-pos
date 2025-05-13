package com.detrapay.data.model.remote

import kotlinx.serialization.Serializable


@Serializable
data class SimulationRequest(
    val cpf_cnpj: String,
    val name: String,
    val phone_number: String,
    val billing_date: String,
    val vehicle_price: String,
    val vehicle_type_id: Int,
    val is_vehicle_financed: Boolean,
    val is_vehicle_special_plate: Boolean,
)
