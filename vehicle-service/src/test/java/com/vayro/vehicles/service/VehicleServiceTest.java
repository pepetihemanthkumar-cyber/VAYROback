package com.vayro.vehicles.service;

import com.vayro.vehicles.dto.VehicleAvailabilityResponse;
import com.vayro.vehicles.dto.VehicleResponse;
import com.vayro.vehicles.entity.MaintenanceStatus;
import com.vayro.vehicles.entity.Vehicle;
import com.vayro.vehicles.entity.VehicleCategory;
import com.vayro.vehicles.entity.VehicleStatus;
import com.vayro.vehicles.exception.VehicleNotFoundException;
import com.vayro.vehicles.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private VehicleService vehicleService;

    private Vehicle sampleVehicle;

    @BeforeEach
    void setUp() {
        sampleVehicle = new Vehicle();
        sampleVehicle.setId("tata-nexon");
        sampleVehicle.setName("Tata Nexon");
        sampleVehicle.setBrand("Tata");
        sampleVehicle.setModel("Nexon");
        sampleVehicle.setYear(2024);
        sampleVehicle.setVehicleType("car");
        sampleVehicle.setCategory(VehicleCategory.SUV);
        sampleVehicle.setPricePerDay(3200.0);
        sampleVehicle.setPricePerHour(320.0);
        sampleVehicle.setFuelType("Petrol");
        sampleVehicle.setTransmission("Automatic");
        sampleVehicle.setSeats(5);
        sampleVehicle.setStatus(VehicleStatus.AVAILABLE);
        sampleVehicle.setMaintenanceStatus(MaintenanceStatus.GOOD);
        sampleVehicle.setImage1("/assets/vehicles/tata/nexon/nexon-front.webp");
        sampleVehicle.setImage2("/assets/vehicles/tata/nexon/nexon-rear.webp");
    }

    @Test
    void testGetVehicleByIdSuccess() {
        when(vehicleRepository.findById("tata-nexon")).thenReturn(Optional.of(sampleVehicle));

        VehicleResponse response = vehicleService.getVehicleById("tata-nexon");
        assertNotNull(response);
        assertEquals("Tata Nexon", response.getName());
        assertEquals(2, response.getImages().size());
        assertEquals("/assets/vehicles/tata/nexon/nexon-front.webp", response.getImages().get(0));
        assertEquals("/assets/vehicles/tata/nexon/nexon-rear.webp", response.getImages().get(1));
    }

    @Test
    void testGetVehicleByIdNotFound() {
        when(vehicleRepository.findById("unknown-vehicle")).thenReturn(Optional.empty());

        assertThrows(VehicleNotFoundException.class, () -> vehicleService.getVehicleById("unknown-vehicle"));
    }

    @Test
    void testCheckAvailabilityAvailable() {
        when(vehicleRepository.findById("tata-nexon")).thenReturn(Optional.of(sampleVehicle));

        VehicleAvailabilityResponse response = vehicleService.checkAvailability("tata-nexon");
        assertNotNull(response);
        assertTrue(response.isAvailable());
        assertEquals(VehicleStatus.AVAILABLE, response.getStatus());
    }

    @Test
    void testCheckAvailabilityRented() {
        sampleVehicle.setStatus(VehicleStatus.RENTED);
        when(vehicleRepository.findById("tata-nexon")).thenReturn(Optional.of(sampleVehicle));

        VehicleAvailabilityResponse response = vehicleService.checkAvailability("tata-nexon");
        assertNotNull(response);
        assertFalse(response.isAvailable());
        assertEquals(VehicleStatus.RENTED, response.getStatus());
    }

    @Test
    void testUpdateStatusSuccess() {
        when(vehicleRepository.findById("tata-nexon")).thenReturn(Optional.of(sampleVehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(i -> i.getArgument(0));

        VehicleResponse response = vehicleService.updateStatus("tata-nexon", VehicleStatus.MAINTENANCE);
        assertNotNull(response);
        assertEquals(VehicleStatus.MAINTENANCE, response.getStatus());
        verify(vehicleRepository, times(1)).save(sampleVehicle);
    }
}
