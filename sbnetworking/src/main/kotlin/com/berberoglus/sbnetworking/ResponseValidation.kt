package com.berberoglus.sbnetworking

/**
 * Maps an HTTP status code + body to either success (returns the body, possibly empty) or a thrown
 * [HttpClientError]. Faithful port of the iOS `HTTPClient.validateResponse`:
 * - 200..299, 304 -> success (return body)
 * - 204 -> success with null body (empty)
 * - 401 -> Unauthorized
 * - 404 -> NotFound
 * - 400, 402, 403, 405..499 -> ClientError
 * - 500..599 -> ServerError
 * - else -> UnexpectedStatusCode
 *
 * Returns the raw body for a success (null when 204). Throws on any non-success.
 */
internal fun validateAndExtractBody(statusCode: Int, body: ByteArray): ByteArray? {
    when (statusCode) {
        204 -> return null
        in 200..299, 304 -> return body
        401 -> throw HttpClientError.Unauthorized
        404 -> throw HttpClientError.NotFound
        400, 402, 403 -> throw HttpClientError.ClientError(statusCode, body)
        in 405..499 -> throw HttpClientError.ClientError(statusCode, body)
        in 500..599 -> throw HttpClientError.ServerError(statusCode, body)
        else -> throw HttpClientError.UnexpectedStatusCode
    }
}
