package com.detrapay.ui.login

/**
 * Authentication result : success or error message.
 */
data class LoginResult(
    val success: Boolean? = null,
    val error: Int? = null
)