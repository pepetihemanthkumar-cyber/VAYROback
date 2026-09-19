package com.vayro.gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vayro.gateway.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@Order(-2)
public class GatewayErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayErrorWebExceptionHandler.class);
    private final ObjectMapper objectMapper;

    public GatewayErrorWebExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status;
        String errorName;
        String message;

        if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            if (status == HttpStatus.NOT_FOUND) {
                errorName = "NOT_FOUND";
                message = "The requested resource was not found.";
            } else if (status == HttpStatus.SERVICE_UNAVAILABLE) {
                errorName = "SERVICE_UNAVAILABLE";
                message = "The requested VAYRO service is currently unavailable.";
            } else {
                errorName = status.name();
                message = rse.getReason() != null ? rse.getReason() : "Request processing failed.";
            }
        } else if (ex.getClass().getSimpleName().contains("NotFoundException")) {
            status = HttpStatus.NOT_FOUND;
            errorName = "NOT_FOUND";
            message = "The requested service route was not found.";
        } else if (ex.getClass().getSimpleName().contains("ConnectException") ||
                ex.getClass().getSimpleName().contains("TimeoutException") ||
                ex.getMessage() != null && (ex.getMessage().contains("Connection refused") || ex.getMessage().contains("503 SERVICE_UNAVAILABLE"))) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            errorName = "SERVICE_UNAVAILABLE";
            message = "The requested VAYRO service is currently unavailable.";
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            errorName = "INTERNAL_SERVER_ERROR";
            message = "An unexpected error occurred while processing the gateway request.";
        }

        log.warn("Gateway error on path [{}]: status={}, message={}",
                exchange.getRequest().getPath().value(), status.value(), ex.getMessage());

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                errorName,
                message,
                exchange.getRequest().getPath().value()
        );

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(errorResponse);
        } catch (JsonProcessingException e) {
            String fallback = String.format(
                    "{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                    status.value(), errorName, message, exchange.getRequest().getPath().value()
            );
            bytes = fallback.getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
