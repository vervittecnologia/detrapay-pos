package com.detrapay.data.model.remote

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class VehicleTypeListResponse(
    @SerializedName("data")
    val data: List<VehicleTypeItemResponse>,
    @SerializedName("meta")
    val meta: MetaResponse
)

@Serializable
data class VehicleTypeItemResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("documentId")
    val documentId: String,
    @SerializedName("attributes")
    val attributes: VehicleTypeAttributes
)

@Serializable
data class VehicleTypeAttributes(
    @SerializedName("name")
    val name: String,
    @SerializedName("isActive")
    val isActive: Boolean,
    @SerializedName("createdAt")
    val createdAt: String
)
