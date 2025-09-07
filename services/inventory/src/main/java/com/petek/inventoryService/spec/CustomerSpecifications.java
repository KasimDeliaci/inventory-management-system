package com.petek.inventoryService.spec;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.petek.inventoryService.dto.customer.CustomerFilterRequest;
import com.petek.inventoryService.entity.Customer;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class CustomerSpecifications {

    public static Specification<Customer> withFilters(CustomerFilterRequest request) {
        return (Root<Customer> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Query filter (customer name/email/phone contains)
            if (request.getQ() != null && !request.getQ().trim().isEmpty()) {
                String searchTerm = "%" + request.getQ().toLowerCase() + "%";
                Predicate namePredicate = cb.like(cb.lower(root.get("customerName")), searchTerm);
                Predicate emailPredicate = cb.like(cb.lower(root.get("email")), searchTerm);
                Predicate phonePredicate = cb.like(cb.lower(root.get("phoneNumber")), searchTerm);
                
                // OR logic for searching across multiple fields
                predicates.add(cb.or(namePredicate, emailPredicate, phonePredicate));
            }
            
            // Customer segment filter
            if (request.getSegment() != null) {
                predicates.add(cb.equal(root.get("segment"), request.getSegment()));
            }
            
            // City filter
            if (request.getCity() != null && !request.getCity().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("city")), 
                    "%" + request.getCity().toLowerCase() + "%"));
            }
            
            // Updated after filter
            if (request.getUpdatedAfter() != null) {
                predicates.add(cb.greaterThan(root.get("updatedAt"), request.getUpdatedAfter()));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    
}
