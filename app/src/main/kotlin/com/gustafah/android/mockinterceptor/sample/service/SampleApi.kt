package com.gustafah.android.mockinterceptor.sample.service

import com.gustafah.android.mockinterceptor.sample.model.FetchResponse
import retrofit2.Response
import retrofit2.http.GET

interface SampleApi {

    @GET("posts")
    suspend fun fetch(): Response<List<FetchResponse>>

}