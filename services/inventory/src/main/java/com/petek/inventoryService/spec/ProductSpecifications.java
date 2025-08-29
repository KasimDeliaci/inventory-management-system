package com.petek.inventoryService.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import com.petek.inventoryService.dto.ProductFilterRequest;
import com.petek.inventoryService.entity.Product;

import java.util.ArrayList;
import java.util.List;

public class ProductSpecifications {
    
    public static Specification<Product> withFilters(ProductFilterRequest request) {
        return (Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Query filter (product name contains)
            if (request.q() != null && !request.q().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("productName")), 
                    "%" + request.q().toLowerCase() + "%"));
            }
            
            // Category filter (OR logic within categories)
            if (request.category() != null && !request.category().isEmpty()) {
                predicates.add(root.get("category").in(request.category()));
            }
            
            // Unit of measure filter (OR logic within UOMs)
            if (request.uom() != null && !request.uom().isEmpty()) {
                predicates.add(root.get("unitOfMeasure").in(request.uom()));
            }
            
            // Price range filters
            if (request.priceGte() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("currentPrice"), request.priceGte()));
            }
            if (request.priceLte() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("currentPrice"), request.priceLte()));
            }
            
            // Safety stock range filters
            if (request.safetyGte() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("safetyStock"), request.safetyGte()));
            }
            if (request.safetyLte() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("safetyStock"), request.safetyLte()));
            }
            
            // Reorder point range filters
            if (request.reorderGte() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("reorderPoint"), request.reorderGte()));
            }
            if (request.reorderLte() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("reorderPoint"), request.reorderLte()));
            }
            
            // Updated after filter
            if (request.updatedAfter() != null) {
                predicates.add(cb.greaterThan(root.get("updatedAt"), request.updatedAfter()));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}