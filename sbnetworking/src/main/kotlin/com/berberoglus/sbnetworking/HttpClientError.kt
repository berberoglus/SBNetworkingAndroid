package com.berberoglus.sbnetworking

sealed class HttpClientError(message: String?) : Exception(message) {

    data object InvalidUrl : HttpClientError("Invalid URL format or construction")
    data object InvalidResponse : HttpClientError("Invalid or malformed server response")
    data object DecodingFailed : HttpClientError("Failed to decode the response data")
    data object Unauthorized : HttpClientError("Authentication required (401)")

    data class ClientError(val statusCode: Int, val body: ByteArray) :
        HttpClientError("Client error (HTTP $statusCode)") {
        override fun equals(other: Any?): Boolean =
            other is ClientError && statusCode == other.statusCode && body.contentEquals(other.body)
        override fun hashCode(): Int = 31 * statusCode + body.contentHashCode()
    }

    // The body stays in the field only: exception messages travel further than
    // intended (crash reports, log aggregation), so raw response text must not
    // ride along in them.
    data class ServerError(val statusCode: Int, val body: ByteArray) :
        HttpClientError("Server error (HTTP $statusCode)") {
        override fun equals(other: Any?): Boolean =
            other is ServerError && statusCode == other.statusCode && body.contentEquals(other.body)
        override fun hashCode(): Int = 31 * statusCode + body.contentHashCode()
    }

    data object UnexpectedStatusCode : HttpClientError("Received an unexpected HTTP status code")
    data object NotFound : HttpClientError("Resource not found (404)")
    data object NotConnectedToInternet : HttpClientError("No internet connection available")
    data object NetworkConnectionLost : HttpClientError("Network connection was lost during the request")
    data object NotImplemented : HttpClientError("The requested operation is not implemented")
}
