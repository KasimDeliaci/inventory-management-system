package com.petek.inventoryService.spec;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.petek.inventoryService.dto.stock.StockMovementFilterRequest;
import com.petek.inventoryService.entity.StockMovement;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class StockMovementSpecifications {

    public static Specification<StockMovement> withFilters(StockMovementFilterRequest request) {
        return (Root<StockMovement> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Product ID filter (OR logic within product IDs)
            if (request.getProductId() != null && !request.getProductId().isEmpty()) {
                predicates.add(root.get("product").get("productId").in(request.getProductId()));
            }
            
            // Movement source filter
            if (request.getMovementSource() != null) {
                predicates.add(cb.equal(root.get("movementSource"), request.getMovementSource()));
            }
            
            // Movement kind filter
            if (request.getMovementKind() != null) {
                predicates.add(cb.equal(root.get("movementKind"), request.getMovementKind()));
            }
            
            // Movement date range filters
            if (request.getMovementDateGte() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("movementDate"), request.getMovementDateGte()));
            }
            if (request.getMovementDateLte() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("movementDate"), request.getMovementDateLte()));
            }
            
            // Source ID filter
            if (request.getSourceId() != null) {
                predicates.add(cb.equal(root.get("sourceId"), request.getSourceId()));
            }
            
            // Source item ID filter
            if (request.getSourceItemId() != null) {
                predicates.add(cb.equal(root.get("sourceItemId"), request.getSourceItemId()));
            }
            
            // Updated after filter
            if (request.getUpdatedAfter() != null) {
                predicates.add(cb.greaterThan(root.get("updatedAt"), request.getUpdatedAfter()));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    
}
