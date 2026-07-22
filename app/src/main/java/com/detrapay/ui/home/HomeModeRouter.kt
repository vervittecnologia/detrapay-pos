package com.detrapay.ui.home

import com.detrapay.data.model.SellerAppMode

object HomeModeRouter {
    fun shouldUseSimplifiedSurface(sellerAppMode: SellerAppMode): Boolean {
        return sellerAppMode == SellerAppMode.SIMPLIFIED
    }

    fun shouldUseDirectCheckoutSurface(sellerAppMode: SellerAppMode): Boolean {
        return sellerAppMode == SellerAppMode.DIRECT_CHECKOUT
    }

    fun shouldHideBottomNavigation(sellerAppMode: SellerAppMode): Boolean {
        return sellerAppMode == SellerAppMode.SIMPLIFIED || sellerAppMode == SellerAppMode.DIRECT_CHECKOUT
    }
}
