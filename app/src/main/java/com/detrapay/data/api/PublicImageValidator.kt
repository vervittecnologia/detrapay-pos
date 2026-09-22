package com.detrapay.data.api

import okio.Buffer
import okhttp3.ResponseBody
import java.io.IOException
import javax.inject.Inject

class PublicImageValidator @Inject constructor() {
    fun read(
        body: ResponseBody,
        contentType: String? = body.contentType()?.toString(),
        maxBytes: Long = DEFAULT_MAX_BYTES,
    ): ByteArray {
        val normalizedType = contentType?.substringBefore(';')?.trim()?.lowercase()
        if (normalizedType !in ALLOWED_TYPES) throw IOException("Unsupported image content type")
        if (body.contentLength() > maxBytes) throw IOException("Image exceeds size limit")

        val source = body.source()
        val buffer = Buffer()
        var total = 0L
        while (total <= maxBytes) {
            val read = source.read(buffer, minOf(8_192L, maxBytes + 1L - total))
            if (read == -1L) break
            total += read
        }
        if (total > maxBytes) throw IOException("Image exceeds size limit")
        return buffer.readByteArray()
    }

    companion object {
        const val DEFAULT_MAX_BYTES = 2_097_152L
        private val ALLOWED_TYPES = setOf(
            "image/png",
            "image/jpeg",
            "image/webp",
            "image/svg+xml",
        )
    }
}
