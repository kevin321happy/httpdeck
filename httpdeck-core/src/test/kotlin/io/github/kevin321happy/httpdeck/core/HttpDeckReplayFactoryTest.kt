package io.github.kevin321happy.httpdeck.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class HttpDeckReplayFactoryTest {

    private fun record(
        method: String = "POST",
        url: String = "https://api.example.com/v1/orders",
        body: String? = """{"a":1}""",
        headers: Map<String, String> = mapOf(
            "Authorization" to "Bearer old",
            "_t" to "123",
            "_s" to "sign",
            "Content-Type" to "application/json",
            "X-Business" to "b1"
        )
    ): HttpDeckRecord {
        return HttpDeckRecord(
            id = 1,
            method = method,
            url = url,
            statusCode = 200,
            durationMs = 10,
            requestHeaders = headers,
            requestBody = body,
            responseHeaders = emptyMap(),
            responseBody = "{}",
            error = null,
            timestamp = 1
        )
    }

    @Test
    fun rebuildsRequestWithoutOldAuthHeaders() {
        val request = assertNotNull(HttpDeckReplayFactory.build(record()))
        assertEquals("POST", request.method)
        assertEquals("""{"a":1}""", request.body?.let {
            val buffer = okio.Buffer()
            it.writeTo(buffer)
            buffer.readUtf8()
        })
        assertNull(request.header("Authorization"))
        assertNull(request.header("_t"))
        assertNull(request.header("_s"))
        assertEquals("b1", request.header("X-Business"))
    }

    @Test
    fun blocksAuthAndUnsupportedMethods() {
        assertNull(HttpDeckReplayFactory.build(record(url = "https://api.example.com/oauth/token")))
        assertNull(HttpDeckReplayFactory.build(record(method = "PUT")))
    }
}