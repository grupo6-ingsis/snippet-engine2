package org.gudelker.snippet.engine.utils.dto

data class InterpretSnippetRequest(
    val snippetContent: String,
    val version: String,
)
