package org.gudelker.snippet.engine.redis

import org.gudelker.snippet.engine.EngineService
import org.gudelker.snippet.engine.api.AssetApiClient
import org.gudelker.snippet.engine.createJsonRuleMap
import org.gudelker.snippet.engine.utils.dto.LintRequest
import org.gudelker.snippet.engine.utils.dto.LintResultRequest
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream

@Service
class LintEngineService(
    private val assetClient: AssetApiClient,
    private val engineService: EngineService,
) {
    fun processLint(request: LintRequest): List<LintResultRequest> {
        val snippetContent =
            assetClient.getAsset(
                container = "snippets",
                key = request.snippetId,
            )
        val src = ByteArrayInputStream(snippetContent.toByteArray())

        val configRules = createJsonRuleMap(request.userRules, request.allRules)
        return engineService.lintSnippet(src, version = request.snippetVersion, configRules)
    }
}
