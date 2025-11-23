package modules.auth0.unit

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.gudelker.snippet.engine.m2m.Auth0TokenResponse
import org.gudelker.snippet.engine.m2m.Auth0TokenService
import org.gudelker.snippet.engine.m2m.CachedTokenService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.springframework.web.client.RestClient

class Auth0TokenServiceTest {
    private val restClient = mockk<RestClient>(relaxed = true)
    private val clientId = "test-client-id"
    private val clientSecret = "test-client-secret"
    private val audience = "test-audience"
    private val tokenUrl = "https://test-domain/oauth/token"

    @Test
    fun `getMachineToMachineToken returns token when response is valid`() {
        val expectedResponse = Auth0TokenResponse("token123", "Bearer", 3600)
        val uriSpec = mockk<RestClient.RequestBodyUriSpec>(relaxed = true)
        val bodySpec = mockk<RestClient.RequestBodySpec>(relaxed = true)
        val responseSpec = mockk<RestClient.ResponseSpec>(relaxed = true)

        every { restClient.post() } returns uriSpec
        every { uriSpec.uri(tokenUrl) } returns bodySpec
        every { bodySpec.body(any<Map<String, String>>()) } returns bodySpec
        every { bodySpec.retrieve() } returns responseSpec
        every { responseSpec.body(Auth0TokenResponse::class.java) } returns expectedResponse

        val service = Auth0TokenService(restClient, clientId, clientSecret, audience, tokenUrl)
        val result = service.getMachineToMachineToken()
        Assertions.assertEquals(expectedResponse, result)
    }

    @Test
    fun `getMachineToMachineToken throws when response is null`() {
        val uriSpec = mockk<RestClient.RequestBodyUriSpec>(relaxed = true)
        val bodySpec = mockk<RestClient.RequestBodySpec>(relaxed = true)
        val responseSpec = mockk<RestClient.ResponseSpec>(relaxed = true)

        every { restClient.post() } returns uriSpec
        every { uriSpec.uri(tokenUrl) } returns bodySpec
        every { bodySpec.body(any<Map<String, String>>()) } returns bodySpec
        every { bodySpec.retrieve() } returns responseSpec
        every { responseSpec.body(Auth0TokenResponse::class.java) } returns null

        val service = Auth0TokenService(restClient, clientId, clientSecret, audience, tokenUrl)
        Assertions.assertThrows(RuntimeException::class.java) {
            service.getMachineToMachineToken()
        }
    }

    @Test
    fun `Auth0TokenResponse equals, hashCode, toString coverage`() {
        val r1 = Auth0TokenResponse("a", "Bearer", 1)
        val r2 = Auth0TokenResponse("a", "Bearer", 1)
        val r3 = Auth0TokenResponse("b", "Bearer", 2)
        Assertions.assertEquals(r1, r2)
        Assertions.assertNotEquals(r1, r3)
        Assertions.assertEquals(r1.hashCode(), r2.hashCode())
        Assertions.assertNotEquals(r1.hashCode(), r3.hashCode())
        Assertions.assertTrue(r1.toString().contains("access_token"))
    }

    @Test
    fun `getMachineToMachineToken throws for unexpected response type`() {
        val uriSpec = mockk<RestClient.RequestBodyUriSpec>(relaxed = true)
        val bodySpec = mockk<RestClient.RequestBodySpec>(relaxed = true)
        val responseSpec = mockk<RestClient.ResponseSpec>(relaxed = true)

        every { restClient.post() } returns uriSpec
        every { uriSpec.uri(tokenUrl) } returns bodySpec
        every { bodySpec.body(any<Map<String, String>>()) } returns bodySpec
        every { bodySpec.retrieve() } returns responseSpec
        every { responseSpec.body(Auth0TokenResponse::class.java) } throws IllegalArgumentException()

        val service = Auth0TokenService(restClient, clientId, clientSecret, audience, tokenUrl)
        Assertions.assertThrows(RuntimeException::class.java) {
            service.getMachineToMachineToken()
        }
    }

    @Test
    fun `CachedTokenService returns cached token and refreshes on expiry`() {
        val auth0TokenService = mockk<Auth0TokenService>()
        val token1 = Auth0TokenResponse("token1", "Bearer", 2)
        val token2 = Auth0TokenResponse("token2", "Bearer", 3600)
        every { auth0TokenService.getMachineToMachineToken() } returnsMany listOf(token1, token2)
        val cachedService = CachedTokenService(auth0TokenService)

        // First call fetches and caches token1
        val t1 = cachedService.getToken()
        Assertions.assertEquals("token1", t1)
        // Simulate expiry
        Thread.sleep(2100)
        // Second call fetches token2
        val t2 = cachedService.getToken()
        Assertions.assertEquals("token2", t2)
        verify(exactly = 2) { auth0TokenService.getMachineToMachineToken() }
    }
}
