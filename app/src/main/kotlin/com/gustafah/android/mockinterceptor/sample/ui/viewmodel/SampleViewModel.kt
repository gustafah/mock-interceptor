package com.gustafah.android.mockinterceptor.sample.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.gustafah.android.mockinterceptor.sample.model.FetchResponse
import com.gustafah.android.mockinterceptor.sample.model.Response
import com.gustafah.android.mockinterceptor.sample.repository.SampleRepository

class SampleViewModel(private val repository: SampleRepository) {

    private val _responseLiveData = MediatorLiveData<List<FetchResponse>>()
    val responseLiveData: LiveData<List<FetchResponse>>
        get() = _responseLiveData

    private val _responseErrorLiveData = MediatorLiveData<String>()
    val responseErrorLiveData: LiveData<String>
        get() = _responseErrorLiveData

    fun fetchResponseMock() {
        sendToLiveData(repository.fetchResponseMock())
    }

    fun fetchResponseWithNoAdditional() {
        sendToLiveData(repository.fetchResponseWithNoAdditional())
    }

    fun fetchResponseNoMock() {
        sendToLiveData(repository.fetchResponseNoMock())
    }

    fun fetchResponseNoMockWithParams() {
        sendToLiveData(repository.fetchResponseNoMockWithParams())
    }

    fun fetchResponseMultiMock() {
        sendToLiveData(repository.fetchResponseMultiMock())
    }

    fun fetchResponseNoMockNoFile() {
        sendToLiveData(repository.fetchResponseNoMockNoFile())
    }

    fun fetchResponseMultiMockNoFile() {
        sendToLiveData(repository.fetchResponseMultiMockNoFile())
    }

    fun fetchResponsePaginated(page: String) {
        sendToLiveData(repository.fetchResponsePaginated(if (page.isEmpty()) 0 else page.toInt()))
    }

    private fun sendToLiveData(response: LiveData<Response<List<FetchResponse>?>>) =
        _responseLiveData.addSource(response) {
            when (it) {
                is Response.Success -> {
                    _responseLiveData.postValue(it.data ?: emptyList())
                }
                is Response.Error -> {
                    _responseErrorLiveData.postValue(it.exception.message)
                }
            }
        }

}