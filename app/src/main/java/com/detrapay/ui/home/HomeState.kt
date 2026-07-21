package com.detrapay.ui.home

import com.detrapay.data.model.Salesman
import com.detrapay.data.model.SellerAppMode

data class HomeState(
    val companyName: String,
    val companyDocument: String,
    val dispatcherName: String,
    val companyLogoKey: String?,
    val salesmen: List<Salesman>,
    val sellerAppMode: SellerAppMode
) {
    val isSimplifiedMode: Boolean
        get() = sellerAppMode.usesSimplifiedHome
}
