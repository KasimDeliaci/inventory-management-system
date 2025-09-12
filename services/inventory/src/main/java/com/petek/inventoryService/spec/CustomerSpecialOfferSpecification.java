package com.petek.inventoryService.spec;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.petek.inventoryService.dto.customerSpecialOffer.CustomerSpecialOfferFilterRequest;
import com.petek.inventoryService.entity.CustomerSpecialOffer;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class CustomerSpecialOfferSpecification {
    
    public static Specification<CustomerSpecialOffer> withFilters(CustomerSpecialOfferFilterRequest request) {
        return (Root<CustomerSpecialOffer> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Customer ID filter (OR logic within customer IDs)
            if (request.getCustomerId() != null && !request.getCustomerId().isEmpty()) {
                predicates.add(root.get("customerId").in(request.getCustomerId()));
            }
            
            // Percent off range filters
            if (request.getPercentGte() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("percentOff"), request.getPercentGte()));
            }
            if (request.getPercentLte() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("percentOff"), request.getPercentLte()));
            }
            
            // Start date range filters
            if (request.getStartGte() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), request.getStartGte()));
            }
            if (request.getStartLte() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), request.getStartLte()));
            }
            
            // End date range filters
            if (request.getEndGte() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), request.getEndGte()));
            }
            if (request.getEndLte() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("endDate"), request.getEndLte()));
            }
            
            // Active on date filter (inclusive window check)
            if (request.getActiveOn() != null) {
                predicates.add(cb.and(
                    cb.lessThanOrEqualTo(root.get("startDate"), request.getActiveOn()),
                    cb.greaterThanOrEqualTo(root.get("endDate"), request.getActiveOn())
                ));
            }
            
            // Updated after filter
            if (request.getUpdatedAfter() != null) {
                predicates.add(cb.greaterThan(root.get("updatedAt"), request.getUpdatedAfter()));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

}
