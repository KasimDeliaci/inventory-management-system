package com.petek.inventoryService.service;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.petek.inventoryService.dto.SupplierCreateRequest;
import com.petek.inventoryService.dto.SupplierResponse;
import com.petek.inventoryService.entity.Supplier;
import com.petek.inventoryService.mapper.SupplierMapper;
import com.petek.inventoryService.repository.SupplierRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupplierService {
    
    private final SupplierRepository repository;
    private final SupplierMapper mapper;

    /**
     * Create a new supplier.
     */
    public SupplierResponse createSupplier(SupplierCreateRequest request) {
        Supplier supplier = mapper.toSupplier(request);
        supplier.setCreatedAt(Instant.now());
        return mapper.toResponse(repository.save(supplier));
    }

    /**
     * Get a supplier by ID.
     */
    public SupplierResponse getSupplierById(Long supplierId) {
        return repository.findById(supplierId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found with id: " + supplierId));
    }

}
