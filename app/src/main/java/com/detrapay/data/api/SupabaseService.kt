package com.detrapay.data.api

import com.detrapay.data.model.remote.CalculateFeesResponse
import com.detrapay.data.model.remote.CardBrandIconResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SupabaseService {
    @GET("mobile/calculate-fees")
    suspend fun calculateFees(
        @Query("value") value: Double,
        @Query("payment_type") paymentType: String,
        @Query("bandeira") brand: String? = null
    ): Response<CalculateFeesResponse>

    @GET("mobile/card-brand-icons")
    suspend fun getCardBrandIcons(@Query("brands") brands: String): Response<List<CardBrandIconResponse>>
}
