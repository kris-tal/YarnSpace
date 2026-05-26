package com.yarnspace.app.core.util

import com.yarnspace.app.BuildConfig
import java.net.URI

object UrlUtils {
    fun resolve(url: String?): String? {
        val raw = url?.trim().orEmpty()
        if (raw.isBlank()) return null

        return when {
            raw.startsWith("http://") || raw.startsWith("https://") -> {
                val parsed = runCatching { URI(raw) }.getOrNull()
                val host = parsed?.host
                if (host == "localhost" || host == "127.0.0.1" || host == "yarnspace_api" || host == "backend") {
                    val path = parsed.rawPath ?: return raw
                    val query = parsed.rawQuery?.let { "?$it" }.orEmpty()
                    BuildConfig.API_BASE_URL.trimEnd('/') + path + query
                } else {
                    raw
                }
            }
            raw.startsWith("/") -> BuildConfig.API_BASE_URL.trimEnd('/') + raw
            else -> null
        }
    }
}



