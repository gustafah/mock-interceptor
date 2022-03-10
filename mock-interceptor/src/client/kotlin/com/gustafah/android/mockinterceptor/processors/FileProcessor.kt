package com.gustafah.android.mockinterceptor.processors

import com.gustafah.android.mockinterceptor.MockConfig
import okhttp3.Request
import org.json.JSONObject

open class FileProcessor {

    open fun process(config: MockConfig, request: Request): JSONObject? = null

}