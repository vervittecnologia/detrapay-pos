package com.detrapay.data

class UnauthorizedException(
    val endpoint: String = "endpoint nao informado",
    val backendMessage: String? = null,
) : Exception(
    buildString {
        append("Sessao invalida ou expirada em ")
        append(endpoint)
        if (!backendMessage.isNullOrBlank()) {
            append(": ")
            append(backendMessage)
        }
    }
)
