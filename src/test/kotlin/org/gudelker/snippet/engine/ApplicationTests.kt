package org.gudelker.snippet.engine

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@SpringBootTest
class ApplicationTests {
    @TestConfiguration
    class TestRedisConfig {
        @Bean
        @Primary
        fun redisConnectionFactory(): RedisConnectionFactory {
            return mock(RedisConnectionFactory::class.java)
        }
    }

    @Test
    fun contextLoads() {
        println("Context")
    }
}
