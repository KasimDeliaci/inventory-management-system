package com.petek.inventoryService.spec;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.petek.inventoryService.dto.campaign.CampaignFilterRequest;
import com.petek.inventoryService.entity.Campaign;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class CampaignSpecifications {
    
    public static Specification<Campaign> withFilters(CampaignFilterRequest request) {
        return (Root<Campaign> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Query filter (campaign name or description contains)
            if (request.getQ() != null && !request.getQ().trim().isEmpty()) {
                String searchTerm = "%" + request.getQ().toLowerCase() + "%";
                Predicate namePredicate = cb.like(cb.lower(root.get("campaignName")), searchTerm);
                Predicate descriptionPredicate = cb.like(cb.lower(root.get("description")), searchTerm);
                predicates.add(cb.or(namePredicate, descriptionPredicate));
            }
            
            // Campaign type filter
            if (request.getType() != null) {
                predicates.add(cb.equal(root.get("type"), request.getType()));
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
            
            // Active on specific date filter
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
