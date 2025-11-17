package org.gudelker.snippet.engine.utils.dto

data class SnippetIdWithLintResultsDto(
    val snippetId: String,
    val results: List<LintResultRequest>,
)
