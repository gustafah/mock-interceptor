package com.gustafah.android.mockinterceptor.sample.ui

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.gustafah.android.mockinterceptor.MockInterceptor
import com.gustafah.android.mockinterceptor.MockUtils
import com.gustafah.android.mockinterceptor.notification.MockNotification
import com.gustafah.android.mockinterceptor.sample.R
import com.gustafah.android.mockinterceptor.sample.repository.SampleRepository
import com.gustafah.android.mockinterceptor.sample.service.serviceClient
import com.gustafah.android.mockinterceptor.sample.ui.viewmodel.SampleViewModel
import kotlinx.android.synthetic.main.activity_sample.*


class SampleActivity : AppCompatActivity(R.layout.activity_sample),
    DialogInterface.OnDismissListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        val saveMockMode = getPreferences(Context.MODE_PRIVATE).getInt(
            "SAVE_MOCK_MODE",
            MockUtils.SAVE_MOCK_MODE_NONE
        )
        val isOnSaveMockMode = saveMockMode != MockUtils.SAVE_MOCK_MODE_NONE

        val repository = SampleRepository(serviceClient(context = this, saveMockMode))
        val viewModel = SampleViewModel(repository)

        addOnRadioButton("Response from Mock File", MockUtils.SAVE_MOCK_MODE_NONE, saveMockMode)
        addOnRadioButton("Record API response and save on Database", MockUtils.SAVE_MOCK_MODE_RECORDING, saveMockMode)
        addOnRadioButton("Playback API response from Database", MockUtils.SAVE_MOCK_MODE_PLAYBACK, saveMockMode)

        button_save_db.setOnClickListener {
            val checkedView = radio_group.findViewById<View>(radio_group.checkedRadioButtonId)
            with(getPreferences(Context.MODE_PRIVATE).edit()) {
                putInt("SAVE_MOCK_MODE", checkedView.tag.toString().toInt())
                apply()
            }
            triggerRebirth()
        }


        linear_database.isVisible = isOnSaveMockMode
        linear_mock_file.isVisible = !isOnSaveMockMode
        button_fetch_response.setOnClickListener {
            viewModel.fetchResponse()
        }

        button_export_database.setOnClickListener {
            MockInterceptor.exportDatabase(this)
        }
        button_import_database.setOnClickListener {
            MockInterceptor.importDatabase(this)
        }
        button_delete_database.setOnClickListener {
            MockInterceptor.deleteDatabase(this)
        }
        button_sample1.setOnClickListener {
            viewModel.fetchResponse()
        }
        button_sample2.setOnClickListener {
            viewModel.fetchResponse2()
        }
        button_sample3.setOnClickListener {
            viewModel.fetchResponse3()
        }
        button_sample4.setOnClickListener {
            MockNotification.showMockNotification(this)
        }
        viewModel.responseLiveData.observe(this) {
            it.forEach { data -> println(data) }
        }
    }

    private fun addOnRadioButton(text: String, tag: Int, saveMockMode: Int) {
        val radioButton = RadioButton(this).apply {
            this.id = View.generateViewId()
            this.text = text
            this.tag = tag
        }
        radio_group.addView(radioButton)
        radioButton.isChecked = saveMockMode == tag
    }

    private fun triggerRebirth() {
        AlertDialog.Builder(this)
            .setMessage("The page will need to be reloaded")
            .setPositiveButton(
                "Ok"
            ) { _, _ ->
                val intent = Intent(this, SampleActivity::class.java)
                intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
                Runtime.getRuntime().exit(0)
            }
            .create()
            .show()

    }

    override fun onDismiss(p0: DialogInterface?) {}

}