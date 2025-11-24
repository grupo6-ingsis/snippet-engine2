package org.gudelker.snippet.engine.redis

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.gudelker.snippet.engine.utils.dto.FormatRequest
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
class FormatConsumer(
    private val formatEngine: FormatEngineService,
    private val redisTemplate: RedisTemplate<String, String>,
    private val container: StreamMessageListenerContainer<String, MapRecord<String, String, String>>,
    private val objectMapper: ObjectMapper,
) : StreamListener<String, MapRecord<String, String, String>> {
    private val logger = LoggerFactory.getLogger(FormatConsumer::class.java)
    private val streamKey = "formatting-requests"
    private val group = "format-engine-group"
    private val consumerName = "engine-2"

    @PostConstruct
    fun init() {
        logger.info("Initializing FormatConsumer for stream: {}", streamKey)

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
            "FormatConsumer started. Listening to stream: {}, group: {}, consumer: {}",
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
            logger.info("Redis: Received format request event")
            logger.debug("Redis: Record ID: {}, Fields: {}", record.id, record.value)

            // Extraer JSON del record
            val jsonString =
                record.value["data"]
                    ?: throw IllegalArgumentException("Missing 'data' field in record")

            logger.debug("Redis: JSON payload: {}", jsonString)

            // Deserializar request
            val request = objectMapper.readValue(jsonString, FormatRequest::class.java)
            logger.info(
                "Redis: Processing format for snippetId: {}, Rules count: {}",
                request.snippetId,
                request.allRules.size,
            )

            // Ejecutar lógica de format
            val results = formatEngine.processFormat(request)

            logger.info(
                "Redis: Format processing completed successfully. SnippetId: {}, Results: {}",
                request.snippetId,
                results,
            )
        } catch (e: IllegalArgumentException) {
            logger.error("Redis: Invalid message format. Error: {}", e.message)
        } catch (e: Exception) {
            logger.error("Redis: Format processing failed. Error: {}", e.message, e)
        } finally {
            MDC.remove("correlation-id")
        }
    }
}
