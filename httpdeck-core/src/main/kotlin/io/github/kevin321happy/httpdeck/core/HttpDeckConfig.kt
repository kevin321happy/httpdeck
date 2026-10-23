package io.github.kevin321happy.httpdeck.core

data class HttpDeckConfig(
    val enabled: Boolean = true,
    /**
     * Release builds are disabled by default. Only enable this in a controlled
     * internal/field build, never in a public store build.
     */
    val enableInRelease: Boolean = false,
    val maxRecords: Int = 300,
    val maxBodyBytes: Int = 64 * 1024,
    val maxTotalBytes: Long = 12L * 1024 * 1024
)