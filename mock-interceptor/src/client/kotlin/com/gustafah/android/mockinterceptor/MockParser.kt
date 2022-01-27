package com.gustafah.android.mockinterceptor

import android.content.Context
import org.json.JSONObject
import java.io.FileNotFoundException

object MockParser {

    /**
     *
     * Uses the provided context to load path from the assets folder, returning the defaultValue
     * data
     *
     */
    fun fromAssetFile(context: Context, path: String, defaultValue: Int = 0): String? {
        val content = getContentFromAsset(context, path) ?: throw FileNotFoundException()
        val jsonContent = JSONObject(content)
        val optionContent =
            jsonContent.getJSONArray(MockUtils.JSON_FIELD_SAVED_DATA).getJSONObject(defaultValue)
        return getMockedData(context, optionContent)
    }

    internal fun getMockedData(context: Context, content: JSONObject) = with(content) {
        when {
            has(MockUtils.JSON_FIELD_DATA) ->
                getJSONObject(MockUtils.JSON_FIELD_DATA).toString()
            has(MockUtils.JSON_FIELD_DATA_ARRAY) ->
                getJSONArray(MockUtils.JSON_FIELD_DATA_ARRAY).toString()
            has(MockUtils.JSON_FIELD_DATA_PATH) ->
                getContentFromAsset(context, getString(MockUtils.JSON_FIELD_DATA_PATH))
            optBoolean(MockUtils.JSON_FIELD_IS_UNIT, false) -> ""
            else -> null
        }
    }

    internal fun getContentFromAsset(context: Context, fileName: String): String? = try {
        val inputStream = context.resources.assets.open(fileName)
        inputStream.bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        null
    }
}

