package com.detrapay.data.model.remote

import com.google.gson.annotations.SerializedName

data class RefreshSessionRequest(
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("refreshToken")
    val refreshTokenAlias: String = refreshToken,
)
