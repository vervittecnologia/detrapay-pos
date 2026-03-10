package com.detrapay.data.model.remote

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class SalespeopleResponse(
    val data: List<SalespersonResponse>,
    val meta: MetaResponse? = null
)

@Serializable
data class SalespersonResponse(
    val id: Int,
    val attributes: SalespersonAttributesResponse? = null,
    val name: String? = null,
    @SerializedName(value = "phoneNumber", alternate = ["phone_number"])
    val phoneNumber: String? = null,
    val email: String? = null,
    @SerializedName(value = "isActive", alternate = ["is_active"])
    val isActive: Boolean? = null,
    val documentId: String? = null
)

@Serializable
data class SalespersonAttributesResponse(
    val name: String,
    val cpf: String? = null,
    @SerializedName(value = "phoneNumber", alternate = ["phone_number"])
    val phoneNumber: String? = null,
    val email: String? = null,
    @SerializedName("company_id")
    val companyId: Int? = null,
    @SerializedName(value = "isActive", alternate = ["is_active"])
    val isActive: Boolean
)
