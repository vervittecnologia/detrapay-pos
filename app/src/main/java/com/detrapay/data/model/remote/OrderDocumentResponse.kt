package com.detrapay.data.model.remote

import com.google.gson.annotations.SerializedName

data class OrderDocumentResponse(
    val id: Int,
    @SerializedName(value = "sales_order_id", alternate = ["salesOrderId"])
    val salesOrderId: Int,
    @SerializedName(value = "file_name", alternate = ["fileName"])
    val fileName: String?,
    @SerializedName(value = "file_url", alternate = ["fileUrl"])
    val fileUrl: String?,
    @SerializedName(value = "download_url", alternate = ["downloadUrl", "signed_url", "url"])
    val downloadUrl: String?,
    @SerializedName(value = "mime_type", alternate = ["mimeType"])
    val mimeType: String?,
    @SerializedName(value = "file_size", alternate = ["fileSize"])
    val fileSizeKb: Int?,
    @SerializedName(value = "created_at", alternate = ["createdAt"])
    val createdAt: String?,
)

data class OrderDocumentListResponse(
    val data: List<OrderDocumentResponse>,
)

data class OrderDocumentMutationResponse(
    val data: OrderDocumentResponse,
)
