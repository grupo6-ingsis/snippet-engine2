package org.gudelker.snippet.engine.utils.dto

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DtoTests {
    @Test
    fun `LintRequest serializes and equals`() {
        val req1 = LintRequest("id1", "1.0", emptyList(), listOf("rule1"), 123L)
        val req2 = LintRequest("id1", "1.0", emptyList(), listOf("rule1"), 123L)
        assertEquals(req1, req2)
        assertEquals(req1.hashCode(), req2.hashCode())
        assertEquals("id1", req1.snippetId)
        assertEquals("1.0", req1.snippetVersion)
        assertEquals(listOf("rule1"), req1.allRules)
        assertEquals(123L, req1.requestedAt)
    }

    @Test
    fun `LintResultRequest basic properties`() {
        val res = LintResultRequest("msg", 2, 3)
        assertEquals("msg", res.message)
        assertEquals(2, res.line)
        assertEquals(3, res.column)
    }

    @Test
    fun `SnippetIdWithLintResultsDto equality`() {
        val l1 = LintResultRequest("a", 1, 1)
        val l2 = LintResultRequest("b", 2, 2)
        val dto1 = SnippetIdWithLintResultsDto("id", listOf(l1, l2))
        val dto2 = SnippetIdWithLintResultsDto("id", listOf(l1, l2))
        assertEquals(dto1, dto2)
        assertEquals(dto1.hashCode(), dto2.hashCode())
        assertEquals("id", dto1.snippetId)
        assertEquals(listOf(l1, l2), dto1.results)
    }
}
