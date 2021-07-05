package com.gustafah.android.mockinterceptor

import androidx.appcompat.app.AppCompatActivity
import com.gustafah.android.mockinterceptor.ui.MockOptionsActivity
import com.gustafah.android.mockinterceptor.ui.MockOptionsDialog
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.ArrayList

object MockInterceptor : Interceptor {
    lateinit var config: MockConfig
    private val isAwaiting = AtomicBoolean(false)
    private val optChoice = AtomicInteger(-1)
    private val mockFlow = Object()
    private val lock = Object()

    private var countDownLatch = CountDownLatch(1)

    fun release(which: Int) = synchronized(lock) {
        optChoice.set(which)
        isAwaiting.set(false)
        countDownLatch.countDown()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request: Request = chain.request()
        val mockContent = config.fetchFileNameFromUrl(request)
        JSONObject(mockContent).let {
            if (it.has(JSON_FIELD_SAVED_DATA).not()) {
                return mockResponse(code = 502, jsonResponse = it.toString(), request = request)
            }

            val arraySavedData = it.getJSONArray(JSON_FIELD_SAVED_DATA)
            val arrayFilteredSavedData =
                arraySavedData.arrayWithFilterAndArgs(config.requestArguments)
            val jsonArray =
                if (arrayFilteredSavedData.length() > 0) arrayFilteredSavedData else arraySavedData
            val default = it.getInt(JSON_FIELD_DEFAULT)
            return if (config.selectorMode == MockConfig.OptionsSelectorMode.NO_SELECTION)
                mockResponse(jsonArray.getJSONObject(0), request)
            else if (AUTO_MOCK || jsonArray.length() == 1) {
                mockResponse(
                    jsonArray.getJSONObject(
                        if (default >= 0 && arrayFilteredSavedData.length() == 0) default else 0
                    ),
                    request
                )
            } else {
                synchronized(mockFlow) {
                    if (default >= 0) {
                        optChoice.set(default)
                    } else if (default == -2) {
                        optChoice.set((0 until jsonArray.length()).random())
                    } else {
                        val currentContext = config.context() as AppCompatActivity
                        val data = jsonArray.mapData()
                        if (config.selectorMode == MockConfig.OptionsSelectorMode.STANDARD) {
                            MockOptionsDialog.newInstance(
                                it.getString(JSON_FIELD_REFERENCE),
                                data.first,
                                data.second
                            ).show(currentContext.supportFragmentManager, "MockOptionsDialog")
                        } else {
                            currentContext.startActivity(
                                MockOptionsActivity.makeIntent(
                                    currentContext,
                                    it.getString(JSON_FIELD_REFERENCE),
                                    data.first,
                                    data.second
                                )
                            )
                        }

                        waitValidation()
                    }

                    return@synchronized mockResponse(
                        jsonArray.getJSONObject(optChoice.get()),
                        request
                    )
                }
            }
        }
    }

    private fun JSONArray.mapData(): Pair<Array<String>, Array<String>> {
        val text = ArrayList<String>()
        val subtext = ArrayList<String>()
        for (i in 0 until length()) {
            val jsonObject = getJSONObject(i)
            text.add(jsonObject.getString(JSON_FIELD_DESCRIPTION))
            subtext.add(jsonObject.getString(JSON_FIELD_CODE))
        }
        val textArray = text.toTypedArray()
        val subtextArray = subtext.toTypedArray()
        return Pair(textArray, subtextArray)
    }

    private fun mockResponse(savedData: JSONObject, request: Request): Response {
        var code = savedData.getInt(JSON_FIELD_CODE)
//        val title = savedData.getString(JSON_FIELD_DESCRIPTION)
        val json = when {
            savedData.has(JSON_FIELD_DATA) -> savedData.getJSONObject(JSON_FIELD_DATA).toString()
            savedData.has(JSON_FIELD_DATA_ARRAY) -> savedData.getJSONArray(JSON_FIELD_DATA_ARRAY)
                .toString()
            savedData.has(JSON_FIELD_DATA_PATH) -> config.getContentFromFileName(
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

    private fun mockResponse(code: Int, jsonResponse: String, request: Request): Response {
        // NOTE: Don't touch
        val serverDate = DateTimeFormatter.ofPattern(PATTERN_DATE_TIME_GMT, Locale.US)
            .format(LocalDateTime.now())

        return Response.Builder()
            .protocol(Protocol.HTTP_1_1)
            .request(request)
            .message("Mocked")
            .header("date", serverDate)
            .code(code)
            .body((jsonResponse).toResponseBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()
    }

    private fun waitValidation() {
        if (countDownLatch.count == 0L) {
            countDownLatch = CountDownLatch(1)
        }
        isAwaiting.set(true)
        countDownLatch.await()
    }

}

fun JSONArray.arrayWithFilterAndArgs(args: List<String>): JSONArray {
    return (0 until length()).asSequence().map { getJSONObject(it) }.filter {
        it.has(JSON_FIELD_FILTER) && it.getJSONObject(JSON_FIELD_FILTER)
            .getJSONArray(JSON_FIELD_DATA).hasAll(args)
    }.run {
        JSONArray().apply {
            iterator().forEach { jsonObj ->
                this.put(jsonObj)
            }
        }
    }
}

fun JSONArray.hasAny(args: List<String>): Boolean =
    (0 until length()).asSequence().map { getString(it) }.any {
        args.contains(it)
    }

fun JSONArray.hasAll(args: List<String>): Boolean =
    (0 until length()).asSequence().map { getString(it) }.all {
        args.contains(it)
    }
