package com.detrapay.data.model.remote

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class LoginCompanyResponse(
    val id: Int,
    val name: String,
    @SerializedName("logo_url")
    val logoUrl: String? = null,
    @SerializedName("seller_app_mode")
    val sellerAppMode: String? = null
)
