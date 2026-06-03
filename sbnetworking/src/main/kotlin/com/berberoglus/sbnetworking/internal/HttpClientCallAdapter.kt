package com.berberoglus.sbnetworking.internal

import com.berberoglus.sbnetworking.HttpClientError
import com.berberoglus.sbnetworking.auth.TokenAuthenticator
import okhttp3.Request
import okio.Timeout
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.io.IOException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Wraps each Retrofit suspend call so failures surface as [HttpClientError] rather than raw
 * HttpException/IOException. Success bodies pass through unchanged; non-2xx and transport errors are
 * mapped exactly like the dynamic [com.berberoglus.sbnetworking.HttpClient.executeRaw] path. When a
 * 401 was caused by a failed token refresh, the original refresh throwable (captured by
 * [tokenAuthenticator]) is surfaced instead of a generic [HttpClientError.Unauthorized].
 */
internal class HttpClientCallAdapterFactory(
    private val tokenAuthenticator: TokenAuthenticator? = null,
) : CallAdapter.Factory() {
    override fun get(returnType: Type, annotations: Array<Annotation>, retrofit: Retrofit): CallAdapter<*, *>? {
        if (getRawType(returnType) != Call::class.java) return null
        val responseType = getParameterUpperBound(0, returnType as ParameterizedType)
        return object : CallAdapter<Any, Call<*>> {
            override fun responseType(): Type = responseType
            override fun adapt(call: Call<Any>): Call<*> = NormalizingCall(call, tokenAuthenticator)
        }
    }
}

private class NormalizingCall<T>(
    private val delegate: Call<T>,
    private val tokenAuthenticator: TokenAuthenticator?,
) : Call<T> {
    override fun enqueue(callback: Callback<T>) {
        delegate.enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                // Retrofit treats only 200..299 as successful; 304 must also pass through as success
                // (iOS validateResponse maps 200..299 AND 304 to success).
                if (response.isSuccessful || response.code() == 304) {
                    callback.onResponse(call, response)
                } else {
                    val refreshError = tokenAuthenticator?.consumeRefreshError()
                    if (refreshError != null) {
                        callback.onFailure(call, refreshError)
                    } else {
                        val body = response.errorBody()?.bytes() ?: ByteArray(0)
                        callback.onFailure(call, mapHttpStatus(response.code(), body))
                    }
                }
            }
            override fun onFailure(call: Call<T>, t: Throwable) {
                val refreshError = tokenAuthenticator?.consumeRefreshError()
                callback.onFailure(call, refreshError ?: if (t is IOException) mapIo(t) else t)
            }
        })
    }
    override fun clone(): Call<T> = NormalizingCall(delegate.clone(), tokenAuthenticator)
    override fun execute(): Response<T> = delegate.execute()
    override fun isExecuted(): Boolean = delegate.isExecuted
    override fun cancel() = delegate.cancel()
    override fun isCanceled(): Boolean = delegate.isCanceled
    override fun request(): Request = delegate.request()
    override fun timeout(): Timeout = delegate.timeout()
}

private fun mapHttpStatus(code: Int, body: ByteArray): HttpClientError = when (code) {
    401 -> HttpClientError.Unauthorized
    404 -> HttpClientError.NotFound
    400, 402, 403 -> HttpClientError.ClientError(code, body)
    in 405..499 -> HttpClientError.ClientError(code, body)
    in 500..599 -> HttpClientError.ServerError(code, body)
    else -> HttpClientError.UnexpectedStatusCode
}

private fun mapIo(e: IOException): HttpClientError = when (e) {
    is java.net.UnknownHostException -> HttpClientError.NotConnectedToInternet
    is java.net.SocketException -> HttpClientError.NetworkConnectionLost
    else -> HttpClientError.NetworkConnectionLost
}
