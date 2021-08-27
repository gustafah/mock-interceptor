package com.gustafah.android.mockinterceptor

import android.annotation.SuppressLint
import android.content.SharedPreferences
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject

object MockUtils {

    internal const val BUNDLE_FIELD_TITLE = "saved_data_title"
    internal const val BUNDLE_FIELD_TEXT = "saved_data_text"
    internal const val BUNDLE_FIELD_SUBTEXT = "saved_data_subtext"

    internal const val JSON_FIELD_REFERENCE = "reference"
    internal const val JSON_FIELD_SAVED_DATA = "saved_data"
    internal const val JSON_FIELD_DEFAULT = "default"
    internal const val JSON_FIELD_DESCRIPTION = "description"
    internal const val JSON_FIELD_CODE = "code"
    internal const val JSON_FIELD_FILTER = "filter"
    internal const val JSON_FIELD_DATA = "data"
    internal const val JSON_FIELD_DATA_ARRAY = "data_array"
    internal const val JSON_FIELD_DATA_PATH = "data_path"
    internal const val JSON_FIELD_IS_UNIT = "is_unit"

    internal const val ERROR_JSON_NOT_FOUND =
        "{\"type\": \"SERVICE_UNAVAILABLE\",\"message\": \"Couldn't find a mock for this request. " +
                "(suggestion: %s\", \"filter\": {\"data\": [\"%s\"]}})"
    internal const val ERROR_JSON_NO_DATA =
        "{\"type\": \"UNKNOWN_ERROR\", \"message\": \"No mocked data.\"}"

    internal const val DEFAULT_MOCK_KEY = "DEFAULT_MOCK_KEY"

    internal var prefs: SharedPreferences? = null
    internal var autoMock: Boolean = false
        set(value) {
            savePref(DEFAULT_MOCK_KEY, value)
            field = value
        }
        get() {
            return getPrefs(DEFAULT_MOCK_KEY, false)
        }

    internal fun mockResponse(savedData: JSONObject, request: Request): Response {
        var code = savedData.getInt(JSON_FIELD_CODE)
        val json = when {
            savedData.has(JSON_FIELD_DATA) -> savedData.getJSONObject(JSON_FIELD_DATA).toString()
            savedData.has(JSON_FIELD_DATA_ARRAY) -> savedData.getJSONArray(JSON_FIELD_DATA_ARRAY)
                .toString()
            savedData.has(JSON_FIELD_DATA_PATH) -> MockInterceptor.config.getContentFromFileName(
                savedData.getString(
                    JSON_FIELD_DATA_PATH
                )
            )
            savedData.optBoolean(JSON_FIELD_IS_UNIT, false) -> ""
            else -> {
                code = 502
                ERROR_JSON_NO_DATA
            }
        }
        return mockResponse(code, json, request)
    }

    internal fun mockResponse(code: Int, jsonResponse: String, request: Request): Response {
        return Response.Builder()
            .protocol(Protocol.HTTP_1_1)
            .request(request)
            .message("Mocked")
            .code(code)
            .body((jsonResponse).toResponseBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()
    }

    @SuppressLint("CommitPrefEdits")
    private fun savePref(key: String, value: String?) {
        if (value == null)
            prefs?.edit()?.remove(key)
        else
            prefs?.edit()?.putString(key, value)
    }

    @SuppressLint("CommitPrefEdits")
    private fun savePref(key: String, value: Boolean?) {
        if (value == null)
            prefs?.edit()?.remove(key)?.apply()
        else
            prefs?.edit()?.putBoolean(key, value)?.apply()
    }

    private fun getPrefs(key: String, default: String? = null) = prefs?.getString(key, default)
    private fun getPrefs(key: String, default: Boolean) = prefs?.getBoolean(key, default) ?: default

}