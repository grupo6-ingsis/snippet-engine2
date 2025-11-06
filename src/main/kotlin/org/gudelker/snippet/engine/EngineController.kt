package org.gudelker.snippet.engine

import org.gudelker.snippet.engine.dto.ParseSnippetRequest
import org.gudelker.snippet.engine.utils.ResultType
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
        @RequestBody request: ParseSnippetRequest,
    ): ResultType {
        val parser = service.createParser(request.version)
        val lexer = service.createLexer(request.version)
        val srcReader = service.createStringInputSourceReader(request.snippetContent)
        return service.parseSnippet(lexer, parser, srcReader)
    }
}
