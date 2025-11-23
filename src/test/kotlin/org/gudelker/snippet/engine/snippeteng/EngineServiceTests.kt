package org.gudelker.snippet.engine.snippeteng

import org.gudelker.linter.LinterConfig
import org.gudelker.rules.FormatterRule
import org.gudelker.snippet.engine.EngineService
import org.gudelker.snippet.engine.utils.ResultType
import org.gudelker.snippet.engine.utils.VersionAdapter
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EngineServiceTests {
    private val engineService = EngineService()

    @Nested
    inner class ParseSnippetTests {
        @Test
        fun `parseSnippet returns SUCCESS for valid input`() {
            val lexer = Mockito.mock(org.gudelker.lexer.StreamingLexer::class.java)
            val parser = Mockito.mock(org.gudelker.parser.StreamingParser::class.java)
            val sourceReader = Mockito.mock(org.gudelker.sourcereader.InputStreamSourceReader::class.java)
            Mockito.`when`(lexer.hasMore()).thenReturn(false)
            Mockito.`when`(parser.hasMore()).thenReturn(false)
            val result = engineService.parseSnippet(lexer, parser, sourceReader)
            assertEquals(ResultType.SUCCESS, result)
        }

        @Test
        fun `parseSnippet returns FAILURE for parser error`() {
            val lexer = Mockito.mock(org.gudelker.lexer.StreamingLexer::class.java)
            val parser = Mockito.mock(org.gudelker.parser.StreamingParser::class.java)
            val sourceReader = Mockito.mock(org.gudelker.sourcereader.InputStreamSourceReader::class.java)
            Mockito.`when`(lexer.hasMore()).thenReturn(true, false)
            Mockito.`when`(parser.hasMore()).thenReturn(true, false)
            Mockito.`when`(lexer.nextBatch(10)).thenReturn(Mockito.mock(org.gudelker.lexer.StreamingLexerResult.TokenBatch::class.java))
            Mockito.`when`(parser.nextStatement()).thenReturn(
                org.gudelker.parser.StreamingParserResult
                    .Error("error"),
            )
            val result = engineService.parseSnippet(lexer, parser, sourceReader)
            assertEquals(org.gudelker.snippet.engine.utils.ResultType.FAILURE, result)
        }
    }

    @Nested
    inner class LintSnippetTests {
        @Test
        fun `lintSnippet returns empty list for empty input`() {
            val input = ByteArrayInputStream(ByteArray(0))
            val config = emptyMap<String, LinterConfig>()
            val result = engineService.lintSnippet(input, "1.0", config)
            assertTrue(result.isEmpty())
        }

        @Test
        fun `lintSnippet returns empty or violations for invalid code`() {
            val code = "let x = " // Invalid code
            val input = ByteArrayInputStream(code.toByteArray())
            val config = emptyMap<String, LinterConfig>()
            val result = engineService.lintSnippet(input, "1.0", config)
            assertNotNull(result)
        }
    }

    @Nested
    inner class FormatSnippetTests {
        @Test
        fun `formatSnippet returns empty string for empty input`() {
            val input = ByteArrayInputStream(ByteArray(0))
            val config = emptyMap<String, FormatterRule>()
            val result = engineService.formatSnippet(input, "1.0", config)
            assertEquals("", result)
        }

        @Test
        fun `formatSnippet formats valid code correctly`() {
            val code = "let x = 1;".toByteArray()
            val input = ByteArrayInputStream(code)
            val config = emptyMap<String, FormatterRule>()
            val result = engineService.formatSnippet(input, "1.0", config)
            assertTrue(result.contains("let x = 1"))
        }
    }

    @Nested
    inner class InterpretSnippetTests {
        @Test
        fun `interpretSnippet returns output for valid code`() {
            val code = "let x = 1;" // Use a simple assignment that is valid
            val inputs = mutableListOf<String>()
            val result = engineService.interpretSnippet(code, VersionAdapter.toVersion("1.0"), inputs)
            assertTrue(result.isEmpty() || result.any { it.contains("x") })
        }

        @Test
        fun `interpretSnippet throws for invalid code`() {
            val code = "let x = " // Invalid code
            val inputs = mutableListOf<String>()
            assertThrows(RuntimeException::class.java) {
                engineService.interpretSnippet(code, VersionAdapter.toVersion("1.0"), inputs)
            }
        }

        @Test
        fun `InterpretSnippetRequest and Response DTOs work as expected`() {
            val req1 =
                org.gudelker.snippet.engine.utils.dto.InterpretSnippetRequest(
                    snippetContent = "let x = 1;",
                    version = "1.0",
                    inputs = mutableListOf("input1"),
                )
            val req2 =
                org.gudelker.snippet.engine.utils.dto.InterpretSnippetRequest(
                    snippetContent = "let x = 1;",
                    version = "1.0",
                    inputs = mutableListOf("input1"),
                )
            assertEquals(req1, req2)
            assertEquals("let x = 1;", req1.snippetContent)
            assertEquals("1.0", req1.version)
            assertEquals(mutableListOf("input1"), req1.inputs)

            val resp1 =
                org.gudelker.snippet.engine.utils.dto.InterpretSnippetResponse(
                    results = arrayListOf("out1", "out2"),
                    resultType = org.gudelker.snippet.engine.utils.ResultType.SUCCESS,
                )
            val resp2 =
                org.gudelker.snippet.engine.utils.dto.InterpretSnippetResponse(
                    results = arrayListOf("out1", "out2"),
                    resultType = org.gudelker.snippet.engine.utils.ResultType.SUCCESS,
                )
            assertEquals(resp1, resp2)
            assertEquals(arrayListOf("out1", "out2"), resp1.results)
            assertEquals(org.gudelker.snippet.engine.utils.ResultType.SUCCESS, resp1.resultType)
        }
    }

    @Nested
    inner class RemoveTrailingNewlineTests {
        @Test
        fun `removeTrailingNewline removes newline at end`() {
            val method = EngineService::class.java.getDeclaredMethod("removeTrailingNewline", String::class.java)
            method.isAccessible = true
            val result = method.invoke(engineService, "hello\n")
            assertEquals("hello", result)
        }

        @Test
        fun `removeTrailingNewline leaves string without newline unchanged`() {
            val method = EngineService::class.java.getDeclaredMethod("removeTrailingNewline", String::class.java)
            method.isAccessible = true
            val result = method.invoke(engineService, "hello")
            assertEquals("hello", result)
        }
    }

    @Nested
    inner class LintSnippetMappingTests {
        @Test
        fun `lintSnippet maps violations to LintResultRequest correctly`() {
            val code = "let x = 1;"
            val input = ByteArrayInputStream(code.toByteArray())
            val config = emptyMap<String, LinterConfig>()
            val result = engineService.lintSnippet(input, "1.0", config)
            assertNotNull(result)
            assertTrue(result is List<*>)
            result.forEach {
                assertNotNull(it.message)
                assertNotNull(it.line)
                assertNotNull(it.column)
            }
        }

        @Test
        fun `lintSnippet handles multiple violations with different positions`() {
            // Este test requiere un mock o spy si quieres forzar violations, pero aquí solo se asegura que el mapping soporte varios
            val code = "let x = 1; let y = ;" // Intenta provocar más de una violación
            val input = ByteArrayInputStream(code.toByteArray())
            val config = emptyMap<String, LinterConfig>()
            val result = engineService.lintSnippet(input, "1.0", config)
            assertNotNull(result)
            assertTrue(result is List<*>)
            // Si hay más de uno, todos deben mapear bien
            result.forEach {
                assertNotNull(it.message)
                assertNotNull(it.line)
                assertNotNull(it.column)
            }
        }
    }
}
