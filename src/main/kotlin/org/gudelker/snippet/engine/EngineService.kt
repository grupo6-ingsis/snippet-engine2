package org.gudelker.snippet.engine

import org.gudelker.lexer.LexerFactory
import org.gudelker.lexer.StreamingLexer
import org.gudelker.parser.DefaultParserFactory
import org.gudelker.parser.StreamingParser
import org.gudelker.sourcereader.StringSourceReader
import org.gudelker.utilities.Version
import org.springframework.stereotype.Service

@Service
class EngineService {
    fun createParser(version: Version): StreamingParser {
        val parserFactory = DefaultParserFactory.createParser(version)
        return StreamingParser(parserFactory)
    }

    fun createLexer(version: Version): StreamingLexer {
        val defaultLexer = LexerFactory.createLexer(version)
        return StreamingLexer(defaultLexer)
    }

    fun parseSnippet(lexer: StreamingLexer, parser: StreamingParser, snippetContent: String): ResultType {
        var result: ResultType
        val sourceReader = StringSourceReader(snippetContent)
        TODO("Creo que hay que recrear la logica del StreamingPipeline pero sin el interpreter")
    }
}

enum class ResultType {
    SUCCESS,
    FAILURE
}