package org.gudelker.snippet.engine

import org.gudelker.snippet.engine.utils.ResultType
import org.gudelker.utilities.Version
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/snippet")
class EngineController (private val service: EngineService) {

    @PostMapping("/parse")
    fun parseSnippet(snippetContent: String, version: Version): ResultType {
        val parser = service.createParser(version)
        val lexer = service.createLexer(version)
        val srcReader = service.createStringInputSourceReader(snippetContent)
        return service.parseSnippet(lexer,parser, srcReader)
    }
}