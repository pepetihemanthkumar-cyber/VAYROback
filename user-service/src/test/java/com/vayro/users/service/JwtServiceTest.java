package com.vayro.users.service;

import com.vayro.users.entity.Role;
import com.vayro.users.entity.User;
import com.vayro.users.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L); // 1 hour
    }

    @Test
    void testGenerateAndValidateToken() {
        User user = new User();
        user.setId(10L);
        user.setFirstName("Alex");
        user.setLastName("Mercer");
        user.setEmail("alex@example.com");
        user.setRole(Role.USER);

        String token = jwtService.generateToken(user);
        assertNotNull(token);
        assertFalse(token.isBlank());

        String username = jwtService.extractUsername(token);
        assertEquals("alex@example.com", username);

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                "alex@example.com", "password", Collections.emptyList()
        );

        assertTrue(jwtService.isTokenValid(token, userDetails));
        assertFalse(jwtService.isTokenExpired(token));
    }
}
