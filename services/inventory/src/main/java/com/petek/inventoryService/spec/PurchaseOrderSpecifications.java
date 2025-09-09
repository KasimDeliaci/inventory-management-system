package com.petek.inventoryService.spec;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.petek.inventoryService.dto.purchaseOrder.PurchaseOrderFilterRequest;
import com.petek.inventoryService.entity.PurchaseOrder;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class PurchaseOrderSpecifications {

    public static Specification<PurchaseOrder> withFilters(PurchaseOrderFilterRequest request) {
        return (Root<PurchaseOrder> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Supplier IDs filter (OR logic within supplier IDs)
            if (request.getSupplierId() != null && !request.getSupplierId().isEmpty()) {
                predicates.add(root.get("supplier").get("id").in(request.getSupplierId()));
            }

            // Status filter (OR logic within statuses)
            if (request.getStatus() != null) {
                predicates.add(root.get("status").in(request.getStatus()));
            }

            // Order date >=
            if (request.getOrderDateGte() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("orderDate"), request.getOrderDateGte()));
            }

            // Order date <=
            if (request.getOrderDateLte() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("orderDate"), request.getOrderDateLte()));
            }

            // Expected delivery >=
            if (request.getExpectedDeliveryGte() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("expectedDeliveryDate"), request.getExpectedDeliveryGte()));
            }

            // Expected delivery <=
            if (request.getExpectedDeliveryLte() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("expectedDeliveryDate"), request.getExpectedDeliveryLte()));
            }

            // Received since (actualDelivery >=)
            if (request.getReceivedSince() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("actualDelivery"), request.getReceivedSince()));
            }

            // Updated after
            if (request.getUpdatedAfter() != null) {
                predicates.add(cb.greaterThan(root.get("updatedAt"), request.getUpdatedAfter()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    
}
