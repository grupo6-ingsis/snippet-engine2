package org.gudelker.snippet.engine.redis

import org.gudelker.snippet.engine.EngineService
import org.gudelker.snippet.engine.api.AssetApiClient
import org.gudelker.snippet.engine.api.ServiceApiClient
import org.gudelker.snippet.engine.createJsonRuleMap
import org.gudelker.snippet.engine.utils.dto.LintRequest
import org.gudelker.snippet.engine.utils.dto.LintResultRequest
import org.gudelker.snippet.engine.utils.dto.SnippetIdWithLintResultsDto
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream

@Service
class LintEngineService(
    private val assetClient: AssetApiClient,
    private val engineService: EngineService,
    private val serviceApiClient: ServiceApiClient,
) {
    fun processLint(request: LintRequest): List<LintResultRequest> {
        println("LintEngineService.processLint called for snippetId: ${request.snippetId}, version: ${request.snippetVersion}")
        val snippetContent =
            assetClient.getAsset(
                container = "snippets",
                key = request.snippetId,
            )
        val src = ByteArrayInputStream(snippetContent.toByteArray())

        val configRules = createJsonRuleMap(request.userRules, request.allRules)
        val results = engineService.lintSnippet(src, version = request.snippetVersion, configRules)
        println("LintEngineService.processLint results for snippetId: ${request.snippetId}: $results")
        serviceApiClient.saveLintResults(SnippetIdWithLintResultsDto(request.snippetId, results))
        return results
    }
}
