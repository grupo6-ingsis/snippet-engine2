package org.gudelker.snippet.engine.redis

import org.gudelker.snippet.engine.EngineService
import org.gudelker.snippet.engine.api.AssetApiClient
import org.gudelker.snippet.engine.createJsonRuleMap
import org.gudelker.snippet.engine.createStringInputSourceReader
import org.gudelker.snippet.engine.utils.LintRequest
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream

@Service
class LintEngineService(
    private val assetClient:  AssetApiClient,
    private val engineService: EngineService,
) {

    fun processLint(request: LintRequest) {
        val snippetContent = assetClient.getAsset(
            container = "snippets",
            key = request.snippetId
        )
        val src = ByteArrayInputStream(snippetContent.toByteArray())

        val configRules = createJsonRuleMap(request.userRules, request.allRules)
        engineService.lintSnippet(src, version = request.snippetVersion,configRules)
    }
}
