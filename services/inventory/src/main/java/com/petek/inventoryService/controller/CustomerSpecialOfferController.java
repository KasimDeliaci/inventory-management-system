package com.petek.inventoryService.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petek.inventoryService.dto.customerSpecialOffer.CustomerSpecialOfferCreateRequest;
import com.petek.inventoryService.dto.customerSpecialOffer.CustomerSpecialOfferResponse;
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
        return ResponseEntity.status(201).body(service.createSpecialOffer(request));
    }

}
