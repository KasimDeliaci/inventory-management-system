package com.petek.inventoryService.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import com.petek.inventoryService.dto.product.ProductFilterRequest;
import com.petek.inventoryService.entity.Product;

import java.util.ArrayList;
import java.util.List;

public class ProductSpecifications {
    
    public static Specification<Product> withFilters(ProductFilterRequest request) {
        return (Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Query filter (product name contains)
            if (request.getQ() != null && !request.getQ().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("productName")), 
                    "%" + request.getQ().toLowerCase() + "%"));
            }
            
            // Category filter (OR logic within categories)
            if (request.getCategory() != null && !request.getCategory().isEmpty()) {
                predicates.add(root.get("category").in(request.getCategory()));
            }
            
            // Unit of measure filter (OR logic within UOMs)
            if (request.getUom() != null && !request.getUom().isEmpty()) {
                predicates.add(root.get("unitOfMeasure").in(request.getUom()));
            }
            
            // Price range filters
            if (request.getPriceGte() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("currentPrice"), request.getPriceGte()));
            }
            if (request.getPriceLte() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("currentPrice"), request.getPriceLte()));
            }
            
            // Safety stock range filters
            if (request.getSafetyGte() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("safetyStock"), request.getSafetyGte()));
            }
            if (request.getSafetyLte() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("safetyStock"), request.getSafetyLte()));
            }
            
            // Reorder point range filters
            if (request.getReorderGte() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("reorderPoint"), request.getReorderGte()));
            }
            if (request.getReorderLte() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("reorderPoint"), request.getReorderLte()));
            }
            
            // Updated after filter
            if (request.getUpdatedAfter() != null) {
                predicates.add(cb.greaterThan(root.get("updatedAt"), request.getUpdatedAfter()));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}