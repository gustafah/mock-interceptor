package com.gustafah.android.mockinterceptor

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.gustafah.android.mockinterceptor.MockUtils.ERROR_WRITING_FILE
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter

object MockFileUtils {

    const val DIRECTORY_NAME = "mock_files"
    const val FILE_EXTENSION_JSON = ".json"
    const val FILE_EXTENSION_ZIP = ".zip"
    const val FILE_EXTENSION_DB = ".db"

    internal fun getFileNameByRequest(request: Request) : String =
        request.url.pathSegments.joinToString("_") + FILE_EXTENSION_JSON

    @Throws(IOException::class)
    internal fun writeFileContent(uri: Uri, externalFileDir: File?, contentResolver: ContentResolver): String? {
        val selectedFileInputStream: InputStream? = contentResolver.openInputStream(uri)
        if (selectedFileInputStream != null) {
            val certCacheDir = File(externalFileDir, "Mock Interceptor")
            var isCertCacheDirExists = certCacheDir.exists()
            if (!isCertCacheDirExists) {
                isCertCacheDirExists = certCacheDir.mkdirs()
            }
            if (isCertCacheDirExists) {
                val filePath = certCacheDir.absolutePath + "/" + getFileDisplayName(uri, contentResolver)
                val selectedFileOutPutStream: OutputStream = FileOutputStream(filePath)
                val buffer = ByteArray(Long.SIZE_BYTES)
                var length: Int
                while (selectedFileInputStream.read(buffer).also { length = it } > 0) {
                    selectedFileOutPutStream.write(buffer, 0, length)
                }
                selectedFileOutPutStream.flush()
                selectedFileOutPutStream.close()
                return filePath
            }
            selectedFileInputStream.close()
        }
        return null
    }

    internal fun copy(source: File, destination: File) {
        val inFile = FileInputStream(source).channel
        val outFile = FileOutputStream(destination).channel

        try {
            inFile.transferTo(0, inFile.size(), outFile)
        } catch (e: java.lang.Exception) {
            Log.e(MockUtils::class.simpleName, e.message ?: ERROR_WRITING_FILE)
        } finally {
            inFile.close()
            outFile.close()
        }
    }

    private fun getFileDisplayName(uri: Uri, contentResolver: ContentResolver): String? {
        var displayName: String? = null
        contentResolver
            .query(uri, null, null, null, null, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    displayName = cursor.getString(
                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    )
                }
            }
        return displayName
    }

    internal fun writeToFile(directory: File, id: String, fileName: String, data: String, context: Context) {
        try {
            if (!directory.exists()) directory.mkdirs()
            val subDirectory = if (id != "") {
                val file = File(directory, id)
                if (!file.exists()) file.mkdirs()
                file
            } else directory
            val file = File(subDirectory, fileName)

            val outputStream = FileOutputStream(file)
            val outputStreamWriter = OutputStreamWriter(outputStream)

            outputStreamWriter.write(data)

            outputStreamWriter.close()
            outputStream.close()
        } catch (e: IOException) {
            Log.e(MockUtils::class.simpleName, e.message ?: ERROR_WRITING_FILE)
        }
    }

    internal fun deleteRecursive(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory) {
            fileOrDirectory.listFiles()?.forEach { deleteRecursive(it) }
        }
        fileOrDirectory.delete()
    }

    internal fun getLastPathComponent(filePath: String): String {
        val segments = filePath.split("/").toTypedArray()
        return if (segments.isEmpty()) "" else segments[segments.size - 1]
    }
}