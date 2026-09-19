package com.vayro.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Component
public class JwtReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    private static final Logger log = LoggerFactory.getLogger(JwtReactiveAuthenticationManager.class);
    private final JwtService jwtService;

    public JwtReactiveAuthenticationManager(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = authentication.getCredentials().toString();

        if (!jwtService.isTokenValid(token)) {
            log.warn("JWT token authentication failed: token is invalid or expired");
            return Mono.error(new BadCredentialsException("Invalid or expired JWT token"));
        }

        try {
            String username = jwtService.extractUsername(token);
            String role = jwtService.extractRole(token);
            String userId = jwtService.extractUserId(token);
            String name = jwtService.extractName(token);

            if (username == null) {
                return Mono.error(new BadCredentialsException("JWT token does not contain a valid subject"));
            }

            String authorityName = (role != null && !role.isBlank()) ?
                    (role.startsWith("ROLE_") ? role.toUpperCase() : "ROLE_" + role.toUpperCase()) :
                    "ROLE_USER";

            List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority(authorityName)
            );

            AuthenticatedPrincipal principal = new AuthenticatedPrincipal(userId, username, name, role);

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    principal,
                    token,
                    authorities
            );

            return Mono.just(auth);
        } catch (Exception e) {
            log.warn("Failed to parse JWT claims: {}", e.getMessage());
            return Mono.error(new BadCredentialsException("Malformed or unparseable JWT claims"));
        }
    }
}
