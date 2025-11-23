package org.gudelker.snippet.engine.utils.dto

import org.gudelker.snippet.engine.utils.FormatRuleNameWithValue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FormatRequestTest {
    @Test
    fun `FormatRequest DTO basic equality and properties`() {
        val req1 =
            FormatRequest(
                snippetId = "id1",
                snippetVersion = "1.0",
                userRules = listOf(FormatRuleNameWithValue("rule1", 2)),
                allRules = listOf("rule1", "rule2"),
                requestedAt = 123L,
            )
        val req2 =
            FormatRequest(
                snippetId = "id1",
                snippetVersion = "1.0",
                userRules = listOf(FormatRuleNameWithValue("rule1", 2)),
                allRules = listOf("rule1", "rule2"),
                requestedAt = 123L,
            )
        assertEquals(req1, req2)
        assertEquals("id1", req1.snippetId)
        assertEquals("1.0", req1.snippetVersion)
        assertEquals(listOf(FormatRuleNameWithValue("rule1", 2)), req1.userRules)
        assertEquals(listOf("rule1", "rule2"), req1.allRules)
        assertEquals(123L, req1.requestedAt)
    }
}
