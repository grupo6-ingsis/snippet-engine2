package org.gudelker.snippet.engine.redis

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.gudelker.snippet.engine.utils.dto.LintRequest
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.connection.stream.Consumer
import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.data.redis.connection.stream.ReadOffset
import org.springframework.data.redis.connection.stream.StreamOffset
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.stream.StreamListener
import org.springframework.data.redis.stream.StreamMessageListenerContainer
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@Profile("!test")
class LintConsumer(
    private val lintEngine: LintEngineService,
    private val redisTemplate: RedisTemplate<String, String>,
    private val container: StreamMessageListenerContainer<String, MapRecord<String, String, String>>,
    private val objectMapper: ObjectMapper,
) : StreamListener<String, MapRecord<String, String, String>> {
    private val logger = LoggerFactory.getLogger(LintConsumer::class.java)
    private val streamKey = "lint-requests"
    private val group = "lint-engine-group"
    private val consumerName = "engine-1"

    @PostConstruct
    fun init() {
        logger.info("Initializing LintConsumer for stream: {}", streamKey)

        // Limpiar el stream para evitar mensajes viejos
        logger.debug("Deleting stream '{}' to avoid old messages", streamKey)
        redisTemplate.delete(streamKey)

        // Crear group
        try {
            redisTemplate
                .opsForStream<String, String>()
                .createGroup(streamKey, group)
            logger.info("Consumer group '{}' created successfully", group)
        } catch (e: Exception) {
            logger.debug("Consumer group '{}' already exists", group)
        }

        // Suscribir listener al stream
        container.receive(
            Consumer.from(group, consumerName),
            StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
            this,
        )

        container.start()
        logger.info(
            "LintConsumer started. Listening to stream: {}, group: {}, consumer: {}",
            streamKey,
            group,
            consumerName,
        )
    }

    override fun onMessage(record: MapRecord<String, String, String>) {
        // Generar correlation ID para trazabilidad
        val correlationId = UUID.randomUUID().toString()
        MDC.put("correlation-id", correlationId)

        try {
            logger.info("Redis: Received lint request event")
            logger.debug("Redis: Record ID: {}, Fields: {}", record.id, record.value)

            // Extraer JSON del record
            val jsonString =
                record.value["data"]
                    ?: throw IllegalArgumentException("Missing 'data' field in record")

            logger.debug("Redis: JSON payload: {}", jsonString)

            // Deserializar request
            val request = objectMapper.readValue(jsonString, LintRequest::class.java)
            logger.info(
                "Redis: Processing lint for snippetId: {}, Rules count: {}",
                request.snippetId,
                request.allRules.size,
            )

            // Ejecutar lógica de lint
            val results = lintEngine.processLint(request)

            logger.info(
                "Redis: Lint processing completed successfully. SnippetId: {}, Results: {}",
                request.snippetId,
                results,
            )
        } catch (e: IllegalArgumentException) {
            logger.error("Redis: Invalid message format. Error: {}", e.message)
        } catch (e: Exception) {
            logger.error("Redis: Lint processing failed. Error: {}", e.message, e)
        } finally {
            MDC.remove("correlation-id")
        }
    }
}
