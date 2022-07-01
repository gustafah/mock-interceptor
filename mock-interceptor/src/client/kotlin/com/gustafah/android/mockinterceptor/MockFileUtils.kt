package com.gustafah.android.mockinterceptor

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.gustafah.android.mockinterceptor.MockUtils.ERROR_WRITING_FILE
import com.gustafah.android.mockinterceptor.persistence.MockInterceptorDatabase
import com.gustafah.android.mockinterceptor.persistence.entities.MockEntity
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object MockFileUtils {

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

    internal fun writeToFile(fileName: String, data: String, context: Context) {
        try {
            val outputStreamWriter =
                OutputStreamWriter(context.openFileOutput(fileName, Context.MODE_PRIVATE))
            outputStreamWriter.write(data)
            outputStreamWriter.close()
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

    internal fun addJsonToDatabase(context: Context, json: File) {
        val br = BufferedReader(FileReader(json))
        var line: String?
        val data = StringBuilder()

        while (br.readLine().also { line = it } != null) {
            data.append(line)
            data.append('\n')
        }
        br.close()
        MockInterceptorDatabase.getInstance(context).mockDao().insertMock(
            MockEntity(json.name, data.toString())
        )
    }

    fun zipFileAtPath(sourcePath: String, toLocation: String?): Boolean {
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
                val entry = ZipEntry(getLastPathComponent(sourcePath))
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

    private fun getLastPathComponent(filePath: String): String {
        val segments = filePath.split("/").toTypedArray()
        return if (segments.isEmpty()) "" else segments[segments.size - 1]
    }

    fun unzipFileAtPath(zipFile: File?, targetDirectory: File?) {
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

}