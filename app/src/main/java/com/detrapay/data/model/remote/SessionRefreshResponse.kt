package com.detrapay.data.model.remote

import com.google.gson.annotations.SerializedName

data class SessionRefreshResponse(
    @SerializedName("jwt")
    val jwt: String? = null,
    @SerializedName("access_token")
    val accessToken: String? = null,
    @SerializedName("refresh_token")
    val refreshToken: String? = null,
    @SerializedName("expires_in")
    val expiresIn: Long? = null,
    @SerializedName("expires_at")
    val expiresAt: Long? = null,
    @SerializedName("token_type")
    val tokenType: String? = null,
    @SerializedName("session")
    val session: SessionPayload? = null,
) {
    fun resolvedAccessToken(): String = accessToken ?: jwt ?: session?.accessToken ?: session?.jwt.orEmpty()
    fun resolvedRefreshToken(): String? = refreshToken ?: session?.refreshToken
    fun resolvedExpiresIn(): Long? = expiresIn ?: session?.expiresIn
    fun resolvedExpiresAt(): Long? = expiresAt ?: session?.expiresAt
    fun resolvedTokenType(): String? = tokenType ?: session?.tokenType
}

data class SessionPayload(
    @SerializedName("jwt")
    val jwt: String? = null,
    @SerializedName("access_token")
    val accessToken: String? = null,
    @SerializedName("refresh_token")
    val refreshToken: String? = null,
    @SerializedName("expires_in")
    val expiresIn: Long? = null,
    @SerializedName("expires_at")
    val expiresAt: Long? = null,
    @SerializedName("token_type")
    val tokenType: String? = null,
)
