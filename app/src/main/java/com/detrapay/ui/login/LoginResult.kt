package com.detrapay.ui.login

import com.detrapay.data.model.LoggedInUser

/**
 * Authentication result : success or error message.
 */
data class LoginResult(
    val success: LoggedInUser? = null,
    val error: Int? = null
)