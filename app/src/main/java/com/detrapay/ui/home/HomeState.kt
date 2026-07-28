package com.detrapay.ui.home

import com.detrapay.data.model.Salesman

data class HomeState(
    val companyName: String,
    val companyDocument: String,
    val dispatcherName: String,
    val companyLogoKey: String?,
    val salesmen: List<Salesman>,
)
