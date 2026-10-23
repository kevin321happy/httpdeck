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

        // Narrow catch: only the business call is allowed to decide the outcome.
        // A capture failure must never replace a successful response or hide the
        // original business exception.
        val response = try {
            chain.proceed(request)
        } catch (t: Throwable) {
            runCatching { recordFailure(request, requestBody, startNs, t) }
            throw t
        }

        // Capture is best-effort; all failures stay inside recordSuccess.
        runCatching { recordSuccess(request, requestBody, startNs, response) }
        return response
    }

    private fun recordSuccess(
        request: Request,
        requestBody: String?,
        startNs: Long,
        response: Response
    ) {
        val durationMs = (System.nanoTime() - startNs) / 1_000_000
        runCatching {
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
                    responseBody = readResponseBody(response),
                    error = null,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    private fun recordFailure(
        request: Request,
        requestBody: String?,
        startNs: Long,
        error: Throwable
    ) {
        val durationMs = (System.nanoTime() - startNs) / 1_000_000
        runCatching {
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
                    error = error.message ?: error.javaClass.simpleName,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    private fun readRequestBody(request: Request): String? {
        val body = request.body ?: return null
        val type = body.contentType()?.toString() ?: ""
        if (body.isOneShot() || body.isDuplex()) return "<$type one-shot/duplex body>"
        if (isStreaming(type) || isMultipart(type)) return "<$type stream/multipart body>"
        val length = runCatching { body.contentLength() }.getOrDefault(-1L)
        if (length > config.maxBodyBytes) return "<body too large: $length bytes>"
        if (length < 0) return "<unknown length body>"
        if (!isTextual(type)) return "<binary: $type, $length bytes>"
        return runCatching {
            val buffer = Buffer()
            body.writeTo(buffer)
            cap(buffer.readUtf8())
        }.getOrElse { "<body unavailable: ${it.message}>" }
    }

    private fun readResponseBody(response: Response): String? {
        if (response.request.method.equals("HEAD", ignoreCase = true)) return "<no body: HEAD>"
        if (response.code == 204 || response.code == 304) return "<no body: ${response.code}>"
        val body = response.body ?: return null
        val type = body.contentType()?.toString() ?: ""
        if (isStreaming(type)) return "<streaming: $type>"
        val length = runCatching { body.contentLength() }.getOrDefault(-1L)
        if (length > config.maxBodyBytes) return "<body too large: $length bytes>"
        if (!isTextual(type)) return "<binary: $type, $length bytes>"
        return runCatching {
            cap(response.peekBody(config.maxBodyBytes.toLong()).string())
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

    private fun isMultipart(type: String): Boolean {
        return type.lowercase(Locale.US).contains("multipart/")
    }

    private fun isStreaming(type: String): Boolean {
        val lower = type.lowercase(Locale.US)
        return lower.contains("text/event-stream") ||
            lower.contains("application/x-ndjson") ||
            lower.contains("application/stream+json") ||
            lower.contains("application/grpc") ||
            lower.contains("application/octet-stream")
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