package com.berberoglus.sbnetworking.envelope

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Generic envelope DTO (Networking_Domain_Rules.md §8). */
@Serializable
data class EnvelopeDto<T>(
    val data: List<T>,
    val meta: MetaDto? = null,
)

@Serializable
data class MetaDto(
    val page: Int = 0,
    @SerialName("total_pages") val totalPages: Int = 0,
)
