package org.gudelker.snippet.engine.redis

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.gudelker.snippet.engine.utils.dto.LintRequest
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.connection.stream.Consumer
import org.springframework.data.redis.connection.stream.MapRecord
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
    private val redisTemplate: RedisTemplate<String, String>,
    private val container: StreamMessageListenerContainer<String, MapRecord<String, String, String>>,
    private val objectMapper: ObjectMapper,
) : StreamListener<String, MapRecord<String, String, String>> {
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
                .opsForStream<String, String>()
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

    override fun onMessage(record: MapRecord<String, String, String>) {
        println("📥 Received lint request event: $record")
        println("📋 Record fields: ${record.value}")

        try {
            // El producer envía el JSON string en el campo "data"
            val jsonString =
                record.value["data"]
                    ?: throw IllegalArgumentException("No se encontró el campo 'data' en el record")

            println("📄 JSON recibido: $jsonString")

            // Deserializar el JSON string a LintRequest
            val request = objectMapper.readValue(jsonString, LintRequest::class.java)
            val snippetId = request.snippetId
            println("🔧 Processing lint for snippetId: $snippetId")

            // Ejecutar lógica real
            val results = lintEngine.processLint(request)

            println("✅ Lint results for snippetId $snippetId: $results")
        } catch (e: Exception) {
            println("❌ Error deserializando mensaje: ${e.message}")
            e.printStackTrace()
        }
    }
}
