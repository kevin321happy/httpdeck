package io.github.kevin321happy.httpdeck.core

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale

object HttpDeckReplayFactory {

    /**
     * Re-issues the captured HTTP request through the same OkHttp client, so
     * the host app's interceptors can refresh auth/signature headers.
     *
     * Only GET/POST are supported in the first release; auth endpoints are
     * blocked. Body and URL stay exactly as captured.
     */
    fun build(record: HttpDeckRecord): Request? {
        val method = record.method.uppercase(Locale.US)
        if (method != "GET" && method != "POST") return null
        if (isAuthEndpoint(record.url)) return null

        val bodyText = record.requestBody
        if (!bodyText.isNullOrBlank() && bodyText.startsWith("<")) return null
        val contentType = record.requestHeaders.entries
            .firstOrNull { it.key.equals("Content-Type", true) }
            ?.value
            ?: "application/json"
        val mediaType = contentType.toMediaTypeOrNull()
        val body: RequestBody? = when {
            method == "GET" -> null
            bodyText.isNullOrBlank() -> "".toRequestBody(mediaType)
            else -> bodyText.toRequestBody(mediaType)
        }

        return runCatching {
            val builder = Request.Builder().url(record.url).method(method, body)
            record.requestHeaders.forEach { (key, value) ->
                if (!shouldSkipHeader(key)) builder.addHeader(key, value)
            }
            builder.build()
        }.getOrNull()
    }

    fun isAuthEndpoint(url: String): Boolean {
        val lower = url.lowercase(Locale.US)
        return lower.contains("oauth/token") ||
            lower.contains("refreshtoken") ||
            lower.contains("sms/send") ||
            lower.contains("code/send") ||
            lower.contains("/login")
    }

    private fun shouldSkipHeader(key: String): Boolean {
        val lower = key.lowercase(Locale.US)
        return lower == "authorization" ||
            lower == "content-type" ||
            lower == "content-length" ||
            lower == "host" ||
            lower == "connection" ||
            lower == "accept-encoding" ||
            lower == "referer" ||
            lower == "user-agent" ||
            lower == "accept-language" ||
            lower == "_t" ||
            lower == "_s" ||
            lower == "_ssv" ||
            lower.contains("encryption")
    }
}