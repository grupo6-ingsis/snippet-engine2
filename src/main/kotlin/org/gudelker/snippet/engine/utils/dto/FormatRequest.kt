package org.gudelker.snippet.engine.utils.dto

import org.gudelker.snippet.engine.utils.FormatRuleNameWithValue

data class FormatRequest(
    val snippetId: String,
    val snippetVersion: String,
    val userRules: List<FormatRuleNameWithValue>,
    val allRules: List<String>,
    val requestedAt: Long = System.currentTimeMillis(),
)
