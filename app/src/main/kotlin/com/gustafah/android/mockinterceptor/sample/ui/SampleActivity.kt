package com.gustafah.android.mockinterceptor.sample.ui

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.isVisible
import com.gustafah.android.mockinterceptor.MockConfig
import com.gustafah.android.mockinterceptor.MockInterceptor
import com.gustafah.android.mockinterceptor.notification.MockNotification
import com.gustafah.android.mockinterceptor.sample.R
import com.gustafah.android.mockinterceptor.sample.repository.SampleRepository
import com.gustafah.android.mockinterceptor.sample.service.serviceClient
import com.gustafah.android.mockinterceptor.sample.ui.viewmodel.SampleViewModel


class SampleActivity : AppCompatActivity(R.layout.activity_sample) {

    private val radioGroup1: RadioGroup by lazy { findViewById(R.id.radio_group) }
    private val buttonSaveDb: Button by lazy { findViewById(R.id.button_save_db) }
    private val linearDatabase: LinearLayout by lazy { findViewById(R.id.linear_database) }
    private val radioButtonRecordMode: RadioGroup by lazy { findViewById(R.id.radio_button_record_mode) }
    private val buttonRecordModeSave: Button by lazy { findViewById(R.id.button_record_mode_save) }
    private val buttonSetIdentification: Button by lazy { findViewById(R.id.button_set_identification) }
    private val layoutSetIdentification: LinearLayoutCompat by lazy { findViewById(R.id.layout_set_identification) }
    private val edittextSetIdentification: EditText by lazy { findViewById(R.id.edittext_set_identification) }
    private val buttonSaveIdentification: Button by lazy { findViewById(R.id.button_save_identification) }
    private val buttonFetchIdentifiers: Button by lazy { findViewById(R.id.button_fetch_identifiers) }
    private val buttonFetchResponse: Button by lazy { findViewById(R.id.button_fetch_response) }
    private val buttonExportDatabase: Button by lazy { findViewById(R.id.button_export_database) }
    private val buttonImportDatabase: Button by lazy { findViewById(R.id.button_import_database) }
    private val buttonReplaceDatabase: Button by lazy { findViewById(R.id.button_replace_database) }
    private val buttonDeleteDatabase: Button by lazy { findViewById(R.id.button_delete_database) }
    private val linearMockFile: LinearLayout by lazy { findViewById(R.id.linear_mock_file) }
    private val buttonSample1: Button by lazy { findViewById(R.id.button_sample1) }
    private val buttonSample11: Button by lazy { findViewById(R.id.button_sample1_1) }
    private val buttonSample2: Button by lazy { findViewById(R.id.button_sample2) }
    private val buttonSample3: Button by lazy { findViewById(R.id.button_sample3) }
    private val buttonSample4: Button by lazy { findViewById(R.id.button_sample4) }
    private val buttonSample5: Button by lazy { findViewById(R.id.button_sample5) }
    private val buttonSample6: Button by lazy { findViewById(R.id.button_sample6) }
    private val inputPage: EditText by lazy { findViewById(R.id.input_page) }
    private val buttonSample7: Button by lazy { findViewById(R.id.button_sample7) }
    private val buttonSample8: Button by lazy { findViewById(R.id.button_sample8) }
    private val textResponse: TextView by lazy { findViewById(R.id.text_response) }

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

        linearDatabase.isVisible = isOnSaveMockMode
        linearMockFile.isVisible = !isOnSaveMockMode
        radioButtonRecordMode.isVisible =
            saveMockMode == MockConfig.OptionRecordMock.RECORD.ordinal

        setupMocksFromDatabase(viewModel, saveMockMode)
        setupMocksFromMockFile(viewModel)

