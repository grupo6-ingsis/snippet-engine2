package org.gudelker.snippet.engine.redis

import jakarta.annotation.PostConstruct
import org.gudelker.snippet.engine.utils.LintRequest
import org.springframework.data.redis.connection.stream.Consumer
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.connection.stream.ReadOffset
import org.springframework.data.redis.connection.stream.StreamOffset
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.stream.StreamListener
import org.springframework.data.redis.stream.StreamMessageListenerContainer
import org.springframework.stereotype.Service

@Service
class LintConsumer(
    private val lintEngine: LintEngineService,
    private val redisTemplate: RedisTemplate<String, Any>,
    private val container: StreamMessageListenerContainer<String, ObjectRecord<String, LintRequest>>
) : StreamListener<String, ObjectRecord<String, LintRequest>> {

    private val streamKey = "lint-requests"
    private val group = "lint-engine-group"
    private val consumerName = "engine-1"

    @PostConstruct
    fun init() {
        // Si el consumer group no existe, lo creamos
        try {
            redisTemplate
                .opsForStream<String, Any>()
                .createGroup(streamKey, group)
        } catch (e: Exception) {
            println("Consumer group ya existe, OK")
        }

        // Suscribir este listener al stream
        container.receive(
            Consumer.from(group, consumerName),
            StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
            this
        )

        container.start()
    }

    override fun onMessage(record: ObjectRecord<String, LintRequest>) {
        val value = record.value

        println("Recibí request de linteo: $value")

        // Ejecutar tu lógica real
        lintEngine.processLint(value)

        // ACK del mensaje
        redisTemplate
            .opsForStream<String, Any>()
            .acknowledge(streamKey, group, record.id)
    }
}
