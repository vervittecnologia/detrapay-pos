package com.detrapay.data.model.remote

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class MetaResponse(
    @SerializedName("pagination")
    val pagination: PaginationResponse
)
