package com.vayro.booking.client;

import com.vayro.booking.dto.VehicleDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;

public interface VehicleServiceClient {
    Optional<VehicleDto> getVehicleById(String vehicleId);
    boolean updateVehicleStatus(String vehicleId, String status, String token);
}
