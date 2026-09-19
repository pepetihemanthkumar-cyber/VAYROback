package com.vayro.vehicles.specification;

import com.vayro.vehicles.entity.Vehicle;
import com.vayro.vehicles.entity.VehicleCategory;
import com.vayro.vehicles.entity.VehicleStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;

/**
 * JPA Specifications for dynamic multi-attribute vehicle filtering.
 */
public class VehicleSpecifications {

    public static Specification<Vehicle> buildSpecification(
            VehicleCategory category,
            VehicleStatus status,
            String vehicleType,
            String search,
            Double minPrice,
            Double maxPrice
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (category != null) {
                predicates.add(criteriaBuilder.equal(root.get("category"), category));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (vehicleType != null && !vehicleType.isBlank()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("vehicleType")),
                        vehicleType.trim().toLowerCase()
                ));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate nameMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern);
                Predicate brandMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("brand")), pattern);
                Predicate modelMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("model")), pattern);
                predicates.add(criteriaBuilder.or(nameMatch, brandMatch, modelMatch));
            }

            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("pricePerDay"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("pricePerDay"), maxPrice));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
