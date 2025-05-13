package com.detrapay.data.model.remote

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val identifier: String,
    val password: String) {
}
