package org.gudelker.snippet.engine.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.data.redis.stream.StreamMessageListenerContainer
import java.time.Duration

@Configuration
@Profile("!test")
class RedisStreamConfig(
    private val factory: RedisConnectionFactory,
) {
    @Bean
    fun objectMapper(): ObjectMapper = ObjectMapper().registerKotlinModule()

    @Bean
    fun streamListenerContainer(): StreamMessageListenerContainer<String, MapRecord<String, String, String>> {
        val options =
            StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                .builder()
                .pollTimeout(Duration.ofSeconds(1))
                // No especificar targetType para recibir como MapRecord
                .build()

        return StreamMessageListenerContainer.create(factory, options)
    }

    @Bean
    fun redisTemplate(factory: RedisConnectionFactory): RedisTemplate<String, String> {
        val template = RedisTemplate<String, String>()
        template.connectionFactory = factory
        template.keySerializer = StringRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()
        template.valueSerializer = StringRedisSerializer()
        template.hashValueSerializer = StringRedisSerializer()
        template.afterPropertiesSet()
        return template
    }
}
