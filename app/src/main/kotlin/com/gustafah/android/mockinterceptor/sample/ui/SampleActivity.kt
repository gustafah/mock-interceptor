package com.gustafah.android.mockinterceptor.sample.ui

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.gustafah.android.mockinterceptor.MockConfig
import com.gustafah.android.mockinterceptor.MockInterceptor
import com.gustafah.android.mockinterceptor.notification.MockNotification
import com.gustafah.android.mockinterceptor.sample.R
import com.gustafah.android.mockinterceptor.sample.repository.SampleRepository
import com.gustafah.android.mockinterceptor.sample.service.serviceClient
import com.gustafah.android.mockinterceptor.sample.ui.viewmodel.SampleViewModel
import kotlinx.android.synthetic.main.activity_sample.*


class SampleActivity : AppCompatActivity(R.layout.activity_sample) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        val saveMockMode = getPreferences(Context.MODE_PRIVATE).getInt(
            "SAVE_MOCK_MODE",
            MockConfig.OptionRecordMock.DISABLED.ordinal
        )
        val replaceMockMode = getPreferences(Context.MODE_PRIVATE).getInt(
            "REPLACE_MOCK_MODE",
            MockConfig.ReplaceMockOption.DEFAULT.ordinal
        )
        val isOnSaveMockMode = saveMockMode != MockConfig.OptionRecordMock.DISABLED.ordinal
        val mockOption = MockConfig.OptionRecordMock.values()[saveMockMode]
        val replaceOption = MockConfig.ReplaceMockOption.values()[replaceMockMode]

        val repository = SampleRepository(serviceClient(context = this, mockOption, replaceOption))
        val viewModel = SampleViewModel(repository)


        setupSaveModeRadioButton(saveMockMode)
        setupRecordRadioButton(replaceMockMode)

        linear_database.isVisible = isOnSaveMockMode
        linear_mock_file.isVisible = !isOnSaveMockMode
        radio_button_record_mode.isVisible =
            saveMockMode == MockConfig.OptionRecordMock.RECORD.ordinal

        setupMocksFromDatabase(viewModel, saveMockMode)
        setupMocksFromMockFile(viewModel)

        viewModel.responseLiveData.observe(this) {
            it.forEach { data ->
                println(data.toJson())
                text_response.append(data.toJson())
            }
        }
        viewModel.responseErrorLiveData.observe(this) {
            AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(it)
                .setPositiveButton(
                    "Ok"
                ) { _, _ -> }
                .show()
        }
    }

    private fun setupSaveModeRadioButton(saveMockMode: Int) {
        addOnRadioButton(
            radio_group,
            "Response from Mock File",
            MockConfig.OptionRecordMock.DISABLED.ordinal,
            saveMockMode
        )
        addOnRadioButton(
            radio_group,
            "Record API response and save on Database",
            MockConfig.OptionRecordMock.RECORD.ordinal,
            saveMockMode
        )
        addOnRadioButton(
            radio_group,
            "Playback API response from Database",
            MockConfig.OptionRecordMock.PLAYBACK.ordinal,
            saveMockMode
        )

        button_save_db.setOnClickListener {
            val checkedView = radio_group.findViewById<View>(radio_group.checkedRadioButtonId)
            with(getPreferences(Context.MODE_PRIVATE).edit()) {
                putInt("SAVE_MOCK_MODE", checkedView.tag.toString().toInt())
                apply()
            }
            triggerRebirth()
        }
    }

    private fun setupRecordRadioButton(replaceMockMode: Int) {
        addOnRadioButton(
            radio_button_record_mode,
            "Default",
            MockConfig.ReplaceMockOption.DEFAULT.ordinal,
            replaceMockMode
        )
        addOnRadioButton(
            radio_button_record_mode,
            "Keep Mock",
            MockConfig.ReplaceMockOption.KEEP_MOCK.ordinal,
            replaceMockMode
        )
        addOnRadioButton(
            radio_button_record_mode,
            "Replace Mock",
            MockConfig.ReplaceMockOption.REPLACE_MOCK.ordinal,
            replaceMockMode
        )
        button_record_mode_save.setOnClickListener {
            val checkedView = radio_button_record_mode.findViewById<View>(radio_button_record_mode.checkedRadioButtonId)
            with(getPreferences(Context.MODE_PRIVATE).edit()) {
                putInt("REPLACE_MOCK_MODE", checkedView.tag.toString().toInt())
                apply()
            }
            triggerRebirth()
        }
    }

    private fun setupMocksFromDatabase(viewModel: SampleViewModel, saveMockMode: Int) {
        button_fetch_response.text =
            if (saveMockMode == MockConfig.OptionRecordMock.RECORD.ordinal) "Fetch Response from API"
            else "Fetch Response from Database"
        button_fetch_response.setOnClickListener {
            viewModel.fetchResponseMock()
        }
        button_export_database.setOnClickListener {
            MockInterceptor.exportDatabase()
        }
        button_import_database.setOnClickListener {
            MockInterceptor.importDatabase()
        }
        button_replace_database.setOnClickListener {
            MockInterceptor.replaceDatabase()
        }
        button_delete_database.setOnClickListener {
            MockInterceptor.deleteDatabase()
        }
    }

    private fun setupMocksFromMockFile(viewModel: SampleViewModel) {
        button_sample1.setOnClickListener {
            viewModel.fetchResponseMock()
        }
        button_sample1_1.setOnClickListener {
            viewModel.fetchResponseWithNoAdditional()
        }
        button_sample2.setOnClickListener {
            viewModel.fetchResponseNoMock()
        }
        button_sample3.setOnClickListener {
            viewModel.fetchResponseNoMockWithParams()
        }
        button_sample4.setOnClickListener {
            viewModel.fetchResponseMultiMock()
        }
        button_sample5.setOnClickListener {
            viewModel.fetchResponseNoMockNoFile()
        }
        button_sample6.setOnClickListener {
            viewModel.fetchResponseMultiMockNoFile()
        }
        button_sample7.setOnClickListener {
            viewModel.fetchResponsePaginated(input_page.text.toString())
        }
        button_sample8.setOnClickListener {
            MockNotification.showMockNotification(this)
        }
    }

    private fun addOnRadioButton(
        radioGroup: RadioGroup,
        text: String,
        tag: Int,
        saveMockMode: Int
    ) {
        val radioButton = RadioButton(this).apply {
            this.id = View.generateViewId()
            this.text = text
            this.tag = tag
        }
        radioGroup.addView(radioButton)
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

}