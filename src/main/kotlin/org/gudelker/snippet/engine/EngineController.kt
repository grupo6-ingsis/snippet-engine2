package org.gudelker.snippet.engine

import org.gudelker.snippet.engine.input.ParseSnippetRequest
import org.gudelker.snippet.engine.utils.ResultType
import org.gudelker.utilities.Version
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/snippet")
class EngineController(
    private val service: EngineService,
) {
    @PostMapping("/parse")
    fun parseSnippet(
        @RequestBody input: ParseSnippetRequest,
    ): ResultType {
        return try {
            val version = when (input.version) {
                "1.0" -> Version.V1
                "1.1" -> Version.V2
                else -> throw IllegalArgumentException("Unsupported version: ${input.version}")
            }
            val parser = createParser(version)
            val lexer = createLexer(version)
            val srcReader = createStringInputSourceReader(input.snippetContent)
            service.parseSnippet(lexer, parser, srcReader)
        } catch (e: IllegalArgumentException) {
            println("Invalid version: ${input.version}. Error: ${e.message}")
            ResultType.FAILURE
        } catch (e: Exception) {
            println("Error parsing snippet: ${e.message}")
            e.printStackTrace()
            ResultType.FAILURE
        }
    }
}
