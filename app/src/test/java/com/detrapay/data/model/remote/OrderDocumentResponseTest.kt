package com.detrapay.data.model.remote

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderDocumentResponseTest {

    @Test
    fun `document response accepts current snake case contract`() {
        val response = Gson().fromJson(
            """
            {
              "data": {
                "id": 12,
                "sales_order_id": 555,
                "file_name": "pedido_555.jpg",
                "file_url": "uploads/pedido_555.jpg",
                "download_url": "https://signed.example/pedido_555.jpg",
                "mime_type": "image/jpeg",
                "file_size": 240,
                "created_at": "2026-07-29T12:00:00Z"
              }
            }
            """.trimIndent(),
            OrderDocumentMutationResponse::class.java,
        )

        assertEquals(12, response.data.id)
        assertEquals(555, response.data.salesOrderId)
        assertEquals("pedido_555.jpg", response.data.fileName)
        assertEquals("https://signed.example/pedido_555.jpg", response.data.downloadUrl)
        assertEquals("image/jpeg", response.data.mimeType)
    }
}
