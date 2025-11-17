package org.gudelker.snippet.engine.utils

data class LintRequest(
    val snippetId: String,
    val snippetVersion: String,
    val userRules: List<RuleNameWithValue>,
    val allRules: List<String>,
    val requestedAt: Long = System.currentTimeMillis()
)
