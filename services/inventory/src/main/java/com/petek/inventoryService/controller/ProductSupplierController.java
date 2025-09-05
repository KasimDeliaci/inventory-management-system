package com.petek.inventoryService.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petek.inventoryService.dto.ProductSupplierCreateRequest;
import com.petek.inventoryService.dto.ProductSupplierResponse;
import com.petek.inventoryService.dto.ProductSupplierUpdateRequest;
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

    /**
     * Update a productSupplier.
     */
    @PutMapping("/{productSupplierId}")
    public ResponseEntity<ProductSupplierResponse> updateProductSupplier(
        @PathVariable Long productSupplierId,
        @RequestBody @Valid ProductSupplierUpdateRequest request
    ) {
        return ResponseEntity.ok(service.updateProductSupplier(productSupplierId, request));
    }

    /**
     * Delete a productSupplier.
     */
    @DeleteMapping("/{productSupplierId}")
    public ResponseEntity<Void> deleteProductSupplier(
        @PathVariable Long productSupplierId
    ) {
        service.deleteProductSupplier(productSupplierId);
        return ResponseEntity.noContent().header("X-Delete-Description", "Deleted").build();
    }


}
