package com.gustafah.android.mockinterceptor.sample.service

import com.gustafah.android.mockinterceptor.Mock
import com.gustafah.android.mockinterceptor.sample.model.FetchResponse
import retrofit2.Response
import retrofit2.http.GET

interface SampleApi {

    @GET("posts")
    @Mock("posts.json")
    suspend fun fetch(): Response<List<FetchResponse>>

    @GET("posts")
    suspend fun fetchNoMock(): Response<List<FetchResponse>>

    @GET("posts")
    @Mock(files = ["posts.json", "error.json"])
    suspend fun fetchMultiMock(): Response<List<FetchResponse>>

    @GET("comments")
    suspend fun fetchNoMockNoFile(): Response<List<FetchResponse>>

}