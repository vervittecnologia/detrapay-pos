package com.detrapay.ui.login

data class LoginFormState(
    val cnpjError: Int? = null,
    val passwordError: Int? = null,
    val isDataValid: Boolean = false
)