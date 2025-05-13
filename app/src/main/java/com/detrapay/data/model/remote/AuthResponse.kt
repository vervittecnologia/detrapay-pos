package com.detrapay.data.model.remote

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    @SerializedName("jwt")
    var token: String,
    @SerializedName("user")
    var user: UserResponse,
)

@Serializable
data class UserResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("username")
    val username: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: String
)