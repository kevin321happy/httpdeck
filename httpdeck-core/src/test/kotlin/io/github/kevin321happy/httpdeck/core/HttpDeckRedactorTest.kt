package io.github.kevin321happy.httpdeck.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HttpDeckRedactorTest {

    private val redactor = HttpDeckRedactor()

    @Test
    fun masksSensitiveHeaders() {
        val headers = redactor.headers(
            mapOf(
                "Authorization" to "Bearer abcdefghijklmnop",
                "Content-Type" to "application/json"
            )
        )
        assertEquals("Bear****mnop", headers["Authorization"])
        assertEquals("application/json", headers["Content-Type"])
    }

    @Test
    fun masksTokenPasswordAndPhoneInJson() {
        val body = """{"token":"abcdefgh12345678","phone":"13800138000","pageNum":1}"""
        val result = redactor.body(body)!!
        assertFalse(result.contains("abcdefgh12345678"))
        assertFalse(result.contains("13800138000"))
        assertTrue(result.contains("pageNum"))
    }

    @Test
    fun masksSensitiveQueryValues() {
        val result = redactor.url("https://api.example.com/x?token=abcdefgh12345678&page=1")
        assertFalse(result.contains("abcdefgh12345678"))
        assertTrue(result.contains("page=1"))
    }
}