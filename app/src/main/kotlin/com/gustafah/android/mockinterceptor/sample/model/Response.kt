package com.gustafah.android.mockinterceptor.sample.model

import com.google.gson.Gson

sealed class Response<out R> {
    data class Success<out T>(val data: T?) : Response<T?>()
    data class Error(val exception: Exception) : Response<Nothing>()
}

data class FetchResponse(
    val userId: Int,
    val id: Int,
    val title: String,
    val body: String
) {
    fun toJson(): String {
        val notFancy = Gson().toJson(this)
        return buildString(notFancy.length) {
            var indent = 0
            fun StringBuilder.line() {
                appendLine()
                repeat(4 * indent) { append(' ') }
            }

            for (char in notFancy) {
                if (char == ' ') continue
                when (char) {
                    ')', ']', '}' -> {
                        indent--
                        line()
                    }
                }
                if (char == '=') append(' ')
                append(char)
                if (char == '=') append(' ')

                when (char) {
                    '(', '[', ',', '{' -> {
                        if (char != ',') indent++
                        line()
                    }
                }
            }
        }
    }

}