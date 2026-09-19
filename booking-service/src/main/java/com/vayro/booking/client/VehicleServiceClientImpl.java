package com.vayro.booking.client;

import com.vayro.booking.dto.VehicleDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;

@Service
public class VehicleServiceClientImpl implements VehicleServiceClient {

    private static final Logger log = LoggerFactory.getLogger(VehicleServiceClientImpl.class);
    private static final String VEHICLE_SERVICE_BASE = "http://VEHICLE-SERVICE";

    private final RestClient restClient;

    public VehicleServiceClientImpl(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl(VEHICLE_SERVICE_BASE).build();
    }

    @Override
    public Optional<VehicleDto> getVehicleById(String vehicleId) {
        if (vehicleId == null || vehicleId.isBlank()) {
            return Optional.empty();
        }

        try {
            VehicleDto vehicle = restClient.get()
                    .uri("/api/vehicles/{id}", vehicleId)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange((request, response) -> {
                        if (response.getStatusCode().is2xxSuccessful()) {
                            return response.bodyTo(VehicleDto.class);
                        } else {
                            log.warn("Vehicle Service returned status {} for vehicleId: {}", response.getStatusCode(), vehicleId);
                            return null;
                        }
                    });

            if (vehicle == null || vehicle.getId() == null || vehicle.getId().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(vehicle);
        } catch (Exception e) {
            log.warn("Could not retrieve vehicle {} from Vehicle Service: {}", vehicleId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean updateVehicleStatus(String vehicleId, String status, String token) {
        if (vehicleId == null || vehicleId.isBlank() || status == null) {
            return false;
        }

        try {
            var requestSpec = restClient.patch()
                    .uri("/api/vehicles/{id}/status", vehicleId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("status", status));

            if (token != null && !token.isBlank()) {
                requestSpec.header(HttpHeaders.AUTHORIZATION, token.startsWith("Bearer ") ? token : "Bearer " + token);
            }

            requestSpec.retrieve()
                    .toBodilessEntity();

            log.info("Successfully updated vehicle {} status to {}", vehicleId, status);
            return true;
        } catch (Exception e) {
            log.warn("Failed to synchronize vehicle {} status to {}: {}", vehicleId, status, e.getMessage());
            return false;
        }
    }
}
