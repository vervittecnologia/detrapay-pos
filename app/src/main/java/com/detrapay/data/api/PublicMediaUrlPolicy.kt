package com.detrapay.data.api

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Inject

class PublicMediaUrlPolicy @Inject constructor() {
    fun validate(raw: String): HttpUrl {
        val url = raw.toHttpUrl()
        require(url.scheme == "https" && url.host.isNotBlank()) {
            "Public media URL must use HTTPS"
        }
        return url
    }
}
