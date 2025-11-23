package org.gudelker.snippet.engine.snippeteng

import org.gudelker.rules.FormatterRule
import org.gudelker.snippet.engine.EngineService
import org.gudelker.snippet.engine.createFormatJsonRuleMap
import org.gudelker.snippet.engine.createJsonRuleMap
import org.gudelker.snippet.engine.createLexer
import org.gudelker.snippet.engine.createParser
import org.gudelker.snippet.engine.createStringInputSourceReader
import org.gudelker.snippet.engine.utils.FormatRuleNameWithValue
import org.gudelker.snippet.engine.utils.RuleNameWithValue
import org.gudelker.utilities.Version
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HelpersTests {
    @Test
    fun `createParser returns StreamingParser`() {
        val parser = createParser(Version.V1)
        assertNotNull(parser)
    }

    @Test
    fun `createLexer returns StreamingLexer`() {
        val lexer = createLexer(Version.V1)
        assertNotNull(lexer)
    }

    @Test
    fun `createStringInputSourceReader returns InputStreamSourceReader`() {
        val reader = createStringInputSourceReader("let x = 1;")
        assertNotNull(reader)
    }

    @Test
    fun `createJsonRuleMap enables user rules and disables others`() {
        val userRules = listOf(RuleNameWithValue("rule1", "val1"))
        val defaultRules = listOf("rule1", "rule2")
        val map = createJsonRuleMap(userRules, defaultRules)
        assertTrue(map["rule1"]!!.restrictPrintlnToIdentifierOrLiteral)
        assertFalse(map["rule2"]!!.restrictPrintlnToIdentifierOrLiteral)
        assertEquals("val1", map["rule1"]!!.identifierFormat)
    }

    @Test
    fun `createFormatJsonRuleMap enables user rules and disables others`() {
        val userRules = listOf(FormatRuleNameWithValue("rule1", 5))
        val defaultRules = listOf("rule1", "rule2")
        val map = createFormatJsonRuleMap(userRules, defaultRules)
        assertTrue(map["rule1"]!!.on)
        assertEquals(5, map["rule1"]!!.quantity)
        assertFalse(map["rule2"]!!.on)
        assertEquals(0, map["rule2"]!!.quantity)
    }

    @Test
    fun `EngineService parseSnippet handles need more tokens error and returns SUCCESS`() {
        val engineService = EngineService()
        val lexer = org.mockito.Mockito.mock(org.gudelker.lexer.StreamingLexer::class.java)
        val parser = org.mockito.Mockito.mock(org.gudelker.parser.StreamingParser::class.java)
        val sourceReader = org.mockito.Mockito.mock(org.gudelker.sourcereader.InputStreamSourceReader::class.java)
        org.mockito.Mockito
            .`when`(lexer.hasMore())
            .thenReturn(true, false)
        org.mockito.Mockito
            .`when`(parser.hasMore())
            .thenReturn(true, false)
        org.mockito.Mockito
            .`when`(
                lexer.nextBatch(10),
            ).thenReturn(org.mockito.Mockito.mock(org.gudelker.lexer.StreamingLexerResult.TokenBatch::class.java))
        org.mockito.Mockito
            .`when`(
                parser.nextStatement(),
            ).thenReturn(
                org.gudelker.parser.StreamingParserResult
                    .Error("need more tokens"),
                org.gudelker.parser.StreamingParserResult.Finished,
            )
        val result = engineService.parseSnippet(lexer, parser, sourceReader)
        assertEquals(org.gudelker.snippet.engine.utils.ResultType.SUCCESS, result)
    }

    @Test
    fun `EngineService formatSnippet handles error and returns partial result`() {
        val engineService = EngineService()
        val code = "let x = " // Invalid code
        val input = ByteArrayInputStream(code.toByteArray())
        val config = emptyMap<String, FormatterRule>()
        val result = engineService.formatSnippet(input, "1.0", config)
        assertTrue(result is String)
    }
}
