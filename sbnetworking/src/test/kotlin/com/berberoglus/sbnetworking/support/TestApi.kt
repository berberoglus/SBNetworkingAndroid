package com.berberoglus.sbnetworking.support

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface TestApi {
    @GET("search")
    suspend fun search(
        @Query("term") term: String,
        @Query("entity") entity: String,
        @Query("limit") limit: String,
    ): ITunesSearchResponseDto

    @POST("test")
    suspend fun post(@Body body: DummyPayload): DummyResponseDto

    @GET("dummy-retry")
    suspend fun dummyRetry(): DummyResponseDto
}
