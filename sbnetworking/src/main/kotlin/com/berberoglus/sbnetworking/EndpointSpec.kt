package com.berberoglus.sbnetworking

/**
 * An immutable, transport-level description of a single request. Feature code normally uses Retrofit
 * `*Api` interfaces (see Networking_Domain_Rules.md §3); [EndpointSpec] exists for dynamic or
 * programmatic requests built and sent through [HttpClient.execute].
 */
data class EndpointSpec(
    val path: String,
    val method: HttpMethod,
    val headerFields: Map<String, String> = emptyMap(),
    val queryParameters: Map<String, String> = emptyMap(),
    val body: ByteArray? = null,
    val timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
) {
    companion object {
        const val DEFAULT_TIMEOUT_SECONDS: Long = 20
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EndpointSpec) return false
        return path == other.path &&
            method == other.method &&
            headerFields == other.headerFields &&
            queryParameters == other.queryParameters &&
            (body?.contentEquals(other.body) ?: (other.body == null)) &&
            timeoutSeconds == other.timeoutSeconds
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + method.hashCode()
        result = 31 * result + headerFields.hashCode()
        result = 31 * result + queryParameters.hashCode()
        result = 31 * result + (body?.contentHashCode() ?: 0)
        result = 31 * result + timeoutSeconds.hashCode()
        return result
    }
}
