package com.detrapay.data.model.remote

import kotlinx.serialization.Serializable

@Serializable
data class LoginCompanyResponse(
    val id: Int,
    val name: String
)
