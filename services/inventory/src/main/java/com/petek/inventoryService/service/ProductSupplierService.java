package com.petek.inventoryService.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.petek.inventoryService.dto.ProductSupplierCreateRequest;
import com.petek.inventoryService.dto.ProductSupplierResponse;
import com.petek.inventoryService.entity.ProductSupplier;
import com.petek.inventoryService.mapper.ProductSupplierMapper;
import com.petek.inventoryService.repository.ProductSupplierRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductSupplierService {
    
    private final ProductSupplierRepository repository;
    private final ProductSupplierMapper mapper;

    /**
     * Create a new productSupplier.
     */
    public ProductSupplierResponse createProductSupplier(ProductSupplierCreateRequest request) {
        ProductSupplier productSupplier = mapper.toProductSupplier(request);
        productSupplier.setTotalOrdersCount(0);
        productSupplier.setDelayedOrdersCount(0);
        productSupplier.setCreatedAt(Instant.now());
        productSupplier.setUpdatedAt(Instant.now());
        return mapper.toProductSupplierResponse(repository.save(productSupplier));
    }

}
