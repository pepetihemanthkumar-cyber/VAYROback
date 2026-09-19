package com.vayro.gateway.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vayro.gateway.dto.ErrorResponse;
import com.vayro.gateway.security.JwtReactiveAuthenticationManager;
import com.vayro.gateway.security.JwtServerAuthenticationConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsProcessor;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final JwtReactiveAuthenticationManager authenticationManager;
    private final JwtServerAuthenticationConverter authenticationConverter;
    private final ObjectMapper objectMapper;

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    public SecurityConfig(
            JwtReactiveAuthenticationManager authenticationManager,
            JwtServerAuthenticationConverter authenticationConverter,
            ObjectMapper objectMapper
    ) {
        this.authenticationManager = authenticationManager;
        this.authenticationConverter = authenticationConverter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        AuthenticationWebFilter authenticationWebFilter = new AuthenticationWebFilter(authenticationManager);
        authenticationWebFilter.setServerAuthenticationConverter(authenticationConverter);
        authenticationWebFilter.setSecurityContextRepository(NoOpServerSecurityContextRepository.getInstance());
        authenticationWebFilter.setRequiresAuthenticationMatcher(
                org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers(
                        "/api/users/**",
                        "/api/bookings/**",
                        "/api/notifications/**",
                        "/api/vehicles/**"
                )
        );
        authenticationWebFilter.setAuthenticationFailureHandler((webFilterExchange, exception) ->
                authenticationEntryPoint().commence(webFilterExchange.getExchange(), exception)
        );

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .cors(ServerHttpSecurity.CorsSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .authorizeExchange(exchanges -> exchanges
                        // Public CORS preflight requests
                        .matchers(exchange -> org.springframework.web.cors.reactive.CorsUtils.isPreFlightRequest(exchange.getRequest())
                                ? org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult.match()
                                : org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult.notMatch()).permitAll()
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Actuator Health and Info
                        .pathMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()

                        // Public Auth Endpoints
                        .pathMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login", "/api/auth/google").permitAll()

                        // Public Vehicle Catalogue Read Endpoints
                        .pathMatchers(HttpMethod.GET, "/api/vehicles/**").permitAll()

                        // Public Booking Availability Checking
                        .pathMatchers(HttpMethod.GET, "/api/bookings/availability/**").permitAll()

                        // Protected User endpoints
                        .pathMatchers("/api/users/**").authenticated()

                        // Protected Booking endpoints
                        .pathMatchers("/api/bookings/**").authenticated()

                        // Notifications endpoints
                        .pathMatchers(HttpMethod.GET, "/api/notifications/status/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/notifications/latest/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/notifications/invoice/**").permitAll()
                        .pathMatchers("/api/notifications/**").authenticated()

                        // Protected Vehicle mutation endpoints
                        .pathMatchers(HttpMethod.POST, "/api/vehicles/**").authenticated()
                        .pathMatchers(HttpMethod.PUT, "/api/vehicles/**").authenticated()
                        .pathMatchers(HttpMethod.PATCH, "/api/vehicles/**").authenticated()
                        .pathMatchers(HttpMethod.DELETE, "/api/vehicles/**").authenticated()

                        // Default: authenticate all other API calls
                        .anyExchange().authenticated()
                )
                .addFilterAt(authenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @Bean
    public ServerAuthenticationEntryPoint authenticationEntryPoint() {
        return (exchange, ex) -> {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

            ErrorResponse error = new ErrorResponse(
                    HttpStatus.UNAUTHORIZED.value(),
                    "UNAUTHORIZED",
                    "Authentication is required.",
                    exchange.getRequest().getPath().value()
            );

            byte[] bytes = serializeError(error);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        };
    }

    @Bean
    public ServerAccessDeniedHandler accessDeniedHandler() {
        return (exchange, denied) -> {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.FORBIDDEN);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

            ErrorResponse error = new ErrorResponse(
                    HttpStatus.FORBIDDEN.value(),
                    "FORBIDDEN",
                    "Access denied.",
                    exchange.getRequest().getPath().value()
            );

            byte[] bytes = serializeError(error);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        };
    }



    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsWebFilter corsWebFilter() {
        CorsConfigurationSource configSource = corsConfigurationSource();
        CorsProcessor processor = (corsConfig, exchange) -> {
            org.springframework.http.server.reactive.ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();
            if (!org.springframework.web.cors.reactive.CorsUtils.isCorsRequest(request)) {
                return true;
            }
            String origin = request.getHeaders().getOrigin();
            if (origin == null || corsConfig == null) {
                return true;
            }
            String allowOrigin = corsConfig.checkOrigin(origin);
            if (allowOrigin == null) {
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }
            HttpHeaders responseHeaders = response.getHeaders();
            responseHeaders.set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, allowOrigin);
            if (Boolean.TRUE.equals(corsConfig.getAllowCredentials())) {
                responseHeaders.set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            }
            if (org.springframework.web.cors.reactive.CorsUtils.isPreFlightRequest(request)) {
                HttpMethod requestMethod = request.getHeaders().getAccessControlRequestMethod();
                List<HttpMethod> allowMethods = corsConfig.checkHttpMethod(requestMethod);
                if (allowMethods != null) {
                    responseHeaders.setAccessControlAllowMethods(allowMethods);
                }
                List<String> requestHeaders = request.getHeaders().getAccessControlRequestHeaders();
                List<String> allowHeaders = corsConfig.checkHeaders(requestHeaders);
                if (allowHeaders != null) {
                    responseHeaders.setAccessControlAllowHeaders(allowHeaders);
                }
                if (corsConfig.getMaxAge() != null) {
                    responseHeaders.setAccessControlMaxAge(corsConfig.getMaxAge());
                }
                response.setStatusCode(HttpStatus.OK);
                return true;
            }
            if (corsConfig.getExposedHeaders() != null) {
                responseHeaders.setAccessControlExposeHeaders(corsConfig.getExposedHeaders());
            }
            return true;
        };
        return new CorsWebFilter(configSource, processor);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        if (allowedOrigins != null && !allowedOrigins.isBlank()) {
            Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(config::addAllowedOriginPattern);
        } else {
            config.addAllowedOriginPattern("http://localhost:*");
            config.addAllowedOriginPattern("https://vayro.com");
        }
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("PATCH");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("OPTIONS");
        config.addAllowedHeader("*");
        config.addExposedHeader("Authorization");
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        return exchange -> config;
    }

    private byte[] serializeError(ErrorResponse error) {
        try {
            return objectMapper.writeValueAsBytes(error);
        } catch (JsonProcessingException e) {
            String fallback = String.format(
                    "{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                    error.getStatus(), error.getError(), error.getMessage(), error.getPath()
            );
            return fallback.getBytes(StandardCharsets.UTF_8);
        }
    }
}
