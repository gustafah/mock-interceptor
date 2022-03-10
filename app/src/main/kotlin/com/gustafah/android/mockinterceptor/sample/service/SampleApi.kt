package com.gustafah.android.mockinterceptor.sample.service

import com.gustafah.android.mockinterceptor.Mock
import com.gustafah.android.mockinterceptor.sample.model.FetchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface SampleApi {

    @GET("posts")
    @Mock("posts.json")
    suspend fun fetch(): Response<List<FetchResponse>>

    @GET("posts")
    suspend fun fetchNoMock(): Response<List<FetchResponse>>

    @GET("posts/{id}")
    suspend fun fetchNoMockWithParams(@Path("id") id: String): Response<List<FetchResponse>>

    @GET("posts")
    @Mock(files = ["posts.json", "error.json"])
    suspend fun fetchMultiMock(): Response<List<FetchResponse>>

    @GET("posts")
    @Mock(files = ["posts.json", "errors.json"])
    suspend fun fetchMultiMockNoFile(): Response<List<FetchResponse>>

    @GET("comments")
    suspend fun fetchNoMockNoFile(): Response<List<FetchResponse>>

}