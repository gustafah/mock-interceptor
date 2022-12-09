package com.gustafah.android.mockinterceptor

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import com.gustafah.android.mockinterceptor.MockConfig.OptionsSelectorMode.NO_SELECTION
import com.gustafah.android.mockinterceptor.MockFileUtils.FILE_EXTENSION_DB
import com.gustafah.android.mockinterceptor.MockFileUtils.FILE_EXTENSION_JSON
import com.gustafah.android.mockinterceptor.MockFileUtils.FILE_EXTENSION_ZIP
import com.gustafah.android.mockinterceptor.MockFileUtils.addJsonToDatabase
import com.gustafah.android.mockinterceptor.MockFileUtils.copy
import com.gustafah.android.mockinterceptor.MockFileUtils.deleteRecursive
import com.gustafah.android.mockinterceptor.MockFileUtils.getFileNameByRequest
import com.gustafah.android.mockinterceptor.MockFileUtils.unzipFileAtPath
import com.gustafah.android.mockinterceptor.MockFileUtils.writeToFile
import com.gustafah.android.mockinterceptor.MockFileUtils.zipFileAtPath
import com.gustafah.android.mockinterceptor.MockUtils.ERROR_JSON_NOT_FOUND
import com.gustafah.android.mockinterceptor.MockUtils.JSON_FIELD_MULTI
import com.gustafah.android.mockinterceptor.MockUtils.JSON_FIELD_REFERENCE
import com.gustafah.android.mockinterceptor.MockUtils.JSON_FIELD_SAVED_DATA
import com.gustafah.android.mockinterceptor.MockUtils.RESPONSE_CODE_INTERNAL_ERROR
import com.gustafah.android.mockinterceptor.MockUtils.RESPONSE_CODE_SUCCESS
import com.gustafah.android.mockinterceptor.MockUtils.mockResponse
import com.gustafah.android.mockinterceptor.MockUtils.processSavedData
import com.gustafah.android.mockinterceptor.MockUtils.shareFile
import com.gustafah.android.mockinterceptor.extensions.arrayWithFilterAndArgs
import com.gustafah.android.mockinterceptor.extensions.first
import com.gustafah.android.mockinterceptor.extensions.isNotEmpty
import com.gustafah.android.mockinterceptor.extensions.mapData
import com.gustafah.android.mockinterceptor.persistence.MockInterceptorDatabase
import com.gustafah.android.mockinterceptor.persistence.entities.MockEntity
import com.gustafah.android.mockinterceptor.ui.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.nio.charset.Charset
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

    fun exportDatabase() {
        config.context()
            .startActivity(Intent(config.context(), MockExportDatabaseActivity::class.java))
    }

    fun importDatabase() {
        config.context()
            .startActivity(Intent(config.context(), MockImportDatabaseActivity::class.java))
    }

    fun deleteDatabase() {
        val context = config.context()
        val database = context.getDatabasePath(MockInterceptorDatabase.NAME)
        if (database.exists()) {
            MockUtils.createDialog(
                context,
                context.getString(R.string.mock_delete_database_title),
                context.getString(R.string.mock_alert_button_yes),
                context.getString(R.string.mock_alert_button_no)
            ) { deleteAllDatabase() }
        } else {
            MockUtils.createDialog(
                context,
                context.getString(R.string.mock_delete_database_error),
                context.getString(R.string.mock_alert_button_ok)
            )
        }
    }

    fun release(which: Int) = synchronized(lock) {
        optChoice.set(which)
        isAwaiting.set(false)
        countDownLatch.countDown()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        return when (config.saveMockMode) {
            MockConfig.OptionRecordMock.RECORD -> recordMockInfo(chain)
            MockConfig.OptionRecordMock.PLAYBACK -> playbackMockInfo(chain)
            else -> readMockFromMockFiles(chain)
        }
    }

    internal fun recreateDatabase(uri: Uri, contentResolver: ContentResolver) =
        scope.launch(Dispatchers.IO) {
            val context = config.context()
            val file = File(
                MockFileUtils.writeFileContent(
                    uri,
                    context.getExternalFilesDir(null),
                    contentResolver
                ) ?: ""
            )
            when (file.path.substring(file.path.lastIndexOf("."))) {
                FILE_EXTENSION_DB -> {
                    importDatabaseContent(file)
                }
                FILE_EXTENSION_JSON -> addJsonToDatabase(context, file)
                FILE_EXTENSION_ZIP -> {
                    unzipFileAtPath(file, context.filesDir.parentFile)
                    context.filesDir?.listFiles()?.forEach { json ->
                        addJsonToDatabase(context, json)
                    }
                }
                else -> throw IllegalAccessError("")
            }
        }

    private fun importDatabaseContent(file: File) =
        scope.launch {
            val context = config.context()
            displayOptions(
                title = context.getString(R.string.mock_import_database_title),
                data = Pair(
                    arrayOf(
                        context.getString(R.string.mock_import_database_option_clean),
                        context.getString(R.string.mock_import_database_option_replace)
                    ),
                    arrayOf(
                        context.getString(R.string.mock_import_database_option_clean_description),
                        context.getString(R.string.mock_import_database_option_replace_description)
                    )
                )
            )
            waitValidation()
            if (optChoice.get() == 0) importDatabaseInCleanMode(file, context)
            else importDatabaseReplacing(file, context)
            optChoice.set(-1)
        }

    internal fun exportDatabaseContent() =
        scope.launch {
            val context = config.context()
            displayOptions(
                title = context.getString(R.string.mock_save_database_title),
                data = Pair(
                    arrayOf(
                        context.getString(R.string.mock_save_database_option_db_file),
                        context.getString(R.string.mock_save_database_option_json_file)
                    ),
                    arrayOf(
                        context.getString(R.string.mock_save_database_option_db_file_description),
                        context.getString(R.string.mock_save_database_option_json_file_description)
                    )
                )
            )
            waitValidation()
            if (optChoice.get() == 0) exportDatabaseInDBFile()
            else exportDatabaseInJsonFiles()
            optChoice.set(-1)
        }

    private fun recordMockInfo(chain: Interceptor.Chain): Response {
        return kotlin.runCatching {
            chain.proceed(chain.request())
        }.onSuccess { mockResponse ->
            val context = config.context()
            val mockDao = MockInterceptorDatabase
                .getInstance(context)
                .mockDao()
            val url = getFileNameByRequest(chain.request())
            mockDao.findMock(url)?.let {
                synchronized(mockFlow) {
                    displayOptions(
                        title = context.getString(R.string.mock_already_saved_title, url),
                        data = Pair(
                            arrayOf(
                                context.getString(R.string.mock_already_saved_option_keep_mock),
                                context.getString(R.string.mock_already_saved_option_replace_mock)
                            ),
                            arrayOf(
                                context.getString(R.string.mock_already_saved_option_keep_mock_description),
                                context.getString(R.string.mock_already_saved_option_replace_mock_description)
                            )
                        )
                    )
                    waitValidation()

                    if (optChoice.get() == 1) {
                        mockDao.insertMock(
                            MockEntity(
                                getFileNameByRequest(chain.request()),
                                mockResponse.peekBody(Long.MAX_VALUE).string()
                            )
                        )
                        optChoice.set(-1)
                    }
                }
            } ?: run {
                val responseBody = mockResponse.body
                val source = responseBody?.source()
                source?.request(Long.MAX_VALUE)
                val buffer = source?.buffer
                val info = buffer?.clone()?.readString(Charset.defaultCharset())
                mockDao.insertMock(
                    MockEntity(
                        getFileNameByRequest(chain.request()),
                        info.toString()
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
            .findMock(getFileNameByRequest(request))
        return mockResponse(
            if (mockData != null) RESPONSE_CODE_SUCCESS else RESPONSE_CODE_INTERNAL_ERROR,
            mockData?.fileData ?: ERROR_JSON_NOT_FOUND,
            request
        )
    }

    private fun readMockFromMockFiles(chain: Interceptor.Chain): Response {
        val request: Request = chain.request()
        val mockContent = config.fetchMockContentFromRequest(request)
        val response = mockContent?.let {
            if (it.remove(JSON_FIELD_MULTI) == true) {
                pickMultiMockResponse(it, request)
            } else {
                if (it.has("type"))
                    mockResponse(code = 502, jsonResponse = it.toString(), request = request)
                else
                    pickMockResponse(it, request)
            }
        } ?: mockResponse(
            code = 502,
            jsonResponse = MockUtils.ERROR_JSON_NO_DATA,
            request = request
        )
        Thread.sleep((config.delay.lower..config.delay.upper).random().toLong())
        return response
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

    private fun importDatabaseInCleanMode(file: File, context: Context) {
        deleteAllDatabase()
        MockInterceptorDatabase.getInstance(context, file)
    }

    private fun importDatabaseReplacing(file: File, context: Context) {
        val database = context.getDatabasePath(MockInterceptorDatabase.NAME)
        copy(file, database)
    }

    private fun exportDatabaseInDBFile() {
        val context = config.context()
        val database = context.getDatabasePath(MockInterceptorDatabase.NAME)
        if (database.exists()) {
            shareFile(context, database)
        } else {
            MockUtils.createDialog(
                context,
                context.getString(R.string.mock_delete_database_error),
                context.getString(R.string.mock_alert_button_ok)
            )
        }
    }

    private fun exportDatabaseInJsonFiles() {
        val context = config.context()
        scope.launch {
            val allMocks = MockInterceptorDatabase.getInstance(context).mockDao().getAllMocks()
            allMocks?.forEach { writeToFile(it.fileName, it.fileData, context) }
            zipFileAtPath(context.filesDir.path, context.filesDir.path + FILE_EXTENSION_ZIP)
            if (context.filesDir.exists()) deleteRecursive(context.filesDir)
            shareFile(context, File(context.filesDir.path + FILE_EXTENSION_ZIP))
        }
    }

    private fun deleteAllDatabase() =
        scope.launch {
            val context = config.context()
            kotlin.runCatching {
                MockInterceptorDatabase.getInstance(context).clearAllTables()
                val database = context.getDatabasePath(MockInterceptorDatabase.NAME)
                if (database.exists()) database.delete()
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