package com.petek.inventoryService.service;

import java.time.Instant;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.petek.inventoryService.dto.ProductSupplierCreateRequest;
import com.petek.inventoryService.dto.ProductSupplierResponse;
import com.petek.inventoryService.dto.ProductSupplierUpdateRequest;
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

        ProductSupplier existsPreferred = repository.findPreferredSupplierByProductId(request.getProductId());
        if (existsPreferred != null) {
            if (Boolean.TRUE.equals(request.getIsPreferred())) {
                existsPreferred.setIsPreferred(false);
            }
        }
        else {
            productSupplier.setIsPreferred(true);
        }
        if (Boolean.FALSE.equals(productSupplier.getActive())) productSupplier.setIsPreferred(false);

        productSupplier.setTotalOrdersCount(0);
        productSupplier.setDelayedOrdersCount(0);
        productSupplier.setCreatedAt(Instant.now());
        productSupplier.setUpdatedAt(Instant.now());

        ProductSupplier saved = repository.save(productSupplier);

        if (existsPreferred != null && Boolean.TRUE.equals(saved.getIsPreferred())) {
            repository.save(existsPreferred);
        }

        return mapper.toProductSupplierResponse(saved);
    }

    /**
     * Get a productSupplier by ID.
     */
    @Transactional(readOnly = true)
    public ProductSupplierResponse getProductSupplierById(Long productSupplierId) {
        return repository.findById(productSupplierId)
            .map(mapper::toProductSupplierResponse)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ProductSupplier not found with id: " + productSupplierId));
    }

    /**
     * Update a productSupplier.
     */
    public ProductSupplierResponse updateProductSupplier(Long productSupplierId, ProductSupplierUpdateRequest request) {
        ProductSupplier existingProductSupplier = repository.findById(productSupplierId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ProductSupplier not found"));

        Optional.ofNullable(request.getMinOrderQuantity())
            .ifPresent(existingProductSupplier::setMinOrderQuantity);

        Optional.ofNullable(request.getIsPreferred())
            .ifPresent(existingProductSupplier::setIsPreferred);

        Optional.ofNullable(request.getActive())
            .ifPresent(existingProductSupplier::setActive);

        existingProductSupplier.setUpdatedAt(Instant.now());

        return mapper.toProductSupplierResponse(repository.save(existingProductSupplier));
    }

    /**
     * Delete a productSupplier.
     */
    public void deleteProductSupplier(Long productSupplierId) {
        ProductSupplier existingProductSupplier = repository.findById(productSupplierId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ProductSupplier not found"));
        repository.delete(existingProductSupplier);
    }

}
