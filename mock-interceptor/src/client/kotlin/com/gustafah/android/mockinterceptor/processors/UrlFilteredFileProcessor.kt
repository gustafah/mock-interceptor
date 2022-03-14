package com.gustafah.android.mockinterceptor.processors

import com.gustafah.android.mockinterceptor.MockConfig
import com.gustafah.android.mockinterceptor.MockParser
import okhttp3.Request
import org.json.JSONObject

class UrlFilteredFileProcessor : FileProcessor() {

    override fun process(config: MockConfig, request: Request): JSONObject? {
        val fileName = with(config) {
            val postfix = "$assetsSeparator${request.method}$assetsSuffix"
            val segments = request.url.pathSegments
            segments.filter {
                requestArguments.contains(it).not()
            }.joinToString(assetsSeparator, assetsPrefix, postfix)
        }
        val content = MockParser.getContentFromAsset(config.context(), fileName)
        return content?.let {
            JSONObject(it)
        }
    }

}