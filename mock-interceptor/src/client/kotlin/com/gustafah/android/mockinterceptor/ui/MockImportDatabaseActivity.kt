package com.gustafah.android.mockinterceptor.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.gustafah.android.mockinterceptor.MockInterceptor
import com.gustafah.android.mockinterceptor.MockFileUtils.writeFileContent
import com.gustafah.android.mockinterceptor.MockUtils.ERROR_FILE_NOT_FOUND
import com.gustafah.android.mockinterceptor.R
import java.io.File

class MockImportDatabaseActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = Intent(Intent.ACTION_OPEN_DOCUMENT)
        data.type = "*/*"
        startActivityForResult(
            Intent.createChooser(data, getString(R.string.mock_file_chooser_import_yes)),
            3455
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 3455) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
                if (data != null && data.data != null) {
                    val file = File(
                        writeFileContent(
                            data.data!!,
                            getExternalFilesDir(null),
                            contentResolver
                        ) ?: ""
                    )
                    MockInterceptor.recreateDatabase(this, file)
                } else {
                    Log.e(MockImportDatabaseActivity::class.simpleName, ERROR_FILE_NOT_FOUND)
                }
            }
        }
        finish()
    }

}