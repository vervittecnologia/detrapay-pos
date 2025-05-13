package com.detrapay.data.model.remote

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class VehicleTypeResponse(
    @SerializedName("vehicle")
    val data: List<VehicleTypItemResponse>,
)

@Serializable
data class VehicleTypItemResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
)