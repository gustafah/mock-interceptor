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
import com.gustafah.android.mockinterceptor.MockInterceptor
import com.gustafah.android.mockinterceptor.MockUtils.BUNDLE_FIELD_TITLE
import com.gustafah.android.mockinterceptor.R

class MockReferenceDialog : DialogFragment() {

    private lateinit var rootView: View
    private var selectedReference = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rootView = inflater.inflate(R.layout.mock_dialog_references, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            createDialog(
                it.getStringArray(BUNDLE_FIELD_TITLE) ?: emptyArray()
            )
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        MockInterceptor.release(selectedReference)
        (activity as DialogInterface.OnDismissListener).onDismiss(dialog)
    }

    private fun createDialog(title: Array<String>) {
        rootView.findViewById<TextView>(R.id.label_title).text = "Multi Mock"
        val listView = rootView.findViewById<ListView>(R.id.list_references)
        listView.adapter = SimpleAdapter(
            context,
            makeDataFromArrays(title),
            android.R.layout.simple_list_item_1,
            arrayOf("desc"),
            intArrayOf(android.R.id.text1)
        )
        listView.setOnItemClickListener { _, _, position: Int, _ ->
            selectedReference = position
            dismiss()
        }
        isCancelable = true
    }

    private fun makeDataFromArrays(
        textArray: Array<String>
    ): List<Map<String, String>> {
        val list = ArrayList<Map<String, String>>()
        for (i in textArray.indices) {
            list.add(HashMap<String, String>().apply {
                set("desc", textArray[i])
            })
        }
        return list
    }

    companion object {
        fun newInstance(
            title: Array<String>
        ) = MockReferenceDialog().apply {
            arguments = Bundle().apply {
                putStringArray(BUNDLE_FIELD_TITLE, title)
            }
        }
    }
}
