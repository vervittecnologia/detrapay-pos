package com.detrapay.data.api

import okhttp3.Authenticator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PublicMediaClientFactoryTest {
    @Test
    fun `public client has no auth hooks or application interceptors`() {
        val client = PublicMediaClientFactory.create()

        assertTrue(client.interceptors.isEmpty())
        assertEquals(Authenticator.NONE, client.authenticator)
    }
}
