package com.gustafah.android.mockinterceptor.ui

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.gustafah.android.mockinterceptor.*
import com.gustafah.android.mockinterceptor.BUNDLE_FIELD_SUBTEXT
import com.gustafah.android.mockinterceptor.BUNDLE_FIELD_TEXT
import com.gustafah.android.mockinterceptor.BUNDLE_FIELD_TITLE

class MockOptionsDialog : DialogFragment() {

    private lateinit var rootView: View
    private var selectedMock = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.mock_dialog_options, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            createDialog(
                it.getString(BUNDLE_FIELD_TITLE, ""),
                it.getStringArray(BUNDLE_FIELD_TEXT) ?: emptyArray(),
                it.getStringArray(BUNDLE_FIELD_SUBTEXT) ?: emptyArray()
            )
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        MockInterceptor.release(selectedMock)
        (activity as DialogInterface.OnDismissListener).onDismiss(dialog)
    }

    private fun createDialog(title: String, text: Array<String>, subtext: Array<String>) {
        rootView.findViewById<TextView>(R.id.label_title).text = title
        val listView = rootView.findViewById<ListView>(R.id.list_options)
        listView.adapter = SimpleAdapter(
            context,
            makeDataFromArrays(text, subtext),
            android.R.layout.simple_list_item_2,
            arrayOf("desc", "code"),
            intArrayOf(android.R.id.text1, android.R.id.text2)
        )
        listView.setOnItemClickListener { _, _, position: Int, _ ->
            selectedMock = position
            dismiss()
        }
        isCancelable = true
    }

    private fun makeDataFromArrays(
        textArray: Array<String>,
        subtextArray: Array<String>
    ): List<Map<String, String>> {
        val list = ArrayList<Map<String, String>>()
        for (i in textArray.indices) {
            list.add(HashMap<String, String>().apply {
                set("desc", textArray[i])
                set("code", subtextArray[i])
            })
        }
        return list
    }

    companion object {
        fun newInstance(
            title: String,
            text: Array<String>,
            subtext: Array<String>
        ) =
            MockOptionsDialog().apply {
                arguments = Bundle().apply {
                    putString(BUNDLE_FIELD_TITLE, title)
                    putStringArray(BUNDLE_FIELD_TEXT, text)
                    putStringArray(BUNDLE_FIELD_SUBTEXT, subtext)
                }
            }
    }
}
