package org.gudelker.snippet.engine.redis

import org.gudelker.snippet.engine.utils.LintRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.stream.StreamMessageListenerContainer
import java.time.Duration
import kotlin.jvm.java

@Configuration
class RedisStreamConfig(
    private val factory: RedisConnectionFactory
) {

    @Bean
    fun streamListenerContainer(): StreamMessageListenerContainer<String, ObjectRecord<String, LintRequest>> {
        val options = StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
            .pollTimeout(Duration.ofSeconds(1))
            .targetType(LintRequest::class.java)   // convierte JSON -> LintRequest
            .build()

        return StreamMessageListenerContainer.create(factory, options)
    }
}
