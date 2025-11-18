package org.gudelker.snippet.engine.redis

import org.gudelker.snippet.engine.utils.dto.LintRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.stream.StreamMessageListenerContainer
import java.time.Duration
import kotlin.jvm.java

@Configuration
class RedisStreamConfig(
    private val factory: RedisConnectionFactory,
) {
    @Bean
    fun streamListenerContainer(): StreamMessageListenerContainer<String, ObjectRecord<String, LintRequest>> {
        val options =
            StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                .builder()
                .pollTimeout(Duration.ofSeconds(1))
                .targetType(LintRequest::class.java) // convierte JSON -> LintRequest
                .build()

        return StreamMessageListenerContainer.create(factory, options)
    }

    @Bean
    fun redisTemplate(): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.setConnectionFactory(factory)
        template.keySerializer =
            org.springframework.data.redis.serializer
                .StringRedisSerializer()
        template.valueSerializer =
            org.springframework.data.redis.serializer
                .GenericJackson2JsonRedisSerializer()
        return template
    }
}
