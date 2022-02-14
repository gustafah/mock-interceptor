package com.gustafah.android.mockinterceptor.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.gustafah.android.mockinterceptor.MockInterceptor
import java.io.*

class MockImportDatabaseActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = Intent(Intent.ACTION_OPEN_DOCUMENT)
        data.type = "*/*"
        startActivityForResult(
            Intent.createChooser(data, "Choose file to import"),
            3455
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 3455) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
                if (data != null && data.data != null) {
                    val file = File(writeFileContent(data.data!!) ?: "")
                    MockInterceptor.recreateDatabase(this, file)
                } else {
                    Log.d(MockImportDatabaseActivity::class.simpleName, "File uri not found {}")
                }
                finish()
            }
        }
    }

    @Throws(IOException::class)
    private fun writeFileContent(uri: Uri): String? {
        val selectedFileInputStream: InputStream? = contentResolver.openInputStream(uri)
        if (selectedFileInputStream != null) {
            val certCacheDir = File(getExternalFilesDir(null), "Mock Interceptor")
            var isCertCacheDirExists = certCacheDir.exists()
            if (!isCertCacheDirExists) {
                isCertCacheDirExists = certCacheDir.mkdirs()
            }
            if (isCertCacheDirExists) {
                val filePath = certCacheDir.absolutePath + "/" + getFileDisplayName(uri)
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

    @SuppressLint("Range")
    private fun getFileDisplayName(uri: Uri): String? {
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

}