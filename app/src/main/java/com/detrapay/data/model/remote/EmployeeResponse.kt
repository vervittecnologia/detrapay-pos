package com.detrapay.data.model.remote

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class EmployeeResponse(
    @SerializedName("data")
    val data: List<EmployeeItemResponse>,
)

@Serializable
data class EmployeeItemResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("username")
    val username: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: String
)