package com.gustafah.android.mockinterceptor.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.gustafah.android.mockinterceptor.MockDatabaseUtils
import com.gustafah.android.mockinterceptor.MockUtils.ERROR_FILE_NOT_FOUND
import com.gustafah.android.mockinterceptor.R

internal const val IMPORT_DATABASE_MODE = "import_database_mode"
internal const val UPDATE_INFO = 3455
internal const val CLEAR_DATABASE = 6584

class MockImportDatabaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = Intent(Intent.ACTION_OPEN_DOCUMENT)
        data.type = "*/*"
        startActivityForResult(
            Intent.createChooser(data, getString(R.string.mock_file_chooser_import_yes)),
            intent.getIntExtra(IMPORT_DATABASE_MODE, UPDATE_INFO)
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (data != null && data.data != null) {
                val info = data.data!!
                when (requestCode) {
                    CLEAR_DATABASE -> {
                        MockDatabaseUtils.replaceDatabase(info, contentResolver)
                    }
                    else -> {
                        MockDatabaseUtils.recreateDatabase(info, contentResolver)
                    }
                }
            } else {
                Log.e(MockImportDatabaseActivity::class.simpleName, ERROR_FILE_NOT_FOUND)
            }
        }
        finish()
    }

}