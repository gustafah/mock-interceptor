package com.gustafah.android.mockinterceptor.sample.repository

import androidx.lifecycle.liveData
import com.gustafah.android.mockinterceptor.sample.service.SampleApi
import com.gustafah.android.mockinterceptor.sample.model.Response

class SampleRepository(private val service: SampleApi) {

    fun fetchResponseMock() = liveData {
        val response = service.fetch()
        if (response.isSuccessful) {
            emit(Response.Success(data = response.body()))
        } else {
            emit(Response.Error(exception = Exception(response.errorBody()?.string() ?: "Fail to fetch Response")))
        }
    }

    fun fetchResponseNoMock() = liveData {
        val response = service.fetchNoMock()
        if (response.isSuccessful) {
            emit(Response.Success(data = response.body()))
        } else {
            emit(Response.Error(exception = Exception(response.errorBody()?.string() ?: "Fail to fetch Response")))
        }
    }

    fun fetchResponseNoMockWithParams() = liveData {
        val response = service.fetchNoMockWithParams("bacate")
        if (response.isSuccessful) {
            emit(Response.Success(data = response.body()))
        } else {
            emit(Response.Error(exception = Exception("Fail to fetch Response")))
        }
    }

    fun fetchResponseMultiMock() = liveData {
        val response = service.fetchMultiMock()
        if (response.isSuccessful) {
            emit(Response.Success(data = response.body()))
        } else {
            emit(Response.Error(exception = Exception(response.errorBody()?.string() ?: "Fail to fetch Response")))
        }
    }

    fun fetchResponseNoMockNoFile() = liveData {
        val response = service.fetchNoMockNoFile()
        if (response.isSuccessful) {
            emit(Response.Success(data = response.body()))
        } else {
            emit(Response.Error(exception = Exception(response.errorBody()?.string() ?: "Fail to fetch Response")))
        }
    }

    fun fetchResponseMultiMockNoFile() = liveData {
        val response = service.fetchMultiMockNoFile()
        if (response.isSuccessful) {
            emit(Response.Success(data = response.body()))
        } else {
            emit(Response.Error(exception = Exception("Fail to fetch Response")))
        }
    }

}