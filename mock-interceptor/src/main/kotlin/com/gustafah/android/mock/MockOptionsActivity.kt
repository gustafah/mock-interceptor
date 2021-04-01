package com.gustafah.android.mock

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MockOptionsActivity : AppCompatActivity(), DialogInterface.OnDismissListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.extras?.let {
            MockOptionsDialog.newInstance(
                it.getString(BUNDLE_FIELD_TITLE, ""),
                it.getStringArray(BUNDLE_FIELD_TEXT) ?: emptyArray(),
                it.getStringArray(BUNDLE_FIELD_SUBTEXT) ?: emptyArray()
            ).show(supportFragmentManager, "MockOptionsDialog")
        }
    }

    companion object {
        fun makeIntent(
            context: Context,
            title: String,
            text: Array<String>,
            subtext: Array<String>
        ) =
            Intent(context, MockOptionsActivity::class.java).apply {
                putExtra(BUNDLE_FIELD_TITLE, title)
                putExtra(BUNDLE_FIELD_TEXT, text)
                putExtra(BUNDLE_FIELD_SUBTEXT, subtext)
            }
    }

    override fun onDismiss(dialog: DialogInterface?) {
        finish()
    }
}
