package com.vayro.gateway.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;

import java.net.ConnectException;

import static org.junit.jupiter.api.Assertions.*;

class GatewayErrorWebExceptionHandlerTest {

    private GatewayErrorWebExceptionHandler exceptionHandler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        exceptionHandler = new GatewayErrorWebExceptionHandler(objectMapper);
    }

    @Test
    @DisplayName("Should handle 503 Service Unavailable when downstream connection fails")
    void testHandleDownstreamConnectionException() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/bookings/my").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ConnectException connectException = new ConnectException("Connection refused");

        StepVerifier.create(exceptionHandler.handle(exchange, connectException))
                .verifyComplete();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("Should handle 404 ResponseStatusException cleanly")
    void testHandleNotFoundException() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/unknown").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ResponseStatusException rse = new ResponseStatusException(HttpStatus.NOT_FOUND, "Route not found");

        StepVerifier.create(exceptionHandler.handle(exchange, rse))
                .verifyComplete();

        assertEquals(HttpStatus.NOT_FOUND, exchange.getResponse().getStatusCode());
    }
}
