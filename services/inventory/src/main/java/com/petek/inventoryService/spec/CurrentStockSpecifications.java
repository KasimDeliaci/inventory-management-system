package com.petek.inventoryService.spec;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.petek.inventoryService.dto.stock.CurrentStockFilterRequest;
import com.petek.inventoryService.entity.CurrentStock;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class CurrentStockSpecifications {
    
    public static Specification<CurrentStock> withFilters(CurrentStockFilterRequest request) {
        return (Root<CurrentStock> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Product ID filter (OR logic within product IDs)
            if (request.getProductId() != null && !request.getProductId().isEmpty()) {
                predicates.add(root.get("productId").in(request.getProductId()));
            }
            
            // Available quantity range filters
            if (request.getAvailableGte() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("quantityAvailable"), request.getAvailableGte()));
            }
            if (request.getAvailableLte() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("quantityAvailable"), request.getAvailableLte()));
            }
            
            // On hand quantity range filters
            if (request.getOnHandGte() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("quantityOnHand"), request.getOnHandGte()));
            }
            if (request.getOnHandLte() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("quantityOnHand"), request.getOnHandLte()));
            }
            
            // Updated after filter
            if (request.getUpdatedAfter() != null) {
                predicates.add(cb.greaterThan(root.get("lastUpdated"), request.getUpdatedAfter()));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

}
