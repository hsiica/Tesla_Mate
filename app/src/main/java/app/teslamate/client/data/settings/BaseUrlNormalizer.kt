package app.teslamate.client.data.settings

import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * Accepts an origin such as `http://192.168.1.10:8080`.
 * A pasted `/api/v1` or `/api/v1/cars` suffix is removed so requests are not doubled.
 */
fun normalizeBaseUrl(input: String): String? {
    val trimmed = input.trim()
    if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) return null
    val parsed = runCatching { trimmed.toHttpUrl() }.getOrNull() ?: return null
    if (parsed.host.isBlank()) return null
    var path = parsed.encodedPath.trimEnd('/')
    for (suffix in listOf("/api/v1/cars", "/api/v1")) {
        if (path.endsWith(suffix)) {
            path = path.removeSuffix(suffix).trimEnd('/')
            break
        }
    }
    return parsed.newBuilder()
        .encodedPath(if (path.isEmpty()) "/" else path)
        .query(null)
        .fragment(null)
        .build()
        .toString()
        .trimEnd('/')
}
