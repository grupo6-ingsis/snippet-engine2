package org.gudelker.snippet.engine

import org.gudelker.lexer.LexerFactory
import org.gudelker.lexer.StreamingLexer
import org.gudelker.lexer.StreamingLexerResult.TokenBatch
import org.gudelker.linter.DefaultLinterFactory.createLinter
import org.gudelker.linter.LinterConfig
import org.gudelker.parser.DefaultParser
import org.gudelker.parser.DefaultParserFactory
import org.gudelker.parser.StreamingParser
import org.gudelker.parser.StreamingParserResult
import org.gudelker.parser.StreamingParserResult.StatementParsed
import org.gudelker.snippet.engine.utils.ResultType
import org.gudelker.snippet.engine.utils.VersionAdapter
import org.gudelker.snippet.engine.utils.dto.LintResultRequest
import org.gudelker.sourcereader.InputStreamSourceReader
import org.gudelker.statements.interfaces.Statement
import org.gudelker.stmtposition.StatementStream
import org.gudelker.utilities.Version
import org.springframework.stereotype.Service
import java.io.InputStream
import java.util.Locale

@Service
class EngineService {
    fun parseSnippet(
        lexer: StreamingLexer,
        parser: StreamingParser,
        sourceReader: InputStreamSourceReader,
    ): ResultType {
        lexer.initialize(sourceReader)

        val statements: MutableList<Statement?> = ArrayList()
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
                return ResultType.FAILURE
            } else if (parseResult === StreamingParserResult.Finished) {
                return ResultType.SUCCESS
            }
        }
        return ResultType.SUCCESS
    }

    fun lintSnippet(
        src: InputStream,
        version: String,
        config: Map<String, LinterConfig>,
    ): List<LintResultRequest> {
        val v: Version = VersionAdapter.toVersion(version)
        val defaultLexer = LexerFactory.createLexer(v)
        val defaultParser: DefaultParser = DefaultParserFactory.createParser(v)
        val streamingLexer = StreamingLexer(defaultLexer)
        val streamingParser = StreamingParser(defaultParser)
        val sourceReader = InputStreamSourceReader(src, 8192)

        streamingLexer.initialize(sourceReader)

        val statements = mutableListOf<Statement>()
        while (streamingLexer.hasMore() || streamingParser.hasMore()) {
            if (streamingLexer.hasMore()) {
                val lexerResult = streamingLexer.nextBatch(10)
                if (lexerResult is TokenBatch) {
                    streamingParser.addTokens(lexerResult.tokens)
                }
            }
            val parseResult = streamingParser.nextStatement()
            if (parseResult is StatementParsed) {
                statements.add(parseResult.statement)
            } else if (parseResult is StreamingParserResult.Error) {
                if (parseResult.message.lowercase(Locale.getDefault()).contains("need more tokens")) {
                    continue
                }
                break
            } else if (parseResult === StreamingParserResult.Finished) {
                break
            }
        }

        // After getting statements and rules
        val statementStream = StatementStream(statements)
        val linter = createLinter(v)
        val result = linter.lint(statementStream, config)
        val lintResultsRequest = ArrayList<LintResultRequest>()

        for (violation in result.results) {
            lintResultsRequest.add(
                LintResultRequest(
                    violation.message,
                    violation.position.startLine,
                    violation.position.startColumn,
                ),
            )
        }

        return lintResultsRequest
    }
}
