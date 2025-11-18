package org.gudelker.snippet.engine.redis

import jakarta.annotation.PostConstruct
import org.gudelker.snippet.engine.utils.dto.LintRequest
import org.gudelker.snippet.engine.utils.dto.SnippetIdWithLintResultsDto
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.connection.stream.Consumer
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.connection.stream.ReadOffset
import org.springframework.data.redis.connection.stream.StreamOffset
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.stream.StreamListener
import org.springframework.data.redis.stream.StreamMessageListenerContainer
import org.springframework.stereotype.Service

@Service
@Profile("!test")
class LintConsumer(
    private val lintEngine: LintEngineService,
    private val redisTemplate: RedisTemplate<String, Any>,
    private val container: StreamMessageListenerContainer<String, ObjectRecord<String, LintRequest>>,
) : StreamListener<String, ObjectRecord<String, LintRequest>> {

    private val streamKey = "lint-requests"
    private val group = "lint-engine-group"
    private val consumerName = "engine-1"

    @PostConstruct
    fun init() {
        // ----------------------------------------------------
        // 🔥 LIMPIAR EL STREAM PARA EVITAR MENSAJES VIEJOS
        // ----------------------------------------------------
        println("🔥 Borrando stream '$streamKey' al iniciar consumidor...")
        redisTemplate.delete(streamKey)

        // ----------------------------------------------------
        // Crear group (solo si el stream existe)
        // ----------------------------------------------------
        try {
            redisTemplate
                .opsForStream<String, Any>()
                .createGroup(streamKey, group)
            println("👥 Grupo '$group' creado.")
        } catch (e: Exception) {
            println("👥 Grupo '$group' ya existe, OK.")
        }

        // ----------------------------------------------------
        // Suscribir este listener al stream
        // ----------------------------------------------------
        container.receive(
            Consumer.from(group, consumerName),
            StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
            this,
        )

        container.start()
        println("📡 Consumidor de '$streamKey' iniciado.")
    }

    override fun onMessage(record: ObjectRecord<String, LintRequest>) {
        println("📥 Received lint request event: $record")

        val request = record.value
        val snippetId = request.snippetId

        println("🔧 Processing lint for snippetId: $snippetId")

        // Ejecutar lógica real
        val results = lintEngine.processLint(request)

        println("✅ Lint results for snippetId $snippetId: $results")

        // Crear mensaje con resultados
        val snippetIdWithResults =
            SnippetIdWithLintResultsDto(
                snippetId,
                results,
            )

        // Publicar al stream 'lint-results'
        redisTemplate
            .opsForStream<String, Any>()
            .add(ObjectRecord.create("lint-results", snippetIdWithResults))

        println("📤 Published lint results for snippetId: $snippetId")

        // ACK
        redisTemplate
            .opsForStream<String, Any>()
            .acknowledge(streamKey, group, record.id)

        println("👍 Acknowledged message for snippetId: $snippetId")
    }
}
