package com.detrapay.data.model

enum class SellerAppMode(val apiValue: String) {
    DIRECT_CHECKOUT("direct_checkout");

    companion object {
        fun from(value: String?): SellerAppMode {
            return DIRECT_CHECKOUT
        }

        fun mostSpecific(values: Iterable<SellerAppMode>): SellerAppMode {
            return DIRECT_CHECKOUT
        }
    }
}
