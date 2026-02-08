package com.detrapay.data.model.remote

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class CompanyListResponse(
    @SerializedName("data")
    val data: List<CompanyItemResponse>
)

@Serializable
data class CompanyItemResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("attributes")
    val attributes: CompanyAttributesResponse
)
