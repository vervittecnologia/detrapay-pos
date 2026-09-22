package com.detrapay.data.api

import org.junit.Assert.assertEquals
import org.junit.Test

class PublicMediaUrlPolicyTest {
    private val policy = PublicMediaUrlPolicy()

    @Test
    fun `accepts https image URL`() {
        assertEquals("https", policy.validate("https://cdn.example/logo.png").scheme)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects http image URL`() {
        policy.validate("http://cdn.example/logo.png")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects non web URL`() {
        policy.validate("file:///data/logo.png")
    }
}
