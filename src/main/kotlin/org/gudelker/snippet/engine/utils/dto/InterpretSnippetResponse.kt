package org.gudelker.snippet.engine.utils.dto

import org.gudelker.snippet.engine.utils.ResultType

data class InterpretSnippetResponse (
    val results: ArrayList<String>,
    val resultType: ResultType,
)