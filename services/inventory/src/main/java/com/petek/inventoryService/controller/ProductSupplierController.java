package com.petek.inventoryService.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petek.inventoryService.dto.ProductSupplierCreateRequest;
import com.petek.inventoryService.dto.ProductSupplierResponse;
import com.petek.inventoryService.service.ProductSupplierService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/product-suppliers")
@RequiredArgsConstructor
public class ProductSupplierController {
    
    private final ProductSupplierService service;

    /**
     * Create a new productSupplier.
     */
    @PostMapping
    public ResponseEntity<ProductSupplierResponse> createProductSupplier(
        @RequestBody @Valid ProductSupplierCreateRequest request
    ) {
        return ResponseEntity.ok(service.createProductSupplier(request));
    }

    /**
     * Get a productSupplier by ID.
     */
    @RequestMapping("/{productSupplierId}")
    public ResponseEntity<ProductSupplierResponse> getProductSupplierById(
        @PathVariable Long productSupplierId
    ) {
        return ResponseEntity.ok(service.getProductSupplierById(productSupplierId));
    }

}
