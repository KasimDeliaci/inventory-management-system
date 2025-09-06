package com.petek.inventoryService.spec;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.petek.inventoryService.dto.productSupplier.ProductSupplierFilterRequest;
import com.petek.inventoryService.entity.ProductSupplier;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class ProductSupplierSpecifications {
    
    public static Specification<ProductSupplier> withFilters(ProductSupplierFilterRequest request) {
        return (Root<ProductSupplier> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Product ID filter (OR logic within product IDs)
            if (request.getProductId() != null && !request.getProductId().isEmpty()) {
                predicates.add(root.get("product").in(request.getProductId()));
            }
            
            // Supplier ID filter (OR logic within supplier IDs)
            if (request.getSupplierId() != null && !request.getSupplierId().isEmpty()) {
                predicates.add(root.get("supplier").in(request.getSupplierId()));
            }
            
            // Active filter - only add if explicitly set
            if (request.getActive() != null) {
                predicates.add(cb.equal(root.get("active"), request.getActive()));
            }
            
            // Preferred filter - only add if explicitly set
            if (request.getPreferred() != null) {
                predicates.add(cb.equal(root.get("isPreferred"), request.getPreferred()));
        }
            
            // Last delivery since filter
            if (request.getLastDeliverySince() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("lastDeliveryDate"), request.getLastDeliverySince()));
            }
            
            // Updated after filter
            if (request.getUpdatedAfter() != null) {
                predicates.add(cb.greaterThan(root.get("updatedAt"), request.getUpdatedAfter()));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

}
