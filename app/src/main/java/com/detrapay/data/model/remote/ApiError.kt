package com.detrapay.data.model.remote

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.ResponseBody


class ApiError constructor(errorBody: ResponseBody?) {
    var message = "Algo de errado aconteceu"

    init {
        if (errorBody != null) {
            val gson = Gson()
            val errorResponse: ErrorObject = gson.fromJson(
                errorBody.string(),
                ErrorObject::class.java
            )
            this.message = errorResponse.error.message

        }
    }
}

@Serializable
data class ErrorObject(
    @SerializedName("error")
    var error: ErrorBodyContent,
)

@Serializable
data class ErrorBodyContent(
    @SerialName("status")
    val status: Int,
    @SerialName("message")
    val message: String
)