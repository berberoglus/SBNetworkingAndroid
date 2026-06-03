package com.berberoglus.sbnetworking

import com.berberoglus.sbnetworking.auth.TokenAuthenticator
import com.berberoglus.sbnetworking.internal.appJson
import com.berberoglus.sbnetworking.internal.decodeOrThrow
import com.berberoglus.sbnetworking.internal.nullIfEmpty
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import retrofit2.Retrofit
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class HttpClient internal constructor(
    val environment: HttpClientEnvironment,
    val authTokenProvider: AuthTokenProvider?,
    val okHttpClient: OkHttpClient,
    val retrofit: Retrofit,
    internal val tokenAuthenticator: TokenAuthenticator? = null,
) {
    /**
     * Decodes the JSON body to [R]; null on 204. If [R] is [ByteArray] the raw success body is
     * returned unchanged — the faithful port of the iOS `responseType is Data.Type` passthrough
     * (behavior #10). @throws HttpClientError
     */
    suspend inline fun <reified R : Any> execute(spec: EndpointSpec): R? {
        val raw = executeRaw(spec) ?: return null
        return if (R::class == ByteArray::class) raw as R else appJson.decodeOrThrow<R>(raw)
    }

    suspend inline fun <reified R : Any> execute(convertible: EndpointConvertible): R? =
        execute(convertible.toEndpoint())

    /** Builds the OkHttp request for [spec]; throws [HttpClientError.InvalidUrl] on a bad URL. */
    internal fun buildRequest(spec: EndpointSpec): Request {
        val base = environment.baseHttpUrl() ?: throw HttpClientError.InvalidUrl
        val urlBuilder = base.newBuilder()
        // iOS sets components.path = endpoint.path (replaces the path entirely).
        urlBuilder.encodedPath(normalizedPath(spec.path))
        spec.queryParameters.nullIfEmpty()?.forEach { (k, v) -> urlBuilder.addQueryParameter(k, v) }
        val url = urlBuilder.build()

        val requestBuilder = Request.Builder().url(url)
        val headers = spec.headerFields.toMutableMap()
        if (spec.body != null && headers.keys.none { it.equals("Content-Type", ignoreCase = true) }) {
            headers["Content-Type"] = "application/json"
        }
        headers.forEach { (k, v) -> requestBuilder.header(k, v) }

        val requestBody = spec.body?.toRequestBody(
            headers["Content-Type"]?.toMediaTypeOrNull() ?: "application/json".toMediaTypeOrNull(),
        ) ?: emptyBodyFor(spec.method)
        requestBuilder.method(spec.method.value, requestBody)
        return requestBuilder.build()
    }

    /**
     * OkHttp rejects a null body for methods that require one (POST/PUT/PATCH). iOS allowed a
     * bodyless POST; reproduce that by sending an empty body for those verbs (bodySize == 0),
     * while GET/DELETE keep a null body.
     */
    internal fun emptyBodyFor(method: HttpMethod) = when (method) {
        HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH -> ByteArray(0).toRequestBody(null)
        HttpMethod.GET, HttpMethod.DELETE -> null
    }

    /** Sends [spec], returns the raw success body (null on 204), maps failures to HttpClientError. */
    suspend fun executeRaw(spec: EndpointSpec): ByteArray? {
        val request = buildRequest(spec)
        // Apply the per-endpoint timeout (iOS `Endpoint.timeoutInterval`, default 20s) as an OkHttp
        // call timeout, derived from the shared, auth-configured client so the auth interceptor +
        // 401 authenticator still apply.
        val call = okHttpClient.newBuilder()
            .callTimeout(spec.timeoutSeconds, TimeUnit.SECONDS)
            .build()
            .newCall(request)
        val response = try {
            call.await()
        } catch (e: IOException) {
            // A failed token refresh inside the authenticator is captured rather than thrown, so
            // surface the original refresh error before falling back to transport-error mapping.
            tokenAuthenticator?.consumeRefreshError()?.let { throw it }
            throw mapIoException(e)
        }
        response.use { resp ->
            tokenAuthenticator?.consumeRefreshError()?.let { throw it }
            val bytes = resp.body?.bytes() ?: ByteArray(0)
            return validateAndExtractBody(resp.code, bytes)
        }
    }

    internal fun normalizedPath(path: String): String = when {
        path.isEmpty() -> "/"
        path.startsWith("/") -> path
        else -> "/$path"
    }

    internal fun mapIoException(e: IOException): HttpClientError {
        val message = e.message?.lowercase().orEmpty()
        return when {
            message.contains("unable to resolve host") ||
                message.contains("no address associated") ||
                e is java.net.UnknownHostException -> HttpClientError.NotConnectedToInternet
            message.contains("connection reset") ||
                message.contains("connection lost") ||
                e is java.net.SocketException -> HttpClientError.NetworkConnectionLost
            else -> HttpClientError.NetworkConnectionLost
        }
    }

    companion object {
        fun Builder(environment: HttpClientEnvironment): HttpClientBuilder =
            HttpClientBuilder(environment)
    }
}

/** Suspends until the OkHttp [Call] completes; cancels the call on coroutine cancellation. */
suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (cont.isCancelled) return
            cont.resumeWithException(e)
        }
        override fun onResponse(call: Call, response: Response) {
            cont.resume(response)
        }
    })
    cont.invokeOnCancellation { runCatching { cancel() } }
}
