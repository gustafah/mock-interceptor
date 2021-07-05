package com.gustafah.android.mockinterceptor.sample.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.gustafah.android.mockinterceptor.sample.R
import com.gustafah.android.mockinterceptor.sample.data.service
import com.gustafah.android.mockinterceptor.sample.data.serviceClient
import com.gustafah.android.mockinterceptor.sample.model.Response
import com.gustafah.android.mockinterceptor.sample.repository.SampleRepository
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
            viewModel.fetchResponse().observe(this) { response ->
                when (response) {
                    is Response.Success -> {
                        response.data?.let { list ->
                            list.forEach { data -> println(data) }
                        }
                    }
                    is Response.Error -> {
                        response.exception.printStackTrace()
                    }
                }
            }
        }
    }

}