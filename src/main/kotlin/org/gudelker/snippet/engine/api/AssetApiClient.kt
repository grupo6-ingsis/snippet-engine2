package org.gudelker.snippet.engine.api

import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class AssetApiClient(
    private val restClient: RestClient,
) {
    private val baseUrl = "http://asset-service:8080/v1/asset"

    fun getAsset(
        container: String,
        key: String,
    ): String =
        restClient
            .get()
            .uri("$baseUrl/{container}/{key}", container, key)
            .retrieve()
            .body(String::class.java)
            ?: throw RuntimeException("Error fetching asset")

    fun updateAsset(
        container: String,
        key: String,
        content: String,
    ) {
        restClient
            .put()
            .uri("$baseUrl/{container}/{key}", container, key)
            .contentType(MediaType.TEXT_PLAIN)
            .body(content)
            .retrieve()
            .toBodilessEntity()
    }
}
