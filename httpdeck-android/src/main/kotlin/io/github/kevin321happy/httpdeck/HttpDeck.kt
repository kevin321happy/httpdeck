package io.github.kevin321happy.httpdeck

import android.app.Activity
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import io.github.kevin321happy.httpdeck.core.HttpDeckConfig
import io.github.kevin321happy.httpdeck.core.HttpDeckInterceptor
import io.github.kevin321happy.httpdeck.core.HttpDeckRecord
import io.github.kevin321happy.httpdeck.core.HttpDeckRedactor
import io.github.kevin321happy.httpdeck.core.HttpDeckReplayFactory
import io.github.kevin321happy.httpdeck.core.HttpDeckSource
import io.github.kevin321happy.httpdeck.core.MemoryRecordStore
import okhttp3.OkHttpClient

object HttpDeck {

    private const val MAX_SHARE_CHARS = 20_000
    private val mainHandler = Handler(Looper.getMainLooper())
    private val redactor = HttpDeckRedactor()

    @Volatile
    private var store: MemoryRecordStore? = null

    @Volatile
    private var replayClient: OkHttpClient? = null

    private var config = HttpDeckConfig()

    /**
     * Install the capture interceptor into the app's OkHttp builder.
     *
     * Call this before `builder.build()`. The interceptor is pass-through when
     * HttpDeck is disabled.
     */
    fun install(
        @Suppress("UNUSED_PARAMETER") application: Application,
        builder: OkHttpClient.Builder,
        config: HttpDeckConfig = HttpDeckConfig()
    ) {
        if (!config.enabled) return
        if (!BuildConfig.DEBUG && !config.enableInRelease) return

        this.config = config
        val newStore = MemoryRecordStore(config)
        store = newStore
        builder.addInterceptor(HttpDeckInterceptor(newStore, config))
        replayClient = builder.build()
    }

    fun isInstalled(): Boolean = store != null

    fun records(): List<HttpDeckRecord> = store?.snapshot() ?: emptyList()

    fun addListener(listener: (List<HttpDeckRecord>) -> Unit) {
        val current = store
        if (current == null) {
            listener(emptyList())
        } else {
            current.addListener(listener)
        }
    }

    fun removeListener(listener: (List<HttpDeckRecord>) -> Unit) {
        store?.removeListener(listener)
    }

    fun clear() {
        store?.clear()
    }

    fun show(activity: Activity) {
        activity.startActivity(Intent(activity, HttpDeckActivity::class.java))
    }

    fun replay(activity: Activity, record: HttpDeckRecord, callback: (Boolean, String) -> Unit) {
        val request = HttpDeckReplayFactory.build(record)
        if (request == null) {
            callback(false, "This request cannot be re-requested.")
            return
        }
        val client = replayClient
        if (client == null) {
            callback(false, "HttpDeck is not installed.")
            return
        }
        Thread({
            try {
                client.newCall(request).execute().use { response ->
                    val body = response.peekBody(config.maxBodyBytes.toLong()).string()
                    mainHandler.post {
                        callback(true, "HTTP ${response.code}\n$body")
                    }
                }
            } catch (t: Throwable) {
                mainHandler.post {
                    callback(false, t.message ?: t.javaClass.simpleName)
                }
            }
        }, "httpdeck-replay").start()
    }

    fun curl(record: HttpDeckRecord): String {
        val headers = redactor.headers(record.requestHeaders)
        val sb = StringBuilder("curl -X ").append(record.method)
            .append(" '").append(escape(redactor.url(record.url))).append("'")
        headers.forEach { (key, value) ->
            if (!skipCurlHeader(key)) {
                sb.append(" \\\n  -H '").append(escape("$key: $value")).append("'")
            }
        }
        record.requestBody?.takeIf { it.isNotBlank() }?.let {
            sb.append(" \\\n  --data-raw '").append(escape(redactor.body(it) ?: it)).append("'")
        }
        return sb.toString()
    }

    fun shareText(record: HttpDeckRecord): String {
        val sb = StringBuilder()
        sb.appendLine("HttpDeck ${record.method} ${redactor.url(record.url)}")
        sb.appendLine(
            "status=${record.statusCode ?: "-"} duration=${record.durationMs}ms " +
                "source=${sourceLabel(record.source)} time=${record.timestamp}"
        )
        if (!record.error.isNullOrBlank()) sb.appendLine("error=${redactor.body(record.error)}")
        sb.appendLine()
        sb.appendLine("Request headers:")
        sb.appendLine(displayHeaders(record.requestHeaders))
        sb.appendLine("Request body:")
        sb.appendLine(redactor.body(record.requestBody) ?: "(empty)")
        sb.appendLine()
        sb.appendLine("Response headers:")
        sb.appendLine(displayHeaders(record.responseHeaders))
        sb.appendLine("Response body:")
        sb.appendLine(redactor.body(record.responseBody) ?: "(empty)")
        sb.appendLine()
        sb.appendLine("(redacted)")
        val text = sb.toString()
        return if (text.length <= MAX_SHARE_CHARS) text else {
            text.take(MAX_SHARE_CHARS) + "\n...[truncated]"
        }
    }

    fun displayHeaders(headers: Map<String, String>): String {
        if (headers.isEmpty()) return "(empty)"
        return redactor.headers(headers).entries.joinToString("\n") { "${it.key}: ${it.value}" }
    }

    fun displayBody(body: String?): String = redactor.body(body) ?: "(empty)"

    fun displayUrl(url: String): String = redactor.url(url)

    fun sourceLabel(source: HttpDeckSource): String =
        if (source == HttpDeckSource.FLUTTER) "flutter" else "android"

    fun copyText(activity: Activity, label: String, text: String) {
        val manager = activity.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        manager?.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    private fun skipCurlHeader(key: String): Boolean {
        val lower = key.lowercase()
        return lower == "content-length" ||
            lower == "host" ||
            lower == "connection" ||
            lower == "accept-encoding"
    }

    private fun escape(value: String): String = value.replace("'", "'\\''")
}