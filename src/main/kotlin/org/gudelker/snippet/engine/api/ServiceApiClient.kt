package org.gudelker.snippet.engine.api

import org.gudelker.snippet.engine.m2m.CachedTokenService
import org.gudelker.snippet.engine.utils.dto.SnippetIdWithLintResultsDto
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class ServiceApiClient(
    private val restClient: RestClient,
    private val cachedTokenService: CachedTokenService,
) {
    fun saveLintResults(request: SnippetIdWithLintResultsDto): SnippetIdWithLintResultsDto {
        val machineToken = cachedTokenService.getToken()
        return restClient
            .put()
            .uri("http://snippet-service:8080/lintresult/all")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $machineToken")
            .body(request)
            .retrieve()
            .body(SnippetIdWithLintResultsDto::class.java) ?: throw RuntimeException("No response from snippet service")
    }
}
