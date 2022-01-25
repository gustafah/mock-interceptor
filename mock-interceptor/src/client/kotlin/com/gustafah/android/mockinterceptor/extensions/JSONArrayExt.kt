package com.gustafah.android.mockinterceptor.extensions

import com.gustafah.android.mockinterceptor.MockUtils
import com.gustafah.android.mockinterceptor.MockUtils.JSON_FIELD_DATA
import com.gustafah.android.mockinterceptor.MockUtils.JSON_FIELD_FILTER
import org.json.JSONArray

fun JSONArray.isEmpty() = this.length() == 0

fun JSONArray.isNotEmpty() = isEmpty().not()

fun JSONArray.first() = getJSONObject(0)

fun JSONArray.arrayWithFilterAndArgs(args: List<String>): JSONArray {
    return (0 until length()).asSequence().map { getJSONObject(it) }.filter {
        it.has(JSON_FIELD_FILTER) && it.getJSONObject(JSON_FIELD_FILTER)
            .getJSONArray(JSON_FIELD_DATA).hasAll(args)
    }.run {
        JSONArray().apply {
            iterator().forEach { jsonObj ->
                this.put(jsonObj)
            }
        }
    }
}

fun JSONArray.hasAny(args: List<String>): Boolean =
    (0 until length()).asSequence().map { getString(it) }.any {
        args.contains(it)
    }

fun JSONArray.hasAll(args: List<String>): Boolean =
    (0 until length()).asSequence().map { getString(it) }.all {
        args.contains(it)
    }



fun JSONArray.mapData(): Pair<Array<String>, Array<String>> {
    val text = ArrayList<String>()
    val subtext = ArrayList<String>()
    for (i in 0 until length()) {
        val jsonObject = getJSONObject(i)
        text.add(jsonObject.getString(MockUtils.JSON_FIELD_DESCRIPTION))
        subtext.add(jsonObject.getString(MockUtils.JSON_FIELD_CODE))
    }
    val textArray = text.toTypedArray()
    val subtextArray = subtext.toTypedArray()
    return Pair(textArray, subtextArray)
}