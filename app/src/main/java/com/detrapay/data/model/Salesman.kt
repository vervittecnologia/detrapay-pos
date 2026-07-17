package com.detrapay.data.model

import java.io.Serializable

data class Salesman(
    val id: Int?,
    val name: String,
    val phoneNumber: String? = null,
    val email: String? = null
) : Serializable
