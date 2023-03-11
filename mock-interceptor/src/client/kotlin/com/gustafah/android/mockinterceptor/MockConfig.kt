package com.gustafah.android.mockinterceptor

import android.content.Context
import android.preference.PreferenceManager
import android.util.Range
import com.gustafah.android.mockinterceptor.MockConfig.OptionsSelectorMode
import com.gustafah.android.mockinterceptor.MockUtils.prefs
import com.gustafah.android.mockinterceptor.processors.AnnotationFileProcessor
import com.gustafah.android.mockinterceptor.processors.FileProcessor
import com.gustafah.android.mockinterceptor.processors.UrlFileProcessor
import com.gustafah.android.mockinterceptor.processors.UrlFilteredFileProcessor
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
    internal val assetsPrefix: String
    internal val assetsSuffix: String
    internal val assetsSeparator: String
    internal var requestArguments = emptyList<String>()
        private set
    val delay: Range<Int>
    val additionalMockFiles: List<String>?
    val saveMockMode: OptionRecordMock
    val replaceMockOption: ReplaceMockOption
    val selectorMode: OptionsSelectorMode
    val context: () -> Context

    private val processors: List<FileProcessor>

    /**
     * Get the information from the Builder
     */
    init {
        assetsPrefix = builder.assetsPrefix
        assetsSuffix = builder.assetsSuffix
        assetsSeparator = builder.assetsSeparator
        selectorMode = builder.selectorMode
        saveMockMode = builder.saveMockMode
        replaceMockOption = builder.replaceMockOption
        delay = builder.delay ?: Range(0, 1)
        additionalMockFiles = builder.additionalMock
        context =
            builder.context ?: throw (InvalidParameterException("No Context"))
        prefs = PreferenceManager.getDefaultSharedPreferences(context())

        processors = listOf(
            AnnotationFileProcessor(),
            UrlFileProcessor(),
            UrlFilteredFileProcessor()
        )
    }

    fun fetchMockContentFromRequest(request: Request): JSONObject? {
        requestArguments = getArguments(request)
        processors.forEach {
            val content = it.process(this, request)
            if (content != null)
                return content
        }
        return null
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
        internal var saveMockMode: OptionRecordMock = OptionRecordMock.DISABLED
            private set
        internal var replaceMockOption: ReplaceMockOption = ReplaceMockOption.DEFAULT
            private set
        internal var context: (() -> Context)? = null
            private set
        internal var selectorMode: OptionsSelectorMode = OptionsSelectorMode.ALWAYS_ON_TOP
            private set
        internal var delay: Range<Int>? = null
            private set
        internal var additionalMock: List<String>? = null
            private set

        fun prefix(prefix: String) = apply { this.assetsPrefix = prefix }
        fun suffix(suffix: String) = apply { this.assetsSuffix = suffix }
        fun separator(separator: String) = apply { this.assetsSeparator = separator }
        fun context(context: () -> Context) = apply { this.context = context }
        fun selectorMode(mode: OptionsSelectorMode) = apply { this.selectorMode = mode }
        fun saveMockMode(saveMock: OptionRecordMock) = apply { this.saveMockMode = saveMock }
        fun replaceMockOption(replaceMockOption: ReplaceMockOption) = apply { this.replaceMockOption = replaceMockOption }
        fun setDelay(minDelay: Int, maxDelay: Int) = setDelay(Range(minDelay, maxDelay))
        fun setDelay(delay: Range<Int>) = apply { this.delay = delay }
        fun additionalMocks(additionalMockFiles: List<String>) =
            apply { this.additionalMock = additionalMockFiles }

        fun build() = MockConfig(this)
    }

    /**
     * Determines the behaviour of Options Selector
     * OptionsSelectorMode.ALWAYS_ON_TOP Displays DIALOG inside an empty Activity
     *     (Will trigger onResume)
     * OptionsSelectorMode.STANDARD Displays DIALOG on the same Activity
     *     (MAY cause the Options Dialog to display under certain views, depending on how
     *     they are added to your view stash).
     * OptionsSelectorMode.NO_SELECTION won't display dialog
     */
    enum class OptionsSelectorMode {
        ALWAYS_ON_TOP,
        STANDARD,
        NO_SELECTION
    }

    /**
     * Determines if it should store the api response or return the mock from asset or database
     * OptionRecordMock.DISABLED will return the mock file from asset
     * OptionRecordMock.RECORD will call the API and save its response in the database
     * OptionRecordMock.PLAYBACK will return the mock file from database
     */
    enum class OptionRecordMock {
        DISABLED,
        RECORD,
        PLAYBACK
    }

    enum class ReplaceMockOption {
        DEFAULT,
        KEEP_MOCK,
        REPLACE_MOCK
    }
}
