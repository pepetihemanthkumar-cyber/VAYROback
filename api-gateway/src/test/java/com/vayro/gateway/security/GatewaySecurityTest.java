package com.vayro.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
@AutoConfigureWebTestClient
class GatewaySecurityTest {

    private static final String SECRET_KEY = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @Autowired
    private WebTestClient webTestClient;

    private String generateToken(String username, String role, String userId, long expirationMillis) {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("userId", userId);
        claims.put("name", "Vayro Tester");

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(key)
                .compact();
    }

    @Test
    @DisplayName("Actuator health endpoint should be publicly accessible")
    void testActuatorHealthPublic() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    @DisplayName("Public auth endpoints (google) should be accessible without Authorization token")
    void testPublicGoogleAuthEndpoint() {
        webTestClient.post()
                .uri("/api/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"credential\":\"test-google-token\"}")
                .exchange()
                .expectStatus().isEqualTo(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE); // Passes gateway security filter (routes to USER-SERVICE which is offline in unit test, giving 503 instead of 401)
    }

    @Test
    @DisplayName("Unauthenticated request to protected endpoint should return 401 Unauthorized")
    void testProtectedUsersEndpointWithoutTokenReturns401() {
        webTestClient.get()
                .uri("/api/users/me")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.error").isEqualTo("UNAUTHORIZED")
                .jsonPath("$.message").isEqualTo("Authentication is required.")
                .jsonPath("$.path").isEqualTo("/api/users/me");
    }

    @Test
    @DisplayName("Unauthenticated request to protected bookings endpoint should return 401 Unauthorized")
    void testProtectedBookingsEndpointWithoutTokenReturns401() {
        webTestClient.post()
                .uri("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"vehicleId\":\"aprilia-rs-457\"}")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.error").isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("Request with malformed token should return 401 Unauthorized")
    void testMalformedTokenReturns401() {
        webTestClient.get()
                .uri("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer not.a.valid.jwt.token")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.error").isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("Request with expired token should return 401 Unauthorized")
    void testExpiredTokenReturns401() {
        String expiredToken = generateToken("user@vayro.com", "USER", "1", -60000);

        webTestClient.get()
                .uri("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.error").isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("CORS preflight OPTIONS request should return 200 OK with allow headers")
    void testCorsPreflightOptions() {
        webTestClient.options()
                .uri("http://localhost:8080/api/bookings")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173")
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
    }
}
