package com.detrapay.ui.splash

/**
 * Authentication result : success or error message.
 */
data class SplashAuthResult(
    val authenticated: Boolean,
    val hasPreferredCompany: Boolean
)