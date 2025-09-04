package com.petek.inventoryService.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.petek.inventoryService.entity.ProductSupplier;

public interface ProductSupplierRepository extends JpaRepository<ProductSupplier, Long> {
    
}
