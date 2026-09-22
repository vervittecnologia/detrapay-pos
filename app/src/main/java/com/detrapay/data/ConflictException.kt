package com.detrapay.data

class ConflictException(
    val code: String,
) : Exception(code)
