package com.gustafah.android.mockinterceptor.sample.ui.viewmodel

import com.gustafah.android.mockinterceptor.sample.repository.SampleRepository

class SampleViewModel(private val repository: SampleRepository) {

    fun fetchResponse() = repository.fetchResponse()

}