package com.berberoglus.sbnetworking.support

import com.berberoglus.sbnetworking.ModelConvertible
import kotlinx.serialization.Serializable

@Serializable
data class ITunesSearchResponseDto(
    val resultCount: Int,
    val results: List<ITunesEntityDto>,
) : ModelConvertible<ITunesSearchResult> {
    override fun toDomain(): ITunesSearchResult =
        ITunesSearchResult(resultCount = resultCount, items = results.map { it.toDomain() })
}

@Serializable
data class ITunesEntityDto(
    // iTunes IDs exceed 32-bit Int (e.g. 6443366003); Swift Int is 64-bit, so the faithful
    // Kotlin equivalent is Long.
    val artistId: Long = 0,
    val trackId: Long = 0,
    val artistName: String = "",
    val trackName: String = "",
)

data class ITunesSearchResult(val resultCount: Int, val items: List<ITunesItem>)
data class ITunesItem(val artistId: Long, val trackId: Long, val artistName: String, val trackName: String)

internal fun ITunesEntityDto.toDomain(): ITunesItem =
    ITunesItem(artistId = artistId, trackId = trackId, artistName = artistName, trackName = trackName)

@Serializable
data class DummyResponseDto(val resultCount: Int)

@Serializable
data class DummyPayload(val name: String, val age: Int)
