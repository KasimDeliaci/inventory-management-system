package com.petek.inventoryService.spec;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.petek.inventoryService.dto.salesOrder.SalesOrderFilterRequest;
import com.petek.inventoryService.entity.SalesOrder;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class SalesOrderSpecifications {

    public static Specification<SalesOrder> withFilters(SalesOrderFilterRequest request) {
        return (Root<SalesOrder> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Customer ID filter (OR logic within customer IDs)
            if (request.getCustomerId() != null && !request.getCustomerId().isEmpty()) {
                predicates.add(root.get("customerId").in(request.getCustomerId()));
            }
            
            // Status filter (OR logic within statuses)
            if (request.getStatus() != null) {
                predicates.add(root.get("status").in(request.getStatus()));
            }
            
            // Order date range filters
            if (request.getOrderDateGte() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("orderDate"), request.getOrderDateGte()));
            }
            if (request.getOrderDateLte() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("orderDate"), request.getOrderDateLte()));
            }
            
            // Delivery date range filters
            if (request.getDeliveryDateGte() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("deliveryDate"), request.getDeliveryDateGte()));
            }
            if (request.getDeliveryDateLte() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("deliveryDate"), request.getDeliveryDateLte()));
            }
            
            // Delivered since filter (deliveredAt >= deliveredSince)
            if (request.getDeliveredSince() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("deliveredAt"), request.getDeliveredSince()));
            }
            
            // Updated after filter (updatedAt >= updatedAfter)
            if (request.getUpdatedAfter() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("updatedAt"), request.getUpdatedAfter()));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

}
