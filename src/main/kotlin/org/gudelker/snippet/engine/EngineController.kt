package org.gudelker.snippet.engine

import org.gudelker.snippet.engine.input.ParseSnippetRequest
import org.gudelker.snippet.engine.utils.ResultType
import org.gudelker.snippet.engine.utils.dto.InterpretSnippetRequest
import org.gudelker.snippet.engine.utils.dto.InterpretSnippetResponse
import org.gudelker.utilities.Version
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/snippet")
class EngineController(
    private val service: EngineService,
) {
    private val logger = LoggerFactory.getLogger(EngineController::class.java)

    @PostMapping("/parse")
    fun parseSnippet(
        @RequestBody input: ParseSnippetRequest,
    ): ResultType =
        try {
            logger.info("Parsing snippet with version: {}", input.version)
            val version =
                when (input.version) {
                    "1.0" -> Version.V1
                    "1.1" -> Version.V2
                    else -> throw IllegalArgumentException("Unsupported version: ${input.version}")
                }
            val parser = createParser(version)
            val lexer = createLexer(version)
            val srcReader = createStringInputSourceReader(input.snippetContent)
            val result = service.parseSnippet(lexer, parser, srcReader)
            logger.info("Snippet parsed successfully with result: {}", result)
            result
        } catch (e: IllegalArgumentException) {
            logger.error("Invalid version: {}. Error: {}", input.version, e.message)
            ResultType.FAILURE
        } catch (e: Exception) {
            logger.error("Error parsing snippet", e)
            ResultType.FAILURE
        }

    @PostMapping("/interpret")
    fun interpretSnippet(
        @RequestBody input: InterpretSnippetRequest,
    ): InterpretSnippetResponse {
        try {
            logger.info("Interpreting snippet with version: {}", input.version)
            val version =
                when (input.version) {
                    "1.0" -> Version.V1
                    "1.1" -> Version.V2
                    else -> throw IllegalArgumentException("Unsupported version: ${input.version}")
                }
            val results = service.interpretSnippet(input.snippetContent, version, input.inputs)
            logger.info("Snippet interpreted successfully. Results count: {}", results.size)
            return InterpretSnippetResponse(
                results = results,
                resultType = ResultType.SUCCESS,
            )
        } catch (e: Exception) {
            logger.error("Error interpreting snippet", e)
            return InterpretSnippetResponse(
                results = arrayListOf(),
                resultType = ResultType.FAILURE,
            )
        }
    }
}
