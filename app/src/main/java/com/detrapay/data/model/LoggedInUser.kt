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
    val appMode: String = APP_MODE_COMPLETE
) {
    val activeSellerAppMode: SellerAppMode
        get() = SellerAppMode.mostSpecific(
            companies.map { it.normalizedSellerAppMode } + SellerAppMode.from(appMode)
        )

    val isSimplifiedMode: Boolean
        get() = activeSellerAppMode.usesSimplifiedHome

    companion object {
        const val APP_MODE_COMPLETE = "complete"
        const val APP_MODE_SIMPLIFIED = "simplified"
        const val APP_MODE_DIRECT_CHECKOUT = "direct_checkout"
    }
}
