package com.vayro.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GatewayRouteConfigurationTest {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    @DisplayName("RouteLocator should load all required VAYRO downstream routes")
    void testRoutesConfigured() {
        List<Route> routes = routeLocator.getRoutes().collectList().block();

        assertNotNull(routes, "Routes should not be null");
        assertTrue(routes.size() >= 4, "Gateway should configure at least 4 routes");

        boolean hasAuthRoute = routes.stream().anyMatch(r ->
                "user-auth-route".equals(r.getId()) && "lb://USER-SERVICE".equals(r.getUri().toString()));
        boolean hasUserRoute = routes.stream().anyMatch(r ->
                "user-service-route".equals(r.getId()) && "lb://USER-SERVICE".equals(r.getUri().toString()));
        boolean hasVehicleRoute = routes.stream().anyMatch(r ->
                "vehicle-service-route".equals(r.getId()) && "lb://VEHICLE-SERVICE".equals(r.getUri().toString()));
        boolean hasBookingRoute = routes.stream().anyMatch(r ->
                "booking-service-route".equals(r.getId()) && "lb://BOOKING-SERVICE".equals(r.getUri().toString()));

        assertTrue(hasAuthRoute, "Auth route (lb://USER-SERVICE) must be present");
        assertTrue(hasUserRoute, "User route (lb://USER-SERVICE) must be present");
        assertTrue(hasVehicleRoute, "Vehicle route (lb://VEHICLE-SERVICE) must be present");
        assertTrue(hasBookingRoute, "Booking route (lb://BOOKING-SERVICE) must be present");

        // Verify no hardcoded localhost ports in route URIs
        for (Route route : routes) {
            String uri = route.getUri().toString();
            assertFalse(uri.contains("localhost:8081"), "Route URI must use Eureka service discovery, not localhost:8081");
            assertFalse(uri.contains("localhost:8082"), "Route URI must use Eureka service discovery, not localhost:8082");
            assertFalse(uri.contains("localhost:8083"), "Route URI must use Eureka service discovery, not localhost:8083");
        }
    }
}
