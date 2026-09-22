package com.detrapay.data.api

import com.detrapay.ui.util.Logger
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject

class SafeHttpLogger @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startedAt = System.nanoTime()
        return try {
            chain.proceed(request).also { response ->
                Logger.d(format(request, response.code, elapsedMillis(startedAt)))
            }
        } catch (error: Exception) {
            Logger.d(format(request, null, elapsedMillis(startedAt)))
            throw error
        }
    }

    fun format(request: Request, status: Int?, durationMs: Long): String =
        "HTTP ${request.method} ${request.url.encodedPath} ${status ?: "FAILED"} ${durationMs}ms"

    private fun elapsedMillis(startedAt: Long): Long =
        (System.nanoTime() - startedAt) / 1_000_000L
}
