package com.detrapay.data.model.remote

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    @SerializedName("jwt")
    var token: String,
    @SerializedName("user")
    var user: UserResponse,
    @SerializedName("companies")
    var companies: List<LoginCompanyResponse>,
    @SerializedName("dispatchers")
    var dispatchers: List<DispatcherResponse>,
    @SerializedName("salesmen")
    var salesmen: List<LoginSalesmanResponse>
)

@Serializable
data class UserResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("documentId")
    val documentId: String,
    @SerializedName("username")
    val username: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("phoneNumber")
    val phoneNumber: String?,
    @SerializedName("cpf_cnpj")
    val cpf_cnpj: String,
    @SerializedName("blocked")
    val blocked: Boolean,
    @SerializedName("role")
    val role: RoleResponse
)

@Serializable
data class RoleResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("type")
    val type: String
)

@Serializable
data class LoginSalesmanResponse(
    @SerializedName("id")
    val id: Int?,
    @SerializedName("name")
    val name: String
)
