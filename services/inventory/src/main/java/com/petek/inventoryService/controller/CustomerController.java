package com.petek.inventoryService.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petek.inventoryService.dto.PageResponse;
import com.petek.inventoryService.dto.customer.CustomerCreateRequest;
import com.petek.inventoryService.dto.customer.CustomerFilterRequest;
import com.petek.inventoryService.dto.customer.CustomerResponse;
import com.petek.inventoryService.dto.customer.CustomerUpdateRequest;
import com.petek.inventoryService.service.CustomerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;

    /**
     * Get all products.
     */
    @GetMapping
    public ResponseEntity<PageResponse<CustomerResponse>> getCustomers(
        @ModelAttribute @Valid CustomerFilterRequest request
    ) {
        return ResponseEntity.ok(service.getCustomers(request));
    }

    /**
     * Create a new customer.
     */
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
        @RequestBody @Valid CustomerCreateRequest request
    ) {
        return ResponseEntity.status(201).body(service.createCustomer(request));
    }

    /**
     * Get a customer by ID.
     */
    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> getCustomerById(
        @PathVariable Long customerId
    ) {
        return ResponseEntity.ok(service.getCustomerById(customerId));
    }

    /**
     * Update a customer.
     */
    @PutMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> updateCustomer(
        @PathVariable Long customerId,
        @RequestBody @Valid CustomerUpdateRequest request
    ) {
        return ResponseEntity.ok(service.updateCustomer(customerId, request));
    }

    /**
     * Delete a customer.
     */
    @DeleteMapping("/{customerId}")
    public ResponseEntity<Void> deleteCustomer(
        @PathVariable Long customerId
    ) {
        service.deleteCustomer(customerId);
        return ResponseEntity.noContent().header("X-Delete-Description", "Deleted").build();
    }

}
