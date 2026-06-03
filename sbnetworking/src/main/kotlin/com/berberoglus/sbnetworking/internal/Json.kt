package com.berberoglus.sbnetworking.internal

import com.berberoglus.sbnetworking.HttpClientError
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.MediaType.Companion.toMediaType

/**
 * Shared kotlinx-serialization configuration used by the Retrofit converter and the dynamic decode
 * path. Marked `@PublishedApi internal` so the public `inline` [com.berberoglus.sbnetworking.HttpClient.execute]
 * can reference it without exposing it as public API.
 */
@PublishedApi
internal val appJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
}

/** Media type for the Retrofit kotlinx-serialization converter and dynamic request bodies. */
internal val JSON_MEDIA_TYPE = "application/json".toMediaType()

/** Decodes [bytes] to [T] or throws [HttpClientError.DecodingFailed] on any failure. */
@PublishedApi
internal inline fun <reified T> Json.decodeOrThrow(bytes: ByteArray): T =
    try {
        decodeFromString(serializer<T>(), bytes.decodeToString())
    } catch (e: Exception) {
        throw HttpClientError.DecodingFailed
    }
