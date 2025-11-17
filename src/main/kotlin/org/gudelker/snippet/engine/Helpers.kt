package org.gudelker.snippet.engine

import org.gudelker.lexer.LexerFactory
import org.gudelker.lexer.StreamingLexer
import org.gudelker.linter.LinterConfig
import org.gudelker.parser.DefaultParserFactory
import org.gudelker.parser.StreamingParser
import org.gudelker.snippet.engine.utils.RuleNameWithValue
import org.gudelker.sourcereader.InputStreamSourceReader
import org.gudelker.utilities.Version
import java.io.ByteArrayInputStream


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

fun createJsonRuleMap(userRules: List<RuleNameWithValue>, defaultRules: List<String>): Map<String, LinterConfig> {
    return defaultRules.associateWith { rule ->
        val userRule = userRules.find { it.ruleName == rule }
        val isEnabled = userRule != null
        LinterConfig(
            identifierFormat = userRule?.value ?: "",
            restrictPrintlnToIdentifierOrLiteral = isEnabled,
            restrictReadInputToIdentifierOrLiteral = isEnabled
        )
    }
}

