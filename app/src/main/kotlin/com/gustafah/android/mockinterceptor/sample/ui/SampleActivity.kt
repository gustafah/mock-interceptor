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

}