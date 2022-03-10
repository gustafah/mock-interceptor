package com.gustafah.android.mockinterceptor.processors

import com.gustafah.android.mockinterceptor.Mock
import com.gustafah.android.mockinterceptor.MockConfig
import com.gustafah.android.mockinterceptor.MockParser
import com.gustafah.android.mockinterceptor.MockUtils
import com.gustafah.android.mockinterceptor.MockUtils.JSON_FIELD_MULTI
import okhttp3.Request
import org.json.JSONObject
import retrofit2.Invocation

class AnnotationFileProcessor : FileProcessor() {

    override fun process(config: MockConfig, request: Request): JSONObject? {
        val files =
            request.tag(Invocation::class.java)?.method()?.getAnnotation(Mock::class.java)?.let {
                when {
                    it.path.isNotEmpty() -> arrayOf(it.path)
                    it.files.isNotEmpty() -> it.files
                    else -> null
                }
            }
        return files?.let {
            if (it.size > 1) {
                val output = JSONObject()
                output.put(JSON_FIELD_MULTI, true)
                it.forEach { fileName ->
                    output.put(fileName, getContentFromFile(config, fileName))
                }
                output
            } else {
                val fileName = it.first()
                getContentFromFile(config, fileName)
            }
        }
    }

    private fun getContentFromFile(config: MockConfig, fileName: String): JSONObject {
        return JSONObject(
            MockParser.getContentFromAsset(config.context(), fileName) ?: String.format(
                MockUtils.ERROR_JSON_NOT_FOUND, fileName
            )
        )
    }
}