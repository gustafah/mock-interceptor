package com.gustafah.android.mockinterceptor

import androidx.appcompat.app.AppCompatActivity
import com.gustafah.android.mockinterceptor.MockConfig.OptionsSelectorMode.NO_SELECTION
import com.gustafah.android.mockinterceptor.MockUtils.ERROR_JSON_NO_DATA
import com.gustafah.android.mockinterceptor.MockUtils.JSON_FIELD_MULTI
import com.gustafah.android.mockinterceptor.MockUtils.JSON_FIELD_REFERENCE
import com.gustafah.android.mockinterceptor.MockUtils.JSON_FIELD_SAVED_DATA
import com.gustafah.android.mockinterceptor.MockUtils.mockResponse
import com.gustafah.android.mockinterceptor.MockUtils.processSavedData
import com.gustafah.android.mockinterceptor.extensions.arrayWithFilterAndArgs
import com.gustafah.android.mockinterceptor.extensions.first
import com.gustafah.android.mockinterceptor.extensions.isNotEmpty
import com.gustafah.android.mockinterceptor.extensions.mapData
import com.gustafah.android.mockinterceptor.ui.MockOptionsActivity
import com.gustafah.android.mockinterceptor.ui.MockOptionsDialog
import com.gustafah.android.mockinterceptor.ui.MockReferenceActivity
import com.gustafah.android.mockinterceptor.ui.MockReferenceDialog
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

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
        val mockContent = config.fetchMockContentFromRequest(request)
        return mockContent?.let {
            if (it.remove(JSON_FIELD_MULTI) == true) {
                pickMultiMockResponse(it, request)
            } else {
                if (it.has("type"))
                    mockResponse(code = 502, jsonResponse = it.toString(), request = request)
                else
                    pickMockResponse(it, request)
            }
        } ?: mockResponse(code = 502, jsonResponse = ERROR_JSON_NO_DATA, request = request)
    }

    private fun pickMultiMockResponse(content: JSONObject, request: Request): Response {
        val nameArray = arrayListOf<String>()
        val titleArray = arrayListOf<String>()
        val names = content.names()!!
        for (i in 0 until names.length()) {
            nameArray.add(names.getString(i))
            titleArray.add(
                content.getJSONObject(names.getString(i)).getString(JSON_FIELD_REFERENCE)
            )
        }
        return synchronized(mockFlow) {
            displayReferences(titleArray.toTypedArray())
            waitValidation()
            val auxOpt = optChoice.get()
            optChoice.set(-1)
            return@synchronized pickMockResponse(
                content.getJSONObject(nameArray[auxOpt]),
                request
            )
        }
    }

    private fun pickMockResponse(content: JSONObject, request: Request): Response {
        if (content.has(JSON_FIELD_SAVED_DATA)) {
            val arraySavedData = content.getJSONArray(JSON_FIELD_SAVED_DATA)
            val arrayFilteredSavedData =
                arraySavedData.arrayWithFilterAndArgs(config.requestArguments)
            val jsonArray = if (arrayFilteredSavedData.isNotEmpty()) arrayFilteredSavedData
            else arraySavedData
            val default = content.getInt(MockUtils.JSON_FIELD_DEFAULT)

            return processSavedData(
                config.context(),
                when {
                    (config.selectorMode == NO_SELECTION) -> jsonArray.first()
                    (MockUtils.autoMock || jsonArray.length() == 1) ->
                        jsonArray.getJSONObject(
                            if (default >= 0 && arrayFilteredSavedData.length() == 0)
                                default
                            else
                                0
                        )
                    else -> synchronized(mockFlow) {
                        when {
                            default >= 0 -> optChoice.set(default)
                            default == -2 -> optChoice.set((0 until jsonArray.length()).random())
                            else -> {
                                val data = jsonArray.mapData()
                                displayOptions(content.getString(JSON_FIELD_REFERENCE), data)
                                waitValidation()
                            }
                        }
                        return@synchronized jsonArray.getJSONObject(optChoice.get())
                    }
                },
                request
            )
        } else {
            return mockResponse(502, content.toString(), request)
        }
    }

    private fun displayReferences(title: Array<String>) {
        val currentContext = config.context() as AppCompatActivity
        if (config.selectorMode == MockConfig.OptionsSelectorMode.STANDARD) {
            MockReferenceDialog.newInstance(title)
                .show(currentContext.supportFragmentManager, "MockOptionsDialog")
        } else {
            currentContext.startActivity(
                MockReferenceActivity.makeIntent(currentContext, title)
            )
        }
    }

    private fun displayOptions(title: String, data: Pair<Array<String>, Array<String>>) {
        val currentContext = config.context() as AppCompatActivity
        if (config.selectorMode == MockConfig.OptionsSelectorMode.STANDARD) {
            MockOptionsDialog.newInstance(title, data.first, data.second)
                .show(currentContext.supportFragmentManager, "MockOptionsDialog")
        } else {
            currentContext.startActivity(
                MockOptionsActivity.makeIntent(
                    currentContext,
                    title,
                    data.first,
                    data.second
                )
            )
        }
    }

    private fun waitValidation() {
        if (countDownLatch.count == 0L) {
            countDownLatch = CountDownLatch(1)
        }
        isAwaiting.set(true)
        countDownLatch.await()
    }

}