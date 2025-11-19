package org.gudelker.snippet.engine.redis

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.gudelker.snippet.engine.utils.dto.FormatRequest
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
class FormatConsumer(
    private val formatEngine: FormatEngineService,
    private val redisTemplate: RedisTemplate<String, String>,
    private val container: StreamMessageListenerContainer<String, MapRecord<String, String, String>>,
    private val objectMapper: ObjectMapper,
) : StreamListener<String, MapRecord<String, String, String>> {
    private val streamKey = "formatting-requests"
    private val group = "format-engine-group"
    private val consumerName = "engine-2"

    @PostConstruct
    fun init() {
        println("🔥 Deleting stream '$streamKey' on consumer startup...")
        redisTemplate.delete(streamKey)

        try {
            redisTemplate
                .opsForStream<String, String>()
                .createGroup(streamKey, group)
            println("👥 Group '$group' created.")
        } catch (e: Exception) {
            println("👥 Group '$group' already exists, OK.")
        }

        container.receive(
            Consumer.from(group, consumerName),
            StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
            this,
        )

        container.start()
        println("📡 FormatConsumer for '$streamKey' started.")
    }

    override fun onMessage(record: MapRecord<String, String, String>) {
        println("📥 Received format request event: $record")
        println("📋 Record fields: ${record.value}")

        try {
            val jsonString =
                record.value["data"]
                    ?: throw IllegalArgumentException("No 'data' field in record")

            println("📄 JSON received: $jsonString")

            val request = objectMapper.readValue(jsonString, FormatRequest::class.java)
            val snippetId = request.snippetId
            println("🖊️ Processing format for snippetId: $snippetId")

            val results = formatEngine.processFormat(request)

            println("✅ Format results for snippetId $snippetId: $results")
        } catch (e: Exception) {
            println("❌ Error deserializing message: ${e.message}")
            e.printStackTrace()
        }
    }
}
