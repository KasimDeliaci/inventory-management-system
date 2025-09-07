package com.petek.inventoryService.spec;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.petek.inventoryService.dto.supplier.SupplierFilterRequest;
import com.petek.inventoryService.entity.Supplier;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class SupplierSpecifications {
    
    public static Specification<Supplier> withFilters(SupplierFilterRequest request) {
        return (Root<Supplier> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Query filter (supplier name contains)
            if (request.getQ() != null && !request.getQ().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("supplierName")), 
                    "%" + request.getQ().toLowerCase() + "%"));
            }
            
            // City filter (OR logic within cities)
            if (request.getCity() != null && !request.getCity().isEmpty()) {
                predicates.add(root.get("city").in(request.getCity()));
            }
            
            // Updated after filter
            if (request.getUpdatedAfter() != null) {
                predicates.add(cb.greaterThan(root.get("updatedAt"), request.getUpdatedAfter()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

}
