package com.berberoglus.sbnetworking

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

data class HttpClientEnvironment(
    val scheme: String = "https",
    val baseUrl: String,
) {
    /** Builds a validated base [HttpUrl]; `null` if scheme+host(+port) cannot parse. */
    fun baseHttpUrl(): HttpUrl? = "$scheme://$baseUrl".toHttpUrlOrNull()
}
