package io.github.kevin321happy.httpdeck.core

enum class HttpDeckSource {
    ANDROID,
    FLUTTER
}

data class HttpDeckRecord(
    val id: Long,
    val source: HttpDeckSource = HttpDeckSource.ANDROID,
    val method: String,
    val url: String,
    val statusCode: Int?,
    val durationMs: Long,
    val requestHeaders: Map<String, String>,
    val requestBody: String?,
    val responseHeaders: Map<String, String>,
    val responseBody: String?,
    val error: String?,
    val timestamp: Long,
    val replayed: Boolean = false
)