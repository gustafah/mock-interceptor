package com.gustafah.android.mockinterceptor

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.gustafah.android.mockinterceptor.MockConfig.OptionsSelectorMode.NO_SELECTION
import com.gustafah.android.mockinterceptor.MockUtils.ERROR_JSON_NOT_FOUND
import com.gustafah.android.mockinterceptor.MockUtils.JSON_FIELD_REFERENCE
import com.gustafah.android.mockinterceptor.MockUtils.JSON_FIELD_SAVED_DATA
import com.gustafah.android.mockinterceptor.MockUtils.SAVE_MOCK_MODE_PLAYBACK
import com.gustafah.android.mockinterceptor.MockUtils.SAVE_MOCK_MODE_RECORDING
import com.gustafah.android.mockinterceptor.MockUtils.mockResponse
import com.gustafah.android.mockinterceptor.MockUtils.processSavedData
import com.gustafah.android.mockinterceptor.extensions.arrayWithFilterAndArgs
import com.gustafah.android.mockinterceptor.extensions.first
import com.gustafah.android.mockinterceptor.extensions.isNotEmpty
import com.gustafah.android.mockinterceptor.extensions.mapData
import com.gustafah.android.mockinterceptor.persistence.MockInterceptorDatabase
import com.gustafah.android.mockinterceptor.persistence.entities.MockEntity
import com.gustafah.android.mockinterceptor.ui.MockImportDatabaseActivity
import com.gustafah.android.mockinterceptor.ui.MockOptionsActivity
import com.gustafah.android.mockinterceptor.ui.MockOptionsDialog
import com.gustafah.android.mockinterceptor.ui.MockReferenceActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.File
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

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    fun exportDatabase(context: Context) {
        val database = context.getDatabasePath(MockInterceptorDatabase.NAME)
        if (database.exists()) {
            val uri = FileProvider.getUriForFile(
                context,
                "com.gustafah.android.mockinterceptor.provider",
                database
            )
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "*/*"
            sharingIntent.putExtra(Intent.EXTRA_STREAM, uri)
            context.startActivity(Intent.createChooser(sharingIntent, "Sharing Mock Database"))
        } else {
            MockUtils.createDialog(context, "There is no database saved", "Ok")
        }
    }

    fun importDatabase(context: Context) {
        context.startActivity(Intent(context, MockImportDatabaseActivity::class.java))
    }

    fun deleteDatabase(context: Context) {
        val database = context.getDatabasePath(MockInterceptorDatabase.NAME)
        if (database.exists()) {
            MockUtils.createDialog(
                context,
                "Are you sure you want to delete the database?",
                "Yes",
                "No"
            ) { deleteAllDatabase(context) }
        } else {
            MockUtils.createDialog(context, "There is no database saved", "Ok")
        }
    }

    fun release(which: Int) = synchronized(lock) {
        optChoice.set(which)
        isAwaiting.set(false)
        countDownLatch.countDown()
    }

    private fun deleteAllDatabase(context: Context) {
        scope.launch {
            val database = context.getDatabasePath(MockInterceptorDatabase.NAME)
            if (database.exists()) {
                MockInterceptorDatabase.getInstance(context).clearAllTables()
                database.delete()
            }
        }
    }

    internal fun recreateDatabase(context: Context, file: File) {
        scope.launch {
            deleteAllDatabase(context)
            MockInterceptorDatabase.getInstance(context, file)
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        return when (config.saveMockMode) {
            SAVE_MOCK_MODE_RECORDING -> recordMockInfo(chain)
            SAVE_MOCK_MODE_PLAYBACK -> playbackMockInfo(chain)
            else -> readMockFromMockFiles(chain)
        }
    }

    private fun recordMockInfo(chain: Interceptor.Chain): Response {
        return kotlin.runCatching {
            chain.proceed(chain.request())
        }.onSuccess { mockResponse ->
            val mockDao = MockInterceptorDatabase
                .getInstance(config.context())
                .mockDao()
            val url = chain.request().url.toUrl().toString()
            mockDao.findMock(url)?.let {
                synchronized(mockFlow) {
                    displayOptions(
                        title = "Looks like you already have a mock with this name",
                        data = Pair(
                            arrayOf("Keep mock", "Replace mock"),
                            arrayOf(
                                "It will keep the mock file from database",
                                "It will replace the mock file from database"
                            )
                        )
                    )
                    waitValidation()

                    if (optChoice.get() == 1) {
                        mockDao.insertMock(
                            MockEntity(
                                chain.request().url.toUrl().toString(),
                                mockResponse.peekBody(Long.MAX_VALUE).string()
                            )
                        )
                    }
                }
            } ?: run {
                mockDao.insertMock(
                    MockEntity(
                        chain.request().url.toUrl().toString(),
                        mockResponse.peekBody(Long.MAX_VALUE).string()
                    )
                )
            }
        }.getOrThrow()
    }

    private fun playbackMockInfo(chain: Interceptor.Chain): Response {
        val request: Request = chain.request()
        val mockData = MockInterceptorDatabase
            .getInstance(config.context())
            .mockDao()
            .findMock(request.url.toUrl().toString())
        return mockResponse(
            if (mockData != null) 200 else 500,
            mockData?.fileData ?: ERROR_JSON_NOT_FOUND,
            request
        )
    }

    private fun readMockFromMockFiles(chain: Interceptor.Chain): Response {
        val request: Request = chain.request()
        val mockContent = config.fetchFileNameFromUrl(request)
        mockContent.let {
            return when (it.names()?.length() ?: 0) {
                0 -> mockResponse(
                    code = 502,
                    jsonResponse = it.toString(),
                    request = request
                )
                1 -> {
                    val json = JSONObject(it.getString(it.names()?.getString(0)!!))
                    if (json.has("type"))
                        mockResponse(code = 502, jsonResponse = json.toString(), request = request)
                    else
                        makeMockResponse(json, request)
                }
                else -> makeMultiMockResponse(it, request)
            }
        }
    }

    private fun makeMultiMockResponse(content: JSONObject, request: Request): Response {
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
            return@synchronized makeMockResponse(
                content.getJSONObject(nameArray[auxOpt]),
                request
            )
        }
    }

    private fun makeMockResponse(content: JSONObject, request: Request): Response {
        val arraySavedData = content.getJSONArray(JSON_FIELD_SAVED_DATA)
        val arrayFilteredSavedData = arraySavedData.arrayWithFilterAndArgs(config.requestArguments)
        val jsonArray = if (arrayFilteredSavedData.isNotEmpty()) arrayFilteredSavedData
        else arraySavedData
        val default = content.getInt(MockUtils.JSON_FIELD_DEFAULT)

        return processSavedData(config.context(), when {
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
    }

    private fun displayReferences(title: Array<String>) {
        val currentContext = config.context() as AppCompatActivity
        currentContext.startActivity(
            MockReferenceActivity.makeIntent(currentContext, title)
        )
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