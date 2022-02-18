package com.gustafah.android.mockinterceptor.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import com.gustafah.android.mockinterceptor.MockInterceptor

class MockExportDatabaseActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkWriteExternalPermission()) {
            MockInterceptor.exportDatabaseContent(this)
            finish()
        } else {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 3456)
        }
    }

    private fun checkWriteExternalPermission(): Boolean {
        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        val res: Int = checkCallingOrSelfPermission(permission)
        return res == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        finish()
    }

}