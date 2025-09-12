package com.petek.inventoryService.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.petek.inventoryService.dto.customerSpecialOffer.CustomerSpecialOfferCreateRequest;
import com.petek.inventoryService.dto.customerSpecialOffer.CustomerSpecialOfferResponse;
import com.petek.inventoryService.entity.CustomerSpecialOffer;
import com.petek.inventoryService.mapper.CustomerSpecialOfferMapper;
import com.petek.inventoryService.repository.CustomerRepository;
import com.petek.inventoryService.repository.CustomerSpecialOfferRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerSpecialOfferService {
    
    private final CustomerSpecialOfferRepository repository;
    private final CustomerSpecialOfferMapper mapper;

    private final CustomerRepository customerRepository;

    /**
     * Create customer special offer.
     */
    public CustomerSpecialOfferResponse createCustomerSpecialOffer(CustomerSpecialOfferCreateRequest request) {
        // Validate date range
        if (request.getStartDate() != null && request.getEndDate() != null &&
            request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("start_date cannot be after end_date");
        }

        customerRepository.findById(request.getCustomerId())
            .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + request.getCustomerId()));
        
        CustomerSpecialOffer customerSpecialOffer = mapper.toCustomerSpecialOffer(request);

        customerSpecialOffer.setCreatedAt(Instant.now());
        customerSpecialOffer.setUpdatedAt(Instant.now());

        return mapper.toCustomerSpecialOfferResponse(repository.save(customerSpecialOffer));
    }

    /**
     * Get a customer special offer by id.
     */
    public CustomerSpecialOfferResponse getCustomerSpecialOfferById(Long customerSpecialOfferId) {
        return repository.findById(customerSpecialOfferId)
            .map(mapper::toCustomerSpecialOfferResponse)
            .orElseThrow(() -> new EntityNotFoundException("Customer Special Offer not found with id: " + customerSpecialOfferId));
    }

}
