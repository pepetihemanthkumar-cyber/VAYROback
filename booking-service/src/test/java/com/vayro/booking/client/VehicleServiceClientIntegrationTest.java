package com.vayro.booking.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vayro.booking.dto.VehicleDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class VehicleServiceClientIntegrationTest {

    private VehicleServiceClientImpl vehicleServiceClient;
    private MockRestServiceServer mockServer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        this.mockServer = MockRestServiceServer.bindTo(builder).build();
        this.vehicleServiceClient = new VehicleServiceClientImpl(builder);
        this.objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("1. Successfully retrieves vehicle by ID via RestClient")
    void testGetVehicleByIdSuccess() throws Exception {
        VehicleDto expected = new VehicleDto();
        expected.setId("aprilia-rs-457");
        expected.setName("Aprilia RS 457");
        expected.setBrand("Aprilia");
        expected.setPricePerDay(2400.0);
        expected.setStatus("AVAILABLE");

        mockServer.expect(requestTo("http://VEHICLE-SERVICE/api/vehicles/aprilia-rs-457"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expected), MediaType.APPLICATION_JSON));

        Optional<VehicleDto> actual = vehicleServiceClient.getVehicleById("aprilia-rs-457");

        assertTrue(actual.isPresent());
        assertEquals("aprilia-rs-457", actual.get().getId());
        assertEquals("Aprilia RS 457", actual.get().getName());
        assertEquals("AVAILABLE", actual.get().getStatus());
        assertEquals(2400.0, actual.get().getPricePerDay());

        mockServer.verify();
    }

    @Test
    @DisplayName("2. Returns Optional.empty() when Vehicle Service responds with 404 NOT_FOUND")
    void testGetVehicleByIdNotFound() {
        mockServer.expect(requestTo("http://VEHICLE-SERVICE/api/vehicles/nonexistent-veh"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withResourceNotFound());

        Optional<VehicleDto> actual = vehicleServiceClient.getVehicleById("nonexistent-veh");

        assertTrue(actual.isEmpty());
        mockServer.verify();
    }

    @Test
    @DisplayName("3. Returns Optional.empty() when Vehicle Service responds with 500 SERVER_ERROR")
    void testGetVehicleByIdServerError() {
        mockServer.expect(requestTo("http://VEHICLE-SERVICE/api/vehicles/error-veh"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        Optional<VehicleDto> actual = vehicleServiceClient.getVehicleById("error-veh");

        assertTrue(actual.isEmpty());
        mockServer.verify();
    }

    @Test
    @DisplayName("4. Propagates JWT Authorization header when updating vehicle status")
    void testUpdateVehicleStatusWithJwtPropagation() {
        String token = "eyJhbGciOiJIUzI1NiJ9.test-admin-jwt";

        mockServer.expect(requestTo("http://VEHICLE-SERVICE/api/vehicles/aprilia-rs-457/status"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json("{\"status\":\"RENTED\"}"))
                .andRespond(withStatus(HttpStatus.OK));

        boolean updated = vehicleServiceClient.updateVehicleStatus("aprilia-rs-457", "RENTED", token);

        assertTrue(updated);
        mockServer.verify();
    }

    @Test
    @DisplayName("5. Returns false when updating vehicle status fails on downstream service")
    void testUpdateVehicleStatusFailure() {
        String token = "user-jwt-without-admin-permission";

        mockServer.expect(requestTo("http://VEHICLE-SERVICE/api/vehicles/aprilia-rs-457/status"))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        boolean updated = vehicleServiceClient.updateVehicleStatus("aprilia-rs-457", "RENTED", token);

        assertFalse(updated);
        mockServer.verify();
    }

    @Test
    @DisplayName("6. Returns Optional.empty() when vehicleId is null or empty without making HTTP call")
    void testGetVehicleByIdBlankParam() {
        Optional<VehicleDto> resultNull = vehicleServiceClient.getVehicleById(null);
        Optional<VehicleDto> resultEmpty = vehicleServiceClient.getVehicleById("   ");

        assertTrue(resultNull.isEmpty());
        assertTrue(resultEmpty.isEmpty());
    }
}
