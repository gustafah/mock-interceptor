package com.gustafah.android.mockinterceptor

import androidx.appcompat.app.AppCompatActivity
import com.gustafah.android.mockinterceptor.MockUtils.JSON_FIELD_DEFAULT
import com.gustafah.android.mockinterceptor.MockUtils.JSON_FIELD_REFERENCE
import com.gustafah.android.mockinterceptor.MockUtils.JSON_FIELD_SAVED_DATA
import com.gustafah.android.mockinterceptor.MockUtils.mockResponse
import com.gustafah.android.mockinterceptor.extensions.arrayWithFilterAndArgs
import com.gustafah.android.mockinterceptor.extensions.first
import com.gustafah.android.mockinterceptor.extensions.isNotEmpty
import com.gustafah.android.mockinterceptor.extensions.mapData
import com.gustafah.android.mockinterceptor.ui.MockOptionsActivity
import com.gustafah.android.mockinterceptor.ui.MockOptionsDialog
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.util.*
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
        val mockContent = config.fetchFileNameFromUrl(request)
        JSONObject(mockContent).let {
            if (it.has(JSON_FIELD_SAVED_DATA).not()) {
                return mockResponse(code = 502, jsonResponse = it.toString(), request = request)
            }

            val arraySavedData = it.getJSONArray(JSON_FIELD_SAVED_DATA)
            val arrayFilteredSavedData =
                arraySavedData.arrayWithFilterAndArgs(config.requestArguments)
            val jsonArray =
                if (arrayFilteredSavedData.isNotEmpty()) arrayFilteredSavedData else arraySavedData
            val default = it.getInt(JSON_FIELD_DEFAULT)
            return if (config.selectorMode == MockConfig.OptionsSelectorMode.NO_SELECTION)
                mockResponse(jsonArray.first(), request)
            else if (MockUtils.autoMock || jsonArray.length() == 1) {
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

    private fun waitValidation() {
        if (countDownLatch.count == 0L) {
            countDownLatch = CountDownLatch(1)
        }
        isAwaiting.set(true)
        countDownLatch.await()
    }

}