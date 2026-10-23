package io.github.kevin321happy.httpdeck

import android.app.Activity
import android.app.Application
import io.github.kevin321happy.httpdeck.core.HttpDeckConfig
import io.github.kevin321happy.httpdeck.core.HttpDeckRecord
import io.github.kevin321happy.httpdeck.core.HttpDeckSource
import okhttp3.OkHttpClient

/**
 * Release no-op artifact. Include this in releaseImplementation for public
 * builds; all APIs are intentionally empty.
 */
object HttpDeck {

    fun install(
        @Suppress("UNUSED_PARAMETER") application: Application,
        @Suppress("UNUSED_PARAMETER") builder: OkHttpClient.Builder,
        @Suppress("UNUSED_PARAMETER") config: HttpDeckConfig = HttpDeckConfig()
    ) = Unit

    fun isInstalled(): Boolean = false

    fun records(): List<HttpDeckRecord> = emptyList()

    fun addListener(listener: (List<HttpDeckRecord>) -> Unit) {
        listener(emptyList())
    }

    fun removeListener(listener: (List<HttpDeckRecord>) -> Unit) = Unit

    fun clear() = Unit

    fun show(@Suppress("UNUSED_PARAMETER") activity: Activity) = Unit

    fun replay(
        @Suppress("UNUSED_PARAMETER") activity: Activity,
        @Suppress("UNUSED_PARAMETER") record: HttpDeckRecord,
        callback: (Boolean, String) -> Unit
    ) {
        callback(false, "HttpDeck is not included in this build.")
    }

    fun curl(@Suppress("UNUSED_PARAMETER") record: HttpDeckRecord): String = ""

    fun shareText(@Suppress("UNUSED_PARAMETER") record: HttpDeckRecord): String = ""

    fun displayUrl(url: String): String = url

    fun displayHeaders(headers: Map<String, String>): String = headers.toString()

    fun displayBody(body: String?): String = body ?: "(empty)"

    fun sourceLabel(source: HttpDeckSource): String =
        if (source == HttpDeckSource.FLUTTER) "flutter" else "android"
}