package com.gustafah.android.mock

internal const val BUNDLE_FIELD_TITLE = "saved_data_title"
internal const val BUNDLE_FIELD_TEXT = "saved_data_text"
internal const val BUNDLE_FIELD_SUBTEXT = "saved_data_subtext"

internal const val JSON_FIELD_REFERENCE = "reference"
internal const val JSON_FIELD_SAVED_DATA = "saved_data"
internal const val JSON_FIELD_DEFAULT = "default"
internal const val JSON_FIELD_DESCRIPTION = "description"
internal const val JSON_FIELD_CODE = "code"
internal const val JSON_FIELD_FILTER = "filter"
internal const val JSON_FIELD_DATA = "data"
internal const val JSON_FIELD_DATA_ARRAY = "data_array"
internal const val JSON_FIELD_DATA_PATH = "data_path"
internal const val JSON_FIELD_IS_UNIT = "is_unit"

internal const val PATTERN_DATE_TIME_GMT = "EEE, dd MMM yyyy HH:mm:ss 'GMT'"
internal const val AUTO_MOCK = false

internal const val ERROR_JSON_NOT_FOUND =
    "{\"type\": \"SERVICE_UNAVAILABLE\",\"message\": \"Couldn't find a mock for this request. " +
            "(suggestion: %s\", \"filter\": {\"data\": [\"%s\"]}})"
internal const val ERROR_JSON_NO_DATA = "{\"type\": \"UNKNOWN_ERROR\", \"message\": \"No mocked data.\"}"
