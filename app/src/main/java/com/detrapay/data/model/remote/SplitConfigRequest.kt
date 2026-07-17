package com.detrapay.data.model.remote

import com.google.gson.annotations.SerializedName

data class SplitConfigRequest(
    @SerializedName("receivable_id")
    val receivableId: Int,
    val serial: String,
    val description: String? = null
)
