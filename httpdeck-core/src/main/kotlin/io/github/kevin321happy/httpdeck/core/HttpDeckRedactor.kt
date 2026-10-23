package io.github.kevin321happy.httpdeck.core

import java.util.Locale

class HttpDeckRedactor {

    private val sensitiveJsonRegex = Regex(
        "\"([^\"]*(?:token|password|passwd|pwd|phone|mobile|idcard|idno|sign|secret|_s)[^\"]*)\"\\s*:\\s*\"([^\"]*)\"",
        RegexOption.IGNORE_CASE
    )
    private val sensitiveQueryRegex = Regex(
        "([?&](?:token|access_token|accessToken|_s|sign|phone|mobile)=)([^&]+)",
        RegexOption.IGNORE_CASE
    )
    private val phoneRegex = Regex("(?<!\\d)(1[3-9]\\d{9})(?!\\d)")

    fun headers(headers: Map<String, String>): Map<String, String> {
        return headers.mapValues { (key, value) ->
            if (isSensitiveHeader(key)) mask(value) else value
        }
    }

    fun body(body: String?): String? {
        if (body.isNullOrBlank()) return body
        var result = sensitiveJsonRegex.replace(body) { match ->
            val key = match.groupValues[1]
            "\"$key\":\"${mask(match.groupValues[2])}\""
        }
        result = phoneRegex.replace(result) { maskPhone(it.value) }
        return result
    }

    fun url(url: String): String {
        var result = sensitiveQueryRegex.replace(url) { match ->
            match.groupValues[1] + mask(match.groupValues[2])
        }
        result = phoneRegex.replace(result) { maskPhone(it.value) }
        return result
    }

    private fun isSensitiveHeader(key: String): Boolean {
        val lower = key.lowercase(Locale.US)
        return lower == "authorization" ||
            lower == "cookie" ||
            lower == "set-cookie" ||
            lower == "_s" ||
            lower == "sign" ||
            lower.contains("token") ||
            lower.contains("password") ||
            lower.contains("secret")
    }

    private fun mask(value: String): String {
        if (value.isBlank()) return value
        if (value.length <= 8) return "****"
        return value.take(4) + "****" + value.takeLast(4)
    }

    private fun maskPhone(value: String): String {
        if (value.length != 11) return "****"
        return value.take(3) + "****" + value.takeLast(4)
    }
}