package org.gudelker.snippet.engine

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@SpringBootTest
class ApplicationTests {
    @MockBean
    private lateinit var redisConnectionFactory: RedisConnectionFactory

    @Test
    fun contextLoads() {
        println("Context")
    }
}
