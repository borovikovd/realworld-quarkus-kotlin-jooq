package com.example.application.readmodel

data class StoredIdempotencyKey(
    val requestPath: String,
    val responseStatus: Int?,
    val responseBody: String?,
) {
    val isProcessing: Boolean get() = responseBody == null
}
