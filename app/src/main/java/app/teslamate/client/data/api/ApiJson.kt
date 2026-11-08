package app.teslamate.client.data.api

import kotlinx.serialization.json.Json

/**
 * TeslaMateApi sometimes adds fields between releases, and a few numeric columns
 * have been null in older responses. Coerce those nulls onto Kotlin defaults.
 * Nullable fields (energy, temperatures) stay null so "unknown" is not shown as zero.
 */
object ApiJson {
    val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }
}
