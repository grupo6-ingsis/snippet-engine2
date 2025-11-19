package org.gudelker.snippet.engine.redis

import org.gudelker.snippet.engine.EngineService
import org.gudelker.snippet.engine.api.AssetApiClient
import org.gudelker.snippet.engine.createFormatJsonRuleMap
import org.gudelker.snippet.engine.utils.dto.FormatRequest
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream

@Service
class FormatEngineService(
    private val assetClient: AssetApiClient,
    private val engineService: EngineService,
) {
    fun processFormat(request: FormatRequest) {
        println("FormatEngine.processFormat called for snippetId: ${request.snippetId}, version: ${request.snippetVersion}")
        val snippetContent =
            assetClient.getAsset(
                container = "snippets",
                key = request.snippetId,
            )
        val src = ByteArrayInputStream(snippetContent.toByteArray())

        val configRules = createFormatJsonRuleMap(request.userRules, request.allRules)
        val result = engineService.formatSnippet(src, version = request.snippetVersion, configRules)
        assetClient.updateAsset("snippets", request.snippetId, result)
    }
}
