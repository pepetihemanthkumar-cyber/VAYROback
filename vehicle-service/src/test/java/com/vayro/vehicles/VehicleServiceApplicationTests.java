package com.vayro.vehicles;

import com.vayro.vehicles.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class VehicleServiceApplicationTests {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Test
    void contextLoadsAndSeeds207Vehicles() {
        long totalVehicles = vehicleRepository.count();
        assertEquals(207, totalVehicles, "Vehicle repository should contain exactly 207 seeded vehicles");

        long cars = vehicleRepository.countByVehicleType("car");
        long bikes = vehicleRepository.countByVehicleType("bike");

        assertEquals(135, cars, "Expected 135 cars in catalogue");
        assertEquals(72, bikes, "Expected 72 bikes in catalogue");
    }
}
