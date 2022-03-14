package com.gustafah.android.mockinterceptor.sample.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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
        val repository = SampleRepository(serviceClient(context = this))
        val viewModel = SampleViewModel(repository)
        button_sample1.setOnClickListener {
            viewModel.fetchResponseMock()
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
            MockNotification.showMockNotification(this)
        }
        viewModel.responseLiveData.observe(this) {
            it.forEach { data -> println(data) }
        }
    }

}