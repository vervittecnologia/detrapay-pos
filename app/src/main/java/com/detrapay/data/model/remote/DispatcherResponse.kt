package com.detrapay.data.model.remote

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class DispatcherResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String
)
