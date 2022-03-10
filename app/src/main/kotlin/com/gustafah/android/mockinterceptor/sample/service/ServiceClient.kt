package com.gustafah.android.mockinterceptor.sample.service

import android.content.Context
import com.gustafah.android.mockinterceptor.MockConfig
import com.gustafah.android.mockinterceptor.MockInterceptor
import com.gustafah.android.mockinterceptor.sample.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.jetbrains.annotations.TestOnly
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

fun serviceClient(context: Context): SampleApi {
    val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .addInterceptor(
            MockInterceptor.apply {
                config = MockConfig.Builder()
                    .suffix(".json") //optional
                    .separator("_") //optional
                    .context { context } //mandatory
                    .selectorMode(MockConfig.OptionsSelectorMode.STANDARD) //recommended
                    .build()
            }
        )
        .build()
    val service = Retrofit.Builder()
        .baseUrl("https://jsonplaceholder.typicode.com")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    return service.create(SampleApi::class.java)
}