package com.gustafah.android.mockinterceptor

@Target(AnnotationTarget.FUNCTION)
annotation class Mock(
    val path: String = "",
    val files: Array<String> = [],
    val ignoreAdditionalList: Boolean = false
)