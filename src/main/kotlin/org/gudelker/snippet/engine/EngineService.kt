package org.gudelker.snippet.engine

import org.gudelker.StreamingPipeline
import org.gudelker.formatter.DefaultFormatterFactory.createFormatter
import org.gudelker.inputprovider.InputProvider
import org.gudelker.inputprovider.TestInputProvider
import org.gudelker.interpreter.ChunkBaseFactory.createInterpreter
import org.gudelker.interpreter.StreamingInterpreter
import org.gudelker.lexer.LexerFactory
import org.gudelker.lexer.LexerFactory.createLexer
import org.gudelker.lexer.StreamingLexer
import org.gudelker.lexer.StreamingLexerResult.TokenBatch
import org.gudelker.linter.DefaultLinterFactory.createLinter
import org.gudelker.linter.LinterConfig
import org.gudelker.parser.DefaultParser
import org.gudelker.parser.DefaultParserFactory
import org.gudelker.parser.DefaultParserFactory.createParser
import org.gudelker.parser.StreamingParser
import org.gudelker.parser.StreamingParserResult
import org.gudelker.parser.StreamingParserResult.StatementParsed
import org.gudelker.rules.FormatterRule
import org.gudelker.snippet.engine.utils.ResultType
import org.gudelker.snippet.engine.utils.VersionAdapter
import org.gudelker.snippet.engine.utils.dto.LintResultRequest
import org.gudelker.sourcereader.InputStreamSourceReader
import org.gudelker.statements.interfaces.Statement
import org.gudelker.stmtposition.StatementStream
import org.gudelker.utilities.Version
import org.springframework.stereotype.Service
import java.io.InputStream
import java.util.ArrayList
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

    fun formatSnippet(
        src: InputStream,
        version: String,
        config: Map<String, FormatterRule>,
    ): String {
        val v: Version = VersionAdapter.toVersion(version)
        val defaultLexer = LexerFactory.createLexer(v)
        val defaultParser: DefaultParser = DefaultParserFactory.createParser(v)
        val streamingLexer = StreamingLexer(defaultLexer)
        val streamingParser = StreamingParser(defaultParser)
        val sourceReader = InputStreamSourceReader(src, 8192)
        streamingLexer.initialize(sourceReader)

        val statements: MutableList<Statement> = ArrayList()
        while (streamingLexer.hasMore() || streamingParser.hasMore()) {
            if (streamingLexer.hasMore()) {
                val lexerResult = streamingLexer.nextBatch(10)
                if (lexerResult is TokenBatch) {
                    val tokenBatch = lexerResult
                    streamingParser.addTokens(tokenBatch.tokens)
                }
            }
            val parseResult = streamingParser.nextStatement()
            if (parseResult is StatementParsed) {
                val statementParsed = parseResult
                statements.add(statementParsed.statement)
            } else if (parseResult is StreamingParserResult.Error) {
                val error = parseResult
                if (error.message.lowercase(Locale.getDefault()).contains("need more tokens")) {
                    continue
                }
                break
            } else if (parseResult === StreamingParserResult.Finished) {
                break
            }
        }

        val formatter = createFormatter(v)
        val sb = StringBuilder()

        for ((index, statement) in statements.withIndex()) {
            val formatted = formatter.format(statement, config)
            if (index == statements.lastIndex) {
                sb.append(removeTrailingNewline(formatted))
            } else {
                sb.append(formatted)
            }
        }

        return sb.toString()
    }

    fun interpretSnippet(
        snippetContent: String,
        version: Version,
        inputs: MutableList<String>,
    ): ArrayList<String> {
        val inputStream = snippetContent.byteInputStream()
        val results = interpret(inputStream, version, TestInputProvider(inputs))
        return ArrayList(results)
    }

    private fun interpret(
        src: InputStream,
        version: Version,
        provider: InputProvider,
    ): MutableList<String?> {
        val lexer = createLexer(version)
        val streamingLexer = StreamingLexer(lexer)
        val parser = createParser(version)
        val streamingParser = StreamingParser(parser)
        val interpreter = createInterpreter(version, provider)
        val streamingInterpreter = StreamingInterpreter(interpreter.getEvaluators())
        val pipeline = StreamingPipeline(streamingLexer, streamingParser, streamingInterpreter)
        val processedResults: MutableList<String?> = ArrayList<String?>()
        val reader = InputStreamSourceReader(src, 8192)
        try {
            pipeline.initialize(reader)
            val success =
                pipeline.processAll { result: Any? ->
                    if (!(result is Unit || result == null)) {
                        processedResults.add(result.toString())
                    }
                    true
                }
            if (!success) {
                throw RuntimeException("Interpreter failed: ${pipeline.getLastErrorMessage()}")
            } else {
                return processedResults
            }
        } catch (e: OutOfMemoryError) {
            throw OutOfMemoryError("Memory limit exceeded during interpretation.")
        }
    }

    private fun removeTrailingNewline(str: String): String = if (str.endsWith("\n")) str.substring(0, str.length - 1) else str
}
