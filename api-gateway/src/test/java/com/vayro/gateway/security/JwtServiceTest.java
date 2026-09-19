package com.vayro.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET_KEY = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET_KEY);
    }

    private String generateToken(String username, String role, String userId, String name, long expirationMillis) {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        Map<String, Object> claims = new HashMap<>();
        if (role != null) claims.put("role", role);
        if (userId != null) claims.put("userId", userId);
        if (name != null) claims.put("name", name);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(key)
                .compact();
    }

    @Test
    @DisplayName("Should extract username and claims from valid token")
    void testExtractClaimsFromValidToken() {
        String token = generateToken("user@vayro.com", "USER", "42", "Test User", 1000 * 60 * 60);

        assertTrue(jwtService.isTokenValid(token));
        assertEquals("user@vayro.com", jwtService.extractUsername(token));
        assertEquals("USER", jwtService.extractRole(token));
        assertEquals("42", jwtService.extractUserId(token));
        assertEquals("Test User", jwtService.extractName(token));
    }

    @Test
    @DisplayName("Should detect expired token")
    void testExpiredToken() {
        String token = generateToken("user@vayro.com", "USER", "42", "Test User", -1000);

        assertFalse(jwtService.isTokenValid(token));
    }

    @Test
    @DisplayName("Should reject malformed token")
    void testMalformedToken() {
        assertFalse(jwtService.isTokenValid("invalid.token.structure"));
        assertFalse(jwtService.isTokenValid(""));
    }

    @Test
    @DisplayName("Should reject token signed with different secret key")
    void testTokenWithDifferentSecret() {
        SecretKey otherKey = Keys.hmacShaKeyFor("differentSecretKeyForVayroApplicationTesting12345678".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("user@vayro.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(otherKey)
                .compact();

        assertFalse(jwtService.isTokenValid(token));
    }
}
