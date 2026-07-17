package com.detrapay.data.model.remote

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class VehicleTypeListResponse(
    @SerializedName("data")
    val data: List<VehicleTypeItemResponse>,
    @SerializedName("meta")
    val meta: MetaResponse? = null
)

@Serializable
data class VehicleTypeItemResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("documentId")
    val documentId: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("attributes")
    val attributes: VehicleTypeAttributes? = null
)

@Serializable
data class VehicleTypeAttributes(
    @SerializedName("name")
    val name: String,
    @SerializedName("isActive")
    val isActive: Boolean? = null,
    @SerializedName("createdAt")
    val createdAt: String? = null
)
