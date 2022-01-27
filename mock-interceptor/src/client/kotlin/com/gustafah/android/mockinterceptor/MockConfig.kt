package com.gustafah.android.mockinterceptor

import android.content.Context
import android.preference.PreferenceManager
import com.gustafah.android.mockinterceptor.MockConfig.OptionsSelectorMode
import com.gustafah.android.mockinterceptor.MockUtils.ERROR_JSON_NOT_FOUND
import com.gustafah.android.mockinterceptor.MockUtils.prefs
import okhttp3.Request
import org.json.JSONObject
import retrofit2.Invocation
import java.security.InvalidParameterException

/**
 * Configuration for MockInterceptor
 * @param builder: Builder object to create the configuration
 *
 * This class provides information to MockInterceptor, such as:
 * @property context            to access the Assets folder, and to provide context do Dialogs
 * @property assetsPrefix       for assets file name formatting
 * @property assetsSeparator    for assets file name formatting
 * @property assetsSuffix       for assets file name formatting
 * @property selectorMode       to determine what strategy will be used for the Options Selector
 * Obs.: selectorMode may influence how your Activity behaves whenever a Options Dialog is displayed
 * @see OptionsSelectorMode
 *
 *@throws InvalidParameterException when no Context is provided
 */
class MockConfig private constructor(builder: Builder) {
    private val assetsPrefix: String
    private val assetsSuffix: String
    private val assetsSeparator: String
    val selectorMode: OptionsSelectorMode
    val context: () -> Context
    var requestArguments = emptyList<String>()
        private set

    /**
     * Get the information from the Builder
     */
    init {
        assetsPrefix = builder.assetsPrefix
        assetsSuffix = builder.assetsSuffix
        assetsSeparator = builder.assetsSeparator
        selectorMode = builder.selectorMode
        context =
            builder.context ?: throw (InvalidParameterException("No Context"))
        prefs = PreferenceManager.getDefaultSharedPreferences(context())
    }

    /**
     * For getting the content of a Mock file from a okhttp3.Request object
     * @param request from okhttp3.Request
     * @return [getContent]
     */
    fun fetchFileNameFromUrl(request: Request): JSONObject {
        val postfix = "$assetsSeparator${request.method}$assetsSuffix"
        val segments = request.url.pathSegments
        requestArguments = getArguments(request)
        val fileName = getFileFromMockAnnotation(request) ?: kotlin.run {
            arrayOf(segments.filter {
                requestArguments.contains(it).not()
            }.joinToString(assetsSeparator, assetsPrefix, postfix))
        }
        return JSONObject().apply {
            fileName.forEach {
                put(it, JSONObject(
                        MockParser.getContentFromAsset(context(), it) ?: String.format(
                            ERROR_JSON_NOT_FOUND,
                            fileName,
                            if (requestArguments.isNotEmpty()) {
                                requestArguments.joinToString(separator = "\", \"")
                            } else {
                                ""
                            }
                        )
                    )
                )
            }
        }
    }

    private fun getFileFromMockAnnotation(request: Request) =
        request.tag(Invocation::class.java)?.method()?.getAnnotation(Mock::class.java)?.let {
            when {
                it.path.isNotEmpty() -> arrayOf(it.path)
                it.files.isNotEmpty() -> it.files
                else -> arrayOf()
            }
        }

    private fun getArguments(request: Request): List<String> {
        val pathParams = request.tags()?.values?.first()?.let {
            val clazz = (it as Invocation)
            clazz.arguments().filterIsInstance<String>()
        } ?: emptyList()

        val params = arrayListOf<String>()
        for (i in 0 until request.url.querySize) {
            params.add(request.url.queryParameterValue(i) ?: "")
        }
        params.addAll(pathParams)
        return params
    }

    private fun Request.tags(): Map<*, *>? {
        val aClass = this::class.java
        val field = aClass.getDeclaredField("tags")
        field.isAccessible = true
        val tags = field.get(this) as? Map<*, *>?
        field.isAccessible = false
        return tags
    }

    /**
     * A Builder class to create a MockConfig
     */
    class Builder {
        internal var assetsPrefix: String = ""
            private set
        internal var assetsSuffix: String = ""
            private set
        internal var assetsSeparator: String = ""
            private set
        internal var context: (() -> Context)? = null
            private set
        internal var selectorMode: OptionsSelectorMode = OptionsSelectorMode.ALWAYS_ON_TOP
            private set

        fun prefix(prefix: String) = apply { this.assetsPrefix = prefix }
        fun suffix(suffix: String) = apply { this.assetsSuffix = suffix }
        fun separator(separator: String) = apply { this.assetsSeparator = separator }
        fun context(context: () -> Context) = apply { this.context = context }
        fun selectorMode(mode: OptionsSelectorMode) = apply { this.selectorMode = mode }
        fun build() = MockConfig(this)
    }

    /**
     * Determines the behaviour of Options Selector
     * OptionsSelectorMode.ALWAYS_ON_TOP WILL cause your onResume to be called
     * OptionsSelectorMode.STANDARD MAY cause the Options Dialog to display under
     * certain views, depending on how they are added to your view stash.
     * OptionsSelectorMode.NO_SELECTION won't display dialog
     */
    enum class OptionsSelectorMode {
        ALWAYS_ON_TOP,
        STANDARD,
        NO_SELECTION
    }
}
