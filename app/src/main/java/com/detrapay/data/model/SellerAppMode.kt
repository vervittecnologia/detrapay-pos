package com.detrapay.data.model

enum class SellerAppMode(val apiValue: String) {
    COMPLETE("complete"),
    SIMPLIFIED("simplified"),
    DIRECT_CHECKOUT("direct_checkout");

    val usesSimplifiedHome: Boolean
        get() = this == SIMPLIFIED || this == DIRECT_CHECKOUT

    companion object {
        fun from(value: String?): SellerAppMode {
            return when (value?.trim()?.lowercase()) {
                "simplified" -> SIMPLIFIED
                "direct_checkout" -> DIRECT_CHECKOUT
                "standard", "complete", null, "" -> COMPLETE
                else -> COMPLETE
            }
        }

        fun mostSpecific(values: Iterable<SellerAppMode>): SellerAppMode {
            return DIRECT_CHECKOUT
        }
    }
}
