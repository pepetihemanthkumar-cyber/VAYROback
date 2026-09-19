package com.vayro.vehicles.repository;

import com.vayro.vehicles.entity.Vehicle;
import com.vayro.vehicles.entity.VehicleCategory;
import com.vayro.vehicles.entity.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository with dynamic specification and pagination support for Vehicle assets.
 */
@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, String>, JpaSpecificationExecutor<Vehicle> {

    long countByCategory(VehicleCategory category);

    long countByVehicleType(String vehicleType);

    List<Vehicle> findByStatus(VehicleStatus status);
}
