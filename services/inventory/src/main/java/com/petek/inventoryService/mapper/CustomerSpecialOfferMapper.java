package com.petek.inventoryService.mapper;

import org.springframework.stereotype.Service;

import com.petek.inventoryService.dto.customerSpecialOffer.CustomerSpecialOfferCreateRequest;
import com.petek.inventoryService.dto.customerSpecialOffer.CustomerSpecialOfferResponse;
import com.petek.inventoryService.entity.CustomerSpecialOffer;

@Service
public class CustomerSpecialOfferMapper {
    
    /**
     * Map CustomerSpecialOfferCreateRequest to CustomerSpecialOffer entity.
     */
    public CustomerSpecialOffer toCustomerSpecialOffer(CustomerSpecialOfferCreateRequest request) {
        return CustomerSpecialOffer.builder()
            .customerId(request.getCustomerId())
            .percentOff(request.getPercentOff())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .build();
    }

    /**
     * Map CustomerSpecialOffer to CustomerSpecialOfferResponse.
     */
    public CustomerSpecialOfferResponse toCustomerSpecialOfferResponse(CustomerSpecialOffer customerSpecialOffer) {
        return CustomerSpecialOfferResponse.builder()
            .specialOfferId(customerSpecialOffer.getSpecialOfferId())
            .customerId(customerSpecialOffer.getCustomerId())
            .percentOff(customerSpecialOffer.getPercentOff())
            .startDate(customerSpecialOffer.getStartDate())
            .endDate(customerSpecialOffer.getEndDate())
            .createdAt(customerSpecialOffer.getCreatedAt())
            .updatedAt(customerSpecialOffer.getUpdatedAt())
            .build();
    }

}
