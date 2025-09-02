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

import com.petek.inventoryService.dto.SupplierCreateRequest;
import com.petek.inventoryService.dto.SupplierResponse;
import com.petek.inventoryService.dto.SupplierUpdateRequest;
import com.petek.inventoryService.service.SupplierService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService service;

    /**
     * Create a new supplier.
     */
    @PostMapping
    public ResponseEntity<SupplierResponse> createSupplier(
        @RequestBody @Valid SupplierCreateRequest request
    ) {
        return ResponseEntity.status(201).body(service.createSupplier(request));
    }

    /**
     * Get a supplier by ID.
     */
    @GetMapping("/{supplierId}")
    public ResponseEntity<SupplierResponse> getSupplierById(
        @PathVariable Long supplierId
    ) {
        return ResponseEntity.ok(service.getSupplierById(supplierId));
    }

    /**
     * Update a supplier.
     */
    @PutMapping("/{supplierId}")
    public ResponseEntity<SupplierResponse> updateSupplier(
        @PathVariable Long supplierId,
        @RequestBody @Valid SupplierUpdateRequest request
    ) {
        return ResponseEntity.ok(service.updateSupplier(supplierId, request));
    }

    /**
     * Delete a supplier.
     */
    @DeleteMapping("/{supplierId}")
    public ResponseEntity<Void> deleteSupplier(
        @PathVariable Long supplierId
    ) {
        service.deleteSupplier(supplierId);
        return ResponseEntity.noContent().header("X-Delete-Description", "Deleted").build();
    }

}
