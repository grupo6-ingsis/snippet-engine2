package org.gudelker.snippet.engine.utils.dto

data class LintResultRequest(
    val message: String,
    val line: Number,
    val column: Number,
)
