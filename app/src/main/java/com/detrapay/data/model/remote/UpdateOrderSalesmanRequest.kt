package com.detrapay.data.model.remote

import com.google.gson.annotations.SerializedName

data class UpdateOrderSalesmanRequest(
    @SerializedName("salesman")
    val salesmanId: Int
)
