package org.gudelker.snippet.engine.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.gudelker.snippet.engine.utils.dto.LintRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.data.redis.stream.StreamMessageListenerContainer
import java.time.Duration
import kotlin.jvm.java

@Configuration
@Profile("!test")
class RedisStreamConfig(
    private val factory: RedisConnectionFactory,
) {
    @Bean
    fun objectMapper(): ObjectMapper = ObjectMapper().registerKotlinModule()

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
    fun redisTemplate(
        factory: RedisConnectionFactory,
        objectMapper: ObjectMapper,
    ): RedisTemplate<String, LintRequest> {
        val template = RedisTemplate<String, LintRequest>()
        template.connectionFactory = factory
        template.keySerializer = StringRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()
        template.valueSerializer = GenericJackson2JsonRedisSerializer(objectMapper)
        template.hashValueSerializer = GenericJackson2JsonRedisSerializer(objectMapper)
        template.afterPropertiesSet()
        return template
    }
}
