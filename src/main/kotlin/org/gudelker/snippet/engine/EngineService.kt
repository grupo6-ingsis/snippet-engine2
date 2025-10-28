package org.gudelker.snippet.engine

import org.gudelker.lexer.LexerFactory
import org.gudelker.lexer.StreamingLexer
import org.gudelker.lexer.StreamingLexerResult.TokenBatch
import org.gudelker.parser.DefaultParserFactory
import org.gudelker.parser.StreamingParser
import org.gudelker.parser.StreamingParserResult
import org.gudelker.parser.StreamingParserResult.StatementParsed
import org.gudelker.snippet.engine.utils.ResultType
import org.gudelker.sourcereader.InputStreamSourceReader
import org.gudelker.statements.interfaces.Statement
import org.gudelker.utilities.Version
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.util.*


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

    fun createStringInputSourceReader(snippetContent: String): InputStreamSourceReader {
        val src = ByteArrayInputStream(snippetContent.toByteArray())
        return InputStreamSourceReader(src, 8192)
    }

    fun parseSnippet(lexer: StreamingLexer, parser: StreamingParser, sourceReader: InputStreamSourceReader): ResultType {

        lexer.initialize(sourceReader)

        val statements: MutableList<Statement?> = ArrayList<Statement?>()
        while (lexer.hasMore() || parser.hasMore()) {
            if (lexer.hasMore()) {
                val lexerResult = lexer.nextBatch(10)
                if (lexerResult is TokenBatch) {
                    parser.addTokens(lexerResult.tokens)
                }
            }
            val parseResult = parser.nextStatement()
            if (parseResult is StatementParsed) {
                statements.add(parseResult.statement)
            } else if (parseResult is StreamingParserResult.Error) {
                if (parseResult.message.lowercase(Locale.getDefault()).contains("need more tokens")) {
                    continue
                }
                break
            } else if (parseResult === StreamingParserResult.Finished) {
                ResultType.SUCCESS
            }
        }
        return ResultType.FAILURE
    }
}