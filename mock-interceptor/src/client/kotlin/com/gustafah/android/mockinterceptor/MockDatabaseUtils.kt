package com.gustafah.android.mockinterceptor

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.gustafah.android.mockinterceptor.MockFileUtils.DIRECTORY_NAME
import com.gustafah.android.mockinterceptor.persistence.MockDatabaseState
import com.gustafah.android.mockinterceptor.persistence.MockInterceptorDatabase
import com.gustafah.android.mockinterceptor.persistence.entities.MockEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object MockDatabaseUtils {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    internal val databaseObserver: MutableLiveData<MockDatabaseState> = MutableLiveData()

    internal fun exportDatabaseInDBFile() {
        val context = MockInterceptor.config.context()
        val database = context.getDatabasePath(MockInterceptorDatabase.NAME)
        if (database.exists()) {
            MockUtils.shareFile(context, database)
        } else {
            MockUtils.createDialog(
                context,
                context.getString(R.string.mock_delete_database_error),
                context.getString(R.string.mock_alert_button_ok)
            )
        }
    }

    internal fun exportDatabaseInJsonFiles() {
        val context = MockInterceptor.config.context()

        scope.launch {
            try {
                updateObserver(MockDatabaseState.LOADING)
                val directory = File(context.getExternalFilesDir(null), DIRECTORY_NAME)
                val allMocks = MockInterceptorDatabase.getInstance(context).mockDao().getAllMocks()
                allMocks?.forEach { MockFileUtils.writeToFile(directory, it.id, it.fileName, it.fileData, context) }
                zipFileAtPath(
                    directory.path,
                    context.filesDir.path + MockFileUtils.FILE_EXTENSION_ZIP
                )
                if (directory.exists()) MockFileUtils.deleteRecursive(directory)
                updateObserver(MockDatabaseState.SUCCESS)
                MockUtils.shareFile(
                    context,
                    File(context.filesDir.path + MockFileUtils.FILE_EXTENSION_ZIP)
                )
            } catch (ex: Exception) {
                updateObserver(MockDatabaseState.ERROR)
            }
        }
    }

    internal fun recreateDatabase(uri: Uri, contentResolver: ContentResolver) =
        scope.launch(Dispatchers.IO) {
            try {
                updateObserver(MockDatabaseState.LOADING)
                val context = MockInterceptor.config.context()
                val file = File(
                    MockFileUtils.writeFileContent(
                        uri,
                        context.getExternalFilesDir(null),
                        contentResolver
                    ) ?: ""
                )
                when (file.path.substring(file.path.lastIndexOf("."))) {
                    MockFileUtils.FILE_EXTENSION_DB -> importDatabaseInCleanMode(file, context)
                    MockFileUtils.FILE_EXTENSION_JSON -> addJsonToDatabase(context, MockInterceptor.config.mockGroupIdentifier ?: "", file)
                    MockFileUtils.FILE_EXTENSION_ZIP -> importZipToDatabase(context, file)
                    else -> throw IllegalAccessError("")
                }
                updateObserver(MockDatabaseState.SUCCESS)
            } catch (ex: Exception) {
                updateObserver(MockDatabaseState.ERROR)
            }
        }

    internal fun replaceDatabase(uri: Uri, contentResolver: ContentResolver) =
        scope.launch(Dispatchers.IO) {
            try {
                updateObserver(MockDatabaseState.LOADING)
                val context = MockInterceptor.config.context()
                val file = File(
                    MockFileUtils.writeFileContent(
                        uri,
                        context.getExternalFilesDir(null),
                        contentResolver
                    ) ?: ""
                )
                importDatabaseReplacing(file, context)
                updateObserver(MockDatabaseState.SUCCESS)
            } catch (ex: Exception) {
                updateObserver(MockDatabaseState.ERROR)
            }
        }

    internal fun deleteAllDatabase() =
        scope.launch {
            val context = MockInterceptor.config.context()
            kotlin.runCatching {
                MockInterceptorDatabase.getInstance(context).clearAllTables()
                val database = context.getDatabasePath(MockInterceptorDatabase.NAME)
                if (database.exists()) database.delete()
            }
        }

    private suspend fun updateObserver(data: MockDatabaseState) {
        withContext(Dispatchers.Main) {
            databaseObserver.value = data
        }
    }

    private fun importDatabaseInCleanMode(file: File, context: Context) {
        deleteAllDatabase()
        MockInterceptorDatabase.getInstance(context, file)
    }

    private fun importDatabaseReplacing(file: File, context: Context) {
        val database = context.getDatabasePath(MockInterceptorDatabase.NAME)
        MockFileUtils.copy(file, database)
    }

    private fun addJsonToDatabase(context: Context, identifier: String, json: File) {
        val br = BufferedReader(FileReader(json))
        var line: String?
        val data = StringBuilder()

        while (br.readLine().also { line = it } != null) {
            data.append(line)
            data.append('\n')
        }
        br.close()
        MockInterceptorDatabase.getInstance(context).mockDao().insertMock(
            MockEntity(identifier, json.name, data.toString())
        )
    }

    private fun importZipToDatabase(context: Context, file: File) {
        val directory = File(context.filesDir.parentFile.path + "/" + DIRECTORY_NAME)
        if (!directory.exists()) directory.mkdirs()
        unzipFileAtPath(file, directory)
        loadFilesToDatabase(context, "", directory)
        if (directory.exists()) MockFileUtils.deleteRecursive(directory)
    }

    private fun loadFilesToDatabase(context: Context, id: String, dir: File) {
        dir.listFiles()?.forEach { file ->
            val fileName = if (file.name == DIRECTORY_NAME) "" else file.name
            if (file.isDirectory) loadFilesToDatabase(context, fileName, file)
            else addJsonToDatabase(context, id, file)
        }
    }

    private fun unzipFileAtPath(zipFile: File?, targetDirectory: File?) {
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var count: Int
            val buffer = ByteArray(8192)
            while (true) {
                val ze = zis.nextEntry ?: break
                val file = File(targetDirectory, ze.name)
                val dir = if (ze.isDirectory) file else file.parentFile
                if (!dir.isDirectory && !dir.mkdirs()) throw FileNotFoundException(
                    "Failed to ensure directory: " +
                            dir.absolutePath
                )
                if (ze.isDirectory) continue
                FileOutputStream(file).use { fout ->
                    while (zis.read(buffer).also { count = it } != -1) fout.write(buffer, 0, count)
                }
            }
        }
    }

    private fun zipFileAtPath(sourcePath: String, toLocation: String?): Boolean {
        val BUFFER = 2048
        val sourceFile = File(sourcePath)
        try {
            val dest = FileOutputStream(toLocation)
            val out = ZipOutputStream(
                BufferedOutputStream(dest)
            )
            if (sourceFile.isDirectory) {
                zipSubFolder(out, sourceFile, sourceFile.parent?.length ?: 0)
            } else {
                val data = ByteArray(BUFFER)
                val fi = FileInputStream(sourcePath)
                val origin = BufferedInputStream(fi, BUFFER)
                val entry = ZipEntry(MockFileUtils.getLastPathComponent(sourcePath))
                entry.time = sourceFile.lastModified()
                out.putNextEntry(entry)
                var count: Int
                while (origin.read(data, 0, BUFFER).also { count = it } != -1) {
                    out.write(data, 0, count)
                }
            }
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    @Throws(IOException::class)
    private fun zipSubFolder(
        out: ZipOutputStream, folder: File,
        basePathLength: Int
    ) {
        val BUFFER = 2048
        val fileList = folder.listFiles()
        var origin: BufferedInputStream?
        fileList?.forEach { file ->
            if (file.isDirectory) {
                zipSubFolder(out, file, basePathLength)
            } else {
                val data = ByteArray(BUFFER)
                val unmodifiedFilePath = file.path
                val relativePath = unmodifiedFilePath.substring(basePathLength)
                val fi = FileInputStream(unmodifiedFilePath)
                origin = BufferedInputStream(fi, BUFFER)
                val entry = ZipEntry(relativePath)
                entry.time = file.lastModified()
                out.putNextEntry(entry)
                var count = 0
                while (origin?.read(data, 0, BUFFER).also { value ->
                        value?.let { count = value }
                    } != -1) {
                    out.write(data, 0, count)
                }
                origin?.close()
            }
        }
    }


}