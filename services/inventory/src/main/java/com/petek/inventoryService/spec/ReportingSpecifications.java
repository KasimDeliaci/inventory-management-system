package com.petek.inventoryService.spec;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.petek.inventoryService.dto.reporting.ProductDaySalesRequest;
import com.petek.inventoryService.entity.ProductDaySales;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class ReportingSpecifications {

    public static Specification<ProductDaySales> withFilters(ProductDaySalesRequest request) {
        return (Root<ProductDaySales> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Date range filters (from date - inclusive)
            if (request.getFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("date"), request.getFrom()));
            }
            
            // Date range filters (to date - inclusive)
            if (request.getTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("date"), request.getTo()));
            }
            
            // Product ID filter (OR logic within product IDs)
            if (request.getProductId() != null && !request.getProductId().isEmpty()) {
                predicates.add(root.get("productId").in(request.getProductId()));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

}
