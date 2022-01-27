package com.gustafah.android.mockinterceptor

import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.gustafah.android.mockinterceptor.sample.model.FetchResponse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Type

class SampleActivityTest {

    @Test
    fun mockPosts_withDefaultValue() {
        val mock = MockParser.fromAssetFile(getInstrumentation().context, "posts.json")
        val listType: Type = object : TypeToken<ArrayList<FetchResponse?>?>() {}.type
        val response = Gson().fromJson<List<FetchResponse>>(mock, listType)
        assertTrue(response.size == 1)
        assertTrue(response[0].id == 1)
    }

    @Test
    fun mockPosts_withNonDefaultValue() {
        val mock = MockParser.fromAssetFile(getInstrumentation().context, "posts.json", 1)
        val listType: Type = object : TypeToken<ArrayList<FetchResponse?>?>() {}.type
        val response = Gson().fromJson<List<FetchResponse>>(mock, listType)
        assertTrue(response.size == 2)
        assertTrue(response[0].id == 1)
        assertTrue(response[1].id == 2)
    }

}