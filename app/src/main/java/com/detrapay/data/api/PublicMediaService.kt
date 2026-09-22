package com.detrapay.data.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface PublicMediaService {
    @GET
    suspend fun download(@Url url: String): Response<ResponseBody>
}
