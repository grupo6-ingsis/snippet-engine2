package org.gudelker.snippet.engine.input

data class ParseSnippetRequest(
    val snippetContent: String,
    val version: String,
)
