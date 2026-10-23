package io.github.kevin321happy.httpdeck.core

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HttpDeckInterceptorTest {

    private val server = MockWebServer()
    private val store = MemoryRecordStore(HttpDeckConfig())

    @AfterTest
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun capturesRequestAndResponse() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"success":true}""")
        )
        server.start()

        val client = OkHttpClient.Builder()
            .addInterceptor(HttpDeckInterceptor(store, HttpDeckConfig()))
            .build()
        val request = Request.Builder()
            .url(server.url("/v1/ping"))
            .post("""{"a":1}""".toRequestBody("application/json".toMediaType()))
            .header("X-Trace", "abc")
            .build()

        client.newCall(request).execute().use { it.body?.string() }

        val record = store.snapshot().single()
        assertEquals("POST", record.method)
        assertTrue(record.url.endsWith("/v1/ping"))
        assertEquals(200, record.statusCode)
        assertEquals("""{"a":1}""", record.requestBody)
        assertEquals("""{"success":true}""", record.responseBody)
        assertEquals("abc", record.requestHeaders["X-Trace"])
    }
}