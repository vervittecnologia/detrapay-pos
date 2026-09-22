package com.detrapay.data.api

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeHttpLoggerTest {
    @Test
    fun `diagnostic contains metadata but omits credentials query and body`() {
        val request = Request.Builder()
            .url("https://example.test/auth/local?access_token=jwt-value")
            .header("Authorization", "Bearer jwt-value")
            .post("{\"password\":\"secret\"}".toRequestBody("application/json".toMediaType()))
            .build()

        val line = SafeHttpLogger().format(request, 200, 15)

        assertTrue(line.contains("POST /auth/local 200 15ms"))
        assertFalse(line.contains("secret"))
        assertFalse(line.contains("jwt-value"))
        assertFalse(line.contains("Authorization"))
        assertFalse(line.contains("access_token"))
    }
}
