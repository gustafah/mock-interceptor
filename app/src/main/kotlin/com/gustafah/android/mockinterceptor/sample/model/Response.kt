package com.gustafah.android.mockinterceptor.sample.model

sealed class Response<out R> {
    data class Success<out T>(val data: T?) : Response<T?>()
    data class Error(val exception: Exception) : Response<Nothing>()
}

data class FetchResponse(
    val userId: Int,
    val id: Int,
    val title: String,
    val body: String
)