package io.github.kevin321happy.httpdeck.core

import java.util.concurrent.CopyOnWriteArrayList

class MemoryRecordStore(private val config: HttpDeckConfig) {

    private val lock = Any()
    private val records = ArrayList<HttpDeckRecord>()
    private var totalBytes = 0L
    private val listeners = CopyOnWriteArrayList<(List<HttpDeckRecord>) -> Unit>()

    fun add(record: HttpDeckRecord) {
        val snapshot: List<HttpDeckRecord>
        synchronized(lock) {
            records.add(0, record)
            totalBytes += recordSize(record)
            while (records.size > config.maxRecords || totalBytes > config.maxTotalBytes) {
                if (records.isEmpty()) break
                val removed = records.removeAt(records.lastIndex)
                totalBytes -= recordSize(removed)
            }
            snapshot = ArrayList(records)
        }
        notifyListeners(snapshot)
    }

    fun snapshot(): List<HttpDeckRecord> = synchronized(lock) { ArrayList(records) }

    fun clear() {
        synchronized(lock) {
            records.clear()
            totalBytes = 0L
        }
        notifyListeners(emptyList())
    }

    fun addListener(listener: (List<HttpDeckRecord>) -> Unit) {
        listeners.add(listener)
        // An observer must never be able to break the install call.
        runCatching { listener(snapshot()) }
    }

    fun removeListener(listener: (List<HttpDeckRecord>) -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners(snapshot: List<HttpDeckRecord>) {
        listeners.forEach { listener ->
            runCatching { listener(snapshot) }
        }
    }

    private fun recordSize(record: HttpDeckRecord): Long {
        return (record.requestBody?.length ?: 0).toLong() +
            (record.responseBody?.length ?: 0).toLong() +
            1024L
    }
}