        MockInterceptor.databaseObserver.observe(this) {
            Toast.makeText(this, it.name, Toast.LENGTH_SHORT).show()
        }
        viewModel.responseLiveData.observe(this) {
            cleanData()
            it.forEach { data ->
                printData(data.toJson())
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
            radioGroup1,
            "Response from Mock File",
            MockConfig.OptionRecordMock.DISABLED.ordinal,
            saveMockMode
        )
        addOnRadioButton(
            radioGroup1,
            "Record API response and save on Database",
            MockConfig.OptionRecordMock.RECORD.ordinal,
            saveMockMode
        )
        addOnRadioButton(
            radioGroup1,
            "Playback API response from Database",
            MockConfig.OptionRecordMock.PLAYBACK.ordinal,
            saveMockMode
        )

        buttonSaveDb.setOnClickListener {
            val checkedView = radioGroup1.findViewById<View>(radioGroup1.checkedRadioButtonId)
            with(getPreferences(Context.MODE_PRIVATE).edit()) {
                putInt("SAVE_MOCK_MODE", checkedView.tag.toString().toInt())
                apply()
            }
            triggerRebirth()
        }
    }

    private fun setupRecordRadioButton(replaceMockMode: Int) {
        addOnRadioButton(
            radioButtonRecordMode,
            "Default",
            MockConfig.ReplaceMockOption.DEFAULT.ordinal,
            replaceMockMode
        )
        addOnRadioButton(
            radioButtonRecordMode,
            "Keep Mock",
            MockConfig.ReplaceMockOption.KEEP_MOCK.ordinal,
            replaceMockMode
        )
        addOnRadioButton(
            radioButtonRecordMode,
            "Replace Mock",
            MockConfig.ReplaceMockOption.REPLACE_MOCK.ordinal,
            replaceMockMode
        )
        buttonRecordModeSave.setOnClickListener {
            val checkedView =
                radioButtonRecordMode.findViewById<View>(radioButtonRecordMode.checkedRadioButtonId)
            with(getPreferences(Context.MODE_PRIVATE).edit()) {
                putInt("REPLACE_MOCK_MODE", checkedView.tag.toString().toInt())
                apply()
            }
            triggerRebirth()
        }
    }

    private fun setupMocksFromDatabase(viewModel: SampleViewModel, saveMockMode: Int) {
        buttonFetchResponse.text =
            if (saveMockMode == MockConfig.OptionRecordMock.RECORD.ordinal) "Fetch Response from API"
            else "Fetch Response from Database"
        buttonSetIdentification.setOnClickListener {
            layoutSetIdentification.isVisible = true
        }
        buttonSaveIdentification.setOnClickListener {
            layoutSetIdentification.isVisible = false
            MockInterceptor.setMockGroupIdentifier(edittextSetIdentification.text.toString())
        }
        buttonFetchIdentifiers.setOnClickListener {
            cleanData()
            MockInterceptor.getAllMockIdentifiers().forEach { data ->
                printData(data)
            }
        }
        buttonFetchResponse.setOnClickListener {
            viewModel.fetchResponseMock()
        }
        buttonExportDatabase.setOnClickListener {
            MockInterceptor.exportDatabase()
        }
        buttonImportDatabase.setOnClickListener {
            MockInterceptor.importDatabase()
        }
        buttonReplaceDatabase.setOnClickListener {
            MockInterceptor.replaceDatabase()
        }
        buttonDeleteDatabase.setOnClickListener {
            MockInterceptor.deleteDatabase()
        }
    }

    private fun setupMocksFromMockFile(viewModel: SampleViewModel) {
        buttonSample1.setOnClickListener {
            viewModel.fetchResponseMock()
        }
        buttonSample11.setOnClickListener {
            viewModel.fetchResponseWithNoAdditional()
        }
        buttonSample2.setOnClickListener {
            viewModel.fetchResponseNoMock()
        }
        buttonSample3.setOnClickListener {
            viewModel.fetchResponseNoMockWithParams()
        }
        buttonSample4.setOnClickListener {
            viewModel.fetchResponseMultiMock()
        }
        buttonSample5.setOnClickListener {
            viewModel.fetchResponseNoMockNoFile()
        }
        buttonSample6.setOnClickListener {
            viewModel.fetchResponseMultiMockNoFile()
        }
        buttonSample7.setOnClickListener {
            viewModel.fetchResponsePaginated(inputPage.text.toString())
        }
        buttonSample8.setOnClickListener {
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

    private fun cleanData() {
        textResponse.text = ""
    }

    private fun printData(data: String) {
        println(data)
        textResponse.append(data + "\n")
    }

}