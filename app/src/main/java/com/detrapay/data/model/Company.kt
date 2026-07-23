package com.detrapay.data.model

import java.io.Serializable

data class Company(
    val id: Int,
    val name: String,
    val logoUrl: String? = null,
    val logoKey: String? = null,
    val sellerAppMode: String? = null
) : Serializable {
    val normalizedSellerAppMode: SellerAppMode
        get() = SellerAppMode.from(sellerAppMode)
}
