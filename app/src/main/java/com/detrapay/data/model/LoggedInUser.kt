package com.detrapay.data.model

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */
data class LoggedInUser(
    val id: Int,
    val sessionToken: String,
    val displayName: String,
    val username: String,
    val email: String
)