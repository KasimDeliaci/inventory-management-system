package com.petek.inventoryService.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petek.inventoryService.dto.PageResponse;
import com.petek.inventoryService.dto.PageResponse.PageInfo;
import com.petek.inventoryService.dto.customerSpecialOffer.CustomerSpecialOfferCreateRequest;
import com.petek.inventoryService.dto.customerSpecialOffer.CustomerSpecialOfferCustomerFilterRequest;
import com.petek.inventoryService.dto.customerSpecialOffer.CustomerSpecialOfferFilterRequest;
import com.petek.inventoryService.dto.customerSpecialOffer.CustomerSpecialOfferResponse;
import com.petek.inventoryService.dto.customerSpecialOffer.CustomerSpecialOfferUpdateRequest;
import com.petek.inventoryService.entity.CustomerSpecialOffer;
import com.petek.inventoryService.mapper.CustomerSpecialOfferMapper;
import com.petek.inventoryService.repository.CustomerRepository;
import com.petek.inventoryService.repository.CustomerSpecialOfferRepository;
import com.petek.inventoryService.spec.CustomerSpecialOfferSpecification;
import com.petek.inventoryService.utils.SortUtils;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerSpecialOfferService {
    
    private final CustomerSpecialOfferRepository repository;
    private final CustomerSpecialOfferMapper mapper;

    private final CustomerRepository customerRepository;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "specialOfferId", "customerId", "percentOff", "startDate", "endDate","updatedAt"
    );  
    
    /**
     * Get all products.
     */
    @Transactional(readOnly = true)
    public PageResponse<CustomerSpecialOfferResponse> getAllCustomerSpecialOffer(CustomerSpecialOfferFilterRequest request) {
        // Validate percent range
        if (request.getPercentGte() != null && request.getPercentLte() != null && 
            request.getPercentGte().compareTo(request.getPercentLte()) > 0) {
            throw new IllegalArgumentException("percent_gte cannot be greater than percent_lte");
        }

        // Validate start date range
        if (request.getStartGte() != null && request.getStartLte() != null && 
            request.getStartGte().isAfter(request.getStartLte())) {
            throw new IllegalArgumentException("start_gte cannot be after start_lte");
        }

        // Validate end date range
        if (request.getEndGte() != null && request.getEndLte() != null && 
            request.getEndGte().isAfter(request.getEndLte())) {
            throw new IllegalArgumentException("end_gte cannot be after end_lte");
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), SortUtils.createSort(request.getSort(), ALLOWED_SORT_FIELDS));
        Specification<CustomerSpecialOffer> spec = CustomerSpecialOfferSpecification.withFilters(request);

        Page<CustomerSpecialOffer> customerSpecialOfferPage = repository.findAll(spec, pageable);

        List<CustomerSpecialOfferResponse> customerSpecialOfferResponses = customerSpecialOfferPage.getContent()
            .stream()
            .map(mapper::toCustomerSpecialOfferResponse)
            .toList();
        
        PageInfo pageInfo = new PageInfo(
            customerSpecialOfferPage.getNumber(),
            customerSpecialOfferPage.getSize(),
            customerSpecialOfferPage.getTotalElements(),
            customerSpecialOfferPage.getTotalPages()
        );

        return new PageResponse<CustomerSpecialOfferResponse>(customerSpecialOfferResponses, pageInfo);
    }

    /**
     * Create customer special offer.
     */
    public CustomerSpecialOfferResponse createCustomerSpecialOffer(CustomerSpecialOfferCreateRequest request) {
        // Validate date range
        if (request.getStartDate() != null && request.getEndDate() != null &&
            request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("start_date cannot be after end_date");
        }

        // Validate customer
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

    /**
     * Update a customer special offer.
     */
    public CustomerSpecialOfferResponse updateCustomerSpecialOffer(Long customerSpecialOfferId, CustomerSpecialOfferUpdateRequest request) {
        // Validate date range
        if (request.getStartDate() != null && request.getEndDate() != null &&
            request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("start_date cannot be after end_date");
        }

        // Validate customer
        if (request.getCustomerId() != null) {
            customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + request.getCustomerId()));
        }

        CustomerSpecialOffer existingCustomerSpecialOffer = repository.findById(customerSpecialOfferId)
            .orElseThrow(() -> new EntityNotFoundException("Customer Special Offer not found with id: " + customerSpecialOfferId));

        Optional.ofNullable(request.getCustomerId())
            .ifPresent(existingCustomerSpecialOffer::setCustomerId);

        Optional.ofNullable(request.getPercentOff())
            .ifPresent(existingCustomerSpecialOffer::setPercentOff);

        Optional.ofNullable(request.getStartDate())
            .ifPresent(existingCustomerSpecialOffer::setStartDate);
        
        Optional.ofNullable(request.getEndDate())
            .ifPresent(existingCustomerSpecialOffer::setEndDate);

        existingCustomerSpecialOffer.setUpdatedAt(Instant.now());

        return mapper.toCustomerSpecialOfferResponse(repository.save(existingCustomerSpecialOffer));
    }

    /**
     * Delete a customer special offer.
     */
    public void deleteCustomerSpecialOfferById(Long customerSpecialOfferId) {
        CustomerSpecialOffer customerSpecialOffer = repository.findById(customerSpecialOfferId)
            .orElseThrow(() -> new EntityNotFoundException("Customer Special Offer not found with id: " + customerSpecialOfferId));
        repository.delete(customerSpecialOffer);
    }

    /**
     * Get all customer special offer with customer id.
     */
    public PageResponse<CustomerSpecialOfferResponse> getAllCustomerSpecialOfferByCustomerId(Long customerId, CustomerSpecialOfferCustomerFilterRequest request) {
        // Validate customer
        customerRepository.findById(customerId)
            .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + customerId));
            
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), SortUtils.createSort(request.getSort(), ALLOWED_SORT_FIELDS));
        Specification<CustomerSpecialOffer> spec = CustomerSpecialOfferSpecification.withFilters(customerId, request);

        Page<CustomerSpecialOffer> customerSpecialOfferPage = repository.findAll(spec, pageable);

        List<CustomerSpecialOfferResponse> customerSpecialOfferResponses = customerSpecialOfferPage.getContent()
            .stream()
            .map(mapper::toCustomerSpecialOfferResponse)
            .toList();
        
        PageInfo pageInfo = new PageInfo(
            customerSpecialOfferPage.getNumber(),
            customerSpecialOfferPage.getSize(),
            customerSpecialOfferPage.getTotalElements(),
            customerSpecialOfferPage.getTotalPages()
        );

        return new PageResponse<CustomerSpecialOfferResponse>(customerSpecialOfferResponses, pageInfo);
    }

}
