package org.gudelker.snippet.engine.input

import org.gudelker.utilities.Version

data class ParseSnippetRequest(
    val snippetContent: String,
    val version: Version,
)
