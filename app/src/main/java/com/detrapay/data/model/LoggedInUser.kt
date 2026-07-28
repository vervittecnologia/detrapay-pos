package com.detrapay.data.model

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */
data class LoggedInUser(
    val id: String,
    val sessionToken: String,
    val displayName: String,
    val username: String,
    val cpfCnpj: String,
    val email: String,
    val companies: List<Company>,
    val dispatchers: List<Dispatcher>,
    val salesmen: List<Salesman>,
    val appMode: String = APP_MODE_DIRECT_CHECKOUT
) {
    val activeSellerAppMode: SellerAppMode
        get() = SellerAppMode.DIRECT_CHECKOUT

    companion object {
        const val APP_MODE_DIRECT_CHECKOUT = "direct_checkout"
    }
}
