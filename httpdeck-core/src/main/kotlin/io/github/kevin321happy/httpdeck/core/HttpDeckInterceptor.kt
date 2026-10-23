package io.github.kevin321happy.httpdeck.core

import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

class HttpDeckInterceptor(
    private val store: MemoryRecordStore,
    private val config: HttpDeckConfig
) : Interceptor {

    private val nextId = AtomicLong(1L)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startNs = System.nanoTime()
        val requestBody = runCatching { readRequestBody(request) }.getOrNull()
        return try {
            val response = chain.proceed(request)
            val durationMs = (System.nanoTime() - startNs) / 1_000_000
            store.add(
                HttpDeckRecord(
                    id = nextId.getAndIncrement(),
                    method = request.method,
                    url = request.url.toString(),
                    statusCode = response.code,
                    durationMs = durationMs,
                    requestHeaders = headersToMap(request.headers),
                    requestBody = requestBody,
                    responseHeaders = headersToMap(response.headers),
                    responseBody = runCatching { readResponseBody(response) }.getOrNull(),
                    error = null,
                    timestamp = System.currentTimeMillis()
                )
            )
            response
        } catch (t: Throwable) {
            val durationMs = (System.nanoTime() - startNs) / 1_000_000
            store.add(
                HttpDeckRecord(
                    id = nextId.getAndIncrement(),
                    method = request.method,
                    url = request.url.toString(),
                    statusCode = null,
                    durationMs = durationMs,
                    requestHeaders = headersToMap(request.headers),
                    requestBody = requestBody,
                    responseHeaders = emptyMap(),
                    responseBody = null,
                    error = t.message ?: t.javaClass.simpleName,
                    timestamp = System.currentTimeMillis()
                )
            )
            throw t
        }
    }

    private fun readRequestBody(request: Request): String? {
        val body = request.body ?: return null
        val type = body.contentType()?.toString() ?: ""
        if (body.isOneShot() || body.isDuplex()) return "<$type one-shot/duplex body>"
        val length = runCatching { body.contentLength() }.getOrDefault(-1L)
        if (length > config.maxBodyBytes) return "<body too large: $length bytes>"
        if (length < 0) return "<unknown length body>"
        return runCatching {
            val buffer = Buffer()
            body.writeTo(buffer)
            val text = buffer.readUtf8()
            if (isTextual(type)) cap(text) else "<binary: $type, $length bytes>"
        }.getOrElse { "<body unavailable: ${it.message}>" }
    }

    private fun readResponseBody(response: Response): String? {
        val body = response.body ?: return null
        val type = body.contentType()?.toString() ?: ""
        val length = runCatching { body.contentLength() }.getOrDefault(-1L)
        if (!isTextual(type)) return "<binary: $type, $length bytes>"
        return runCatching {
            val text = response.peekBody(config.maxBodyBytes.toLong()).string()
            if (length > config.maxBodyBytes) {
                cap(text) + "\n...[truncated, total=$length bytes]"
            } else {
                cap(text)
            }
        }.getOrElse { "<body unavailable: ${it.message}>" }
    }

    private fun cap(text: String): String {
        if (text.length <= config.maxBodyBytes) return text
        return text.take(config.maxBodyBytes) + "\n...[truncated]"
    }

    private fun headersToMap(headers: Headers): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        for (name in headers.names()) {
            map[name] = headers.values(name).joinToString(", ")
        }
        return map
    }

    private fun isTextual(type: String): Boolean {
        val lower = type.lowercase(Locale.US)
        return lower.isEmpty() ||
            lower.startsWith("text/") ||
            lower.contains("json") ||
            lower.contains("xml") ||
            lower.contains("x-www-form-urlencoded") ||
            lower.contains("javascript") ||
            lower.contains("graphql")
    }
}