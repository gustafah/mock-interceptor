package com.gustafah.android.mockinterceptor

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.gustafah.android.mockinterceptor.MockConfig.OptionsSelectorMode
import com.gustafah.android.mockinterceptor.MockUtils.DEFAULT_MOCK_KEY
import com.gustafah.android.mockinterceptor.MockUtils.ERROR_JSON_NOT_FOUND
import com.gustafah.android.mockinterceptor.MockUtils.prefs
import okhttp3.Request
import retrofit2.Invocation
import java.io.InputStream
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
    fun fetchFileNameFromUrl(request: Request): String {
        val postfix = "$assetsSeparator${request.method}$assetsSuffix"
        val segments = request.url.pathSegments
        val fileName =
            request.tag(Invocation::class.java)?.method()?.getAnnotation(Mock::class.java)?.path
                ?: kotlin.run {
                    segments.filter {
                        requestArguments.contains(it).not()
                    }.joinToString(assetsSeparator, assetsPrefix, postfix)
                }
        requestArguments = getArguments(request)
        return getFileFromAssetManager(fileName)?.let {
            getContentFromInputStream(it)
        } ?: run {
            getContentFromFileName(segments.joinToString(assetsSeparator, assetsPrefix, postfix))
        }
    }

    private fun getArguments(request: Request): List<String> {
        val pathParams = request.tags()?.values?.first()?.let {
            val clazz = (it as retrofit2.Invocation)
            clazz.arguments().filterIsInstance<String>()
        } ?: emptyList<String>()

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
     * Gets the content of a Mock File from it's name
     * @param fileName - The name (path) of the Mock File
     * @return the content of the file, as String
     */
    fun getContentFromFileName(fileName: String) =
        getFileFromAssetManager(fileName)?.let { getContentFromInputStream(it) }
            ?: String.format(
                ERROR_JSON_NOT_FOUND,
                fileName,
                if (requestArguments.isNotEmpty()) {
                    requestArguments.joinToString(separator = "\", \"")
                } else {
                    ""
                }
            )

    /**
     * Uses the provided [context] to access the Assets Folder and retrieve a file content
     * @param fileName - The name (path) of the Mock File
     * @return the content of the file, as InputStream, for reading
     */
    private fun getFileFromAssetManager(fileName: String) = try {
        context().assets.open(fileName)
    } catch (ignored: Exception) {
        null
    }

    /**
     * Gets the actual content, as String, from a InputStream
     * @param inputStream, the InputStream of the file
     * @return Actual content of the InputStream, as String
     */
    private fun getContentFromInputStream(inputStream: InputStream) =
        inputStream.bufferedReader().use { it.readText() }

    /**
     * A Builder class to create a MockConfig
     */
    class Builder {
        var assetsPrefix: String = ""
            private set
        var assetsSuffix: String = ""
            private set
        var assetsSeparator: String = ""
            private set
        var context: (() -> Context)? = null
            private set
        var selectorMode: OptionsSelectorMode = OptionsSelectorMode.ALWAYS_ON_TOP
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
     */
    enum class OptionsSelectorMode {
        ALWAYS_ON_TOP,
        STANDARD,
        NO_SELECTION
    }
}