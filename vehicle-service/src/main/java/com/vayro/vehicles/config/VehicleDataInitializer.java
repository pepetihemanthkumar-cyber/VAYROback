package com.vayro.vehicles.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vayro.vehicles.entity.Vehicle;
import com.vayro.vehicles.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * Startup initializer to seed the authoritative 207 fleet vehicle dataset idempotently.
 */
@Component
public class VehicleDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(VehicleDataInitializer.class);

    private final VehicleRepository vehicleRepository;
    private final ObjectMapper objectMapper;

    public VehicleDataInitializer(VehicleRepository vehicleRepository, ObjectMapper objectMapper) {
        this.vehicleRepository = vehicleRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) {
        try {
            long existingCount = vehicleRepository.count();
            if (existingCount >= 207) {
                log.info("Fleet catalogue already fully populated with {} vehicles.", existingCount);
                return;
            }

            ClassPathResource resource = new ClassPathResource("data/vehicles.json");
            if (!resource.exists()) {
                log.warn("Seed resource data/vehicles.json not found on classpath.");
                return;
            }

            try (InputStream is = resource.getInputStream()) {
                List<Vehicle> seedVehicles = objectMapper.readValue(is, new TypeReference<List<Vehicle>>() {});
                List<String> existingIds = vehicleRepository.findAll().stream().map(Vehicle::getId).toList();
                List<Vehicle> toInsert = seedVehicles.stream()
                        .filter(v -> !existingIds.contains(v.getId()))
                        .toList();

                if (!toInsert.isEmpty()) {
                    vehicleRepository.saveAll(toInsert);
                    log.info("Vehicle catalogue initialized. Added {} new vehicles. Total in database: {}",
                            toInsert.size(), vehicleRepository.count());
                } else {
                    log.info("All seed vehicles already exist in database.");
                }
            }
        } catch (Exception e) {
            log.error("Error initializing vehicle catalogue data: {}", e.getMessage(), e);
        }
    }
}
