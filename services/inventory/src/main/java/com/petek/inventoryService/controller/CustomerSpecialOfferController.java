package com.petek.inventoryService.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petek.inventoryService.dto.customerSpecialOffer.CustomerSpecialOfferCreateRequest;
import com.petek.inventoryService.dto.customerSpecialOffer.CustomerSpecialOfferResponse;
import com.petek.inventoryService.dto.customerSpecialOffer.CustomerSpecialOfferUpdateRequest;
import com.petek.inventoryService.service.CustomerSpecialOfferService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/customer-special-offers")
@RequiredArgsConstructor
public class CustomerSpecialOfferController {
    
    private final CustomerSpecialOfferService service;

    /**
     * Create customer special offer.
     */
    @PostMapping
    public ResponseEntity<CustomerSpecialOfferResponse> createCustomerSpecialOffer(
        @RequestBody @Valid CustomerSpecialOfferCreateRequest request
    ) {
        return ResponseEntity.status(201).body(service.createCustomerSpecialOffer(request));
    }

    /**
     * Get a customer special offer by id.
     */
    @GetMapping("/{specialOfferId}")
    public ResponseEntity<CustomerSpecialOfferResponse> getCustomerSpecialOfferById(
        @PathVariable Long specialOfferId
    ) {
        return ResponseEntity.ok(service.getCustomerSpecialOfferById(specialOfferId));
    }

    /**
     * Create customer special offer.
     */
    @PutMapping("/{specialOfferId}")
    public ResponseEntity<CustomerSpecialOfferResponse> updateCustomerSpecialOffer(
        @PathVariable Long specialOfferId,
        @RequestBody @Valid CustomerSpecialOfferUpdateRequest request
    ) {
        return ResponseEntity.ok(service.updateCustomerSpecialOffer(specialOfferId, request));
    }

    /**
     * Delete a customer special offer.
     */
    @DeleteMapping("/{specialOfferId}")
    public ResponseEntity<CustomerSpecialOfferResponse> deleteCustomerSpecialOfferById(
        @PathVariable Long specialOfferId
    ) {
        service.deleteCustomerSpecialOfferById(specialOfferId);
        return ResponseEntity.noContent().header("X-Delete-Description", "Deleted").build();
    }

}
