package com.gustafah.android.mockinterceptor.ui

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.gustafah.android.mockinterceptor.MockUtils.BUNDLE_FIELD_TITLE

class MockReferenceActivity : AppCompatActivity(), DialogInterface.OnDismissListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.extras?.let {
            MockReferenceDialog.newInstance(
                it.getStringArray(BUNDLE_FIELD_TITLE) ?: emptyArray()
            ).show(supportFragmentManager, "MockReferenceDialog")
        }
    }

    companion object {
        fun makeIntent(
            context: Context,
            title: Array<String>
        ) = Intent(context, MockReferenceActivity::class.java).apply {
            putExtra(BUNDLE_FIELD_TITLE, title)
        }
    }

    override fun onDismiss(dialog: DialogInterface?) {
        finish()
    }
}
