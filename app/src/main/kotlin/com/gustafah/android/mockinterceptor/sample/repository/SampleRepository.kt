package com.gustafah.android.mockinterceptor.sample.repository

import androidx.lifecycle.liveData
import com.gustafah.android.mockinterceptor.sample.service.SampleApi
import com.gustafah.android.mockinterceptor.sample.model.Response

class SampleRepository(private val service: SampleApi) {

    fun fetchResponse() = liveData {
        val response = service.fetch()
        if (response.isSuccessful) {
            emit(Response.Success(data = response.body()))
        } else {
            emit(Response.Error(exception = Exception("Fail to fetch Response")))
        }
    }

    fun fetchResponse2() = liveData {
        val response = service.fetchNoMock()
        if (response.isSuccessful) {
            emit(Response.Success(data = response.body()))
        } else {
            emit(Response.Error(exception = Exception("Fail to fetch Response")))
        }
    }

    fun fetchResponse3() = liveData {
        val response = service.fetchMultiMock()
        if (response.isSuccessful) {
            emit(Response.Success(data = response.body()))
        } else {
            emit(Response.Error(exception = Exception("Fail to fetch Response")))
        }
    }

}