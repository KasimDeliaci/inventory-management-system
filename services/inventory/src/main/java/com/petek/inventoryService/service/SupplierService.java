package com.petek.inventoryService.service;

import java.time.Instant;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.petek.inventoryService.dto.SupplierCreateRequest;
import com.petek.inventoryService.dto.SupplierResponse;
import com.petek.inventoryService.dto.SupplierUpdateRequest;
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

    /**
     * Update a supplier.
     */
    public SupplierResponse updateSupplier(Long supplierId, SupplierUpdateRequest request) {
        Supplier existingSupplier = repository.findById(supplierId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found"));

        Optional.ofNullable(request.getSupplierName())
            .filter(name -> !name.trim().isEmpty())
            .ifPresent(existingSupplier::setSupplierName);

        Optional.ofNullable(request.getEmail())
            .filter(email -> !email.trim().isEmpty())
            .ifPresent(existingSupplier::setEmail);

        Optional.ofNullable(request.getPhone())
            .filter(phone -> !phone.trim().isEmpty())
            .ifPresent(existingSupplier::setPhone);

        Optional.ofNullable(request.getCity())
            .filter(city -> !city.trim().isEmpty())
            .ifPresent(existingSupplier::setCity);

        return mapper.toResponse(repository.save(existingSupplier));
    }

    /**
     * Delete a supplier.
     */
    public void deleteSupplier(Long supplierId) {
        Supplier existingSupplier = repository.findById(supplierId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found"));
        repository.delete(existingSupplier);
    }

}
