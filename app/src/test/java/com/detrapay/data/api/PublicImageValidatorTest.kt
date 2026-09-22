package com.detrapay.data.api

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.BufferedSource
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.io.IOException

class PublicImageValidatorTest {
    private val validator = PublicImageValidator()

    @Test
    fun `accepts a bounded image`() {
        val bytes = byteArrayOf(1, 2, 3)
        assertArrayEquals(bytes, validator.read(bytes.toResponseBody("image/png".toMediaType())))
    }

    @Test(expected = IOException::class)
    fun `rejects non image content`() {
        validator.read("<html>".toResponseBody("text/html".toMediaType()))
    }

    @Test(expected = IOException::class)
    fun `rejects oversized streaming body without content length`() {
        validator.read(streamingBody(ByteArray(2_097_153)))
    }

    private fun streamingBody(bytes: ByteArray) = object : ResponseBody() {
        override fun contentType() = "image/png".toMediaType()
        override fun contentLength() = -1L
        override fun source(): BufferedSource = Buffer().write(bytes)
    }
}
