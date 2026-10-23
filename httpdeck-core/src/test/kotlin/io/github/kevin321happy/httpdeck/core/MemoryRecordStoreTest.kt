package io.github.kevin321happy.httpdeck.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MemoryRecordStoreTest {

    private fun record(id: Long, body: String = "{}"): HttpDeckRecord {
        return HttpDeckRecord(
            id = id,
            method = "GET",
            url = "https://api.example.com/$id",
            statusCode = 200,
            durationMs = 1,
            requestHeaders = emptyMap(),
            requestBody = null,
            responseHeaders = emptyMap(),
            responseBody = body,
            error = null,
            timestamp = id
        )
    }

    @Test
    fun keepsNewestRecordsWithinLimit() {
        val store = MemoryRecordStore(HttpDeckConfig(maxRecords = 2))
        store.add(record(1))
        store.add(record(2))
        store.add(record(3))

        val snapshot = store.snapshot()
        assertEquals(2, snapshot.size)
        assertEquals(3, snapshot[0].id)
        assertEquals(2, snapshot[1].id)
    }

    @Test
    fun listenerExceptionDoesNotEscapeToCapturePath() {
        val store = MemoryRecordStore(HttpDeckConfig())
        store.addListener { error("listener boom") }
        store.add(record(1))
        assertEquals(1, store.snapshot().size)
    }

    @Test
    fun notifiesListenersAndClears() {
        val store = MemoryRecordStore(HttpDeckConfig())
        var latest = emptyList<HttpDeckRecord>()
        store.addListener { latest = it }
        store.add(record(1))

        assertEquals(1, latest.size)
        store.clear()
        assertTrue(latest.isEmpty())
    }
}