package com.detrapay.data.model

data class OrderDocument(
    val id: Int,
    val salesOrderId: Int,
    val fileName: String,
    val fileUrl: String,
    val previewUrl: String,
    val mimeType: String,
    val fileSizeKb: Int?,
    val createdAt: String?,
)
