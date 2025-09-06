package com.petek.inventoryService.service;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petek.inventoryService.dto.ProductSupplierCreateRequest;
import com.petek.inventoryService.dto.ProductSupplierResponse;
import com.petek.inventoryService.dto.ProductSupplierUpdateRequest;
import com.petek.inventoryService.entity.ProductSupplier;
import com.petek.inventoryService.mapper.ProductSupplierMapper;
import com.petek.inventoryService.repository.ProductSupplierRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductSupplierService {
    
    private final ProductSupplierRepository repository;
    private final ProductService productService;
    private final SupplierService supplierService;
    private final ProductSupplierMapper mapper;

    /**
     * Create a new productSupplier.
     */
    public ProductSupplierResponse createProductSupplier(ProductSupplierCreateRequest request) {

        if (productService.getProductById(request.getProductId()) == null) {
            throw new EntityNotFoundException("Product not found with id: " + request.getProductId());
        }
        
        if (supplierService.getSupplierById(request.getSupplierId()) == null) {
            throw new EntityNotFoundException("Supplier not found with id: " + request.getSupplierId());
        }
        
        ProductSupplier productSupplier = mapper.toProductSupplier(request);
        productSupplier.setTotalOrdersCount(0);
        productSupplier.setDelayedOrdersCount(0);
        productSupplier.setCreatedAt(Instant.now());
        productSupplier.setUpdatedAt(Instant.now());
        
        ProductSupplier existingPreferred = repository.findPreferredSupplierByProductId(request.getProductId());

        if (existingPreferred != null) {
            if (Boolean.TRUE.equals(request.getIsPreferred())) {
                existingPreferred.setIsPreferred(false);
                existingPreferred.setUpdatedAt(Instant.now());
                repository.save(existingPreferred);
            }
        } else {
            productSupplier.setIsPreferred(true);
        }
        
        if (Boolean.FALSE.equals(productSupplier.getActive())) {
            productSupplier.setIsPreferred(false);
        }
        
        return mapper.toProductSupplierResponse(repository.save(productSupplier));
    }

    /**
     * Get a productSupplier by ID.
     */
    @Transactional(readOnly = true)
    public ProductSupplierResponse getProductSupplierById(Long productSupplierId) {
        return repository.findById(productSupplierId)
            .map(mapper::toProductSupplierResponse)
            .orElseThrow(() -> new EntityNotFoundException("ProductSupplier not found with id: " + productSupplierId));
    }

    /**
     * Update a productSupplier.
     */
    public ProductSupplierResponse updateProductSupplier(Long productSupplierId, ProductSupplierUpdateRequest request) {
        ProductSupplier existingProductSupplier = repository.findById(productSupplierId)
            .orElseThrow(() -> new EntityNotFoundException("ProductSupplier not found with id: " + productSupplierId));

        // Rule: Inactive productSuppliers cant be set as preferred or preferred productSuppliers cant be set as inactive
        if (Boolean.TRUE.equals(request.getIsPreferred() != null ? request.getIsPreferred() : existingProductSupplier.getIsPreferred()) 
            && Boolean.FALSE.equals(request.getActive() != null ? request.getActive() : existingProductSupplier.getActive())
        ) {
            throw new IllegalArgumentException("Cannot set inactive supplier as preferred or preferred supplier as inactive");
        }

        ProductSupplier existingPreferred = repository.findPreferredSupplierByProductId(existingProductSupplier.getProduct().getProductId());
        
        // Rules
        if (existingPreferred != null && existingPreferred.getProductSupplierId() == existingProductSupplier.getProductSupplierId()
            && Boolean.FALSE.equals(request.getIsPreferred())
        ) {
            throw new IllegalArgumentException("Cannot set preferred supplier as not preferred");
        } else if (existingPreferred != null && Boolean.TRUE.equals(request.getIsPreferred())) {
            existingPreferred.setIsPreferred(false);
            existingProductSupplier.setIsPreferred(true);
        } else if (existingPreferred == null) {
            existingProductSupplier.setIsPreferred(true);
        }

        Optional.ofNullable(request.getMinOrderQuantity())
        .ifPresent(existingProductSupplier::setMinOrderQuantity);
        
        Optional.ofNullable(request.getIsPreferred())
        .ifPresent(existingProductSupplier::setIsPreferred);
        
        Optional.ofNullable(request.getActive())
            .ifPresent(existingProductSupplier::setActive);

        existingProductSupplier.setUpdatedAt(Instant.now());
        existingPreferred.setUpdatedAt(Instant.now());

        repository.save(existingPreferred);

        return mapper.toProductSupplierResponse(repository.save(existingProductSupplier));
    }

    /**
     * Delete a productSupplier.
     */
    public void deleteProductSupplier(Long productSupplierId) {
        ProductSupplier existingProductSupplier = repository.findById(productSupplierId)
            .orElseThrow(() -> new EntityNotFoundException("ProductSupplier not found with id: " + productSupplierId));
        repository.delete(existingProductSupplier);
    }

}
