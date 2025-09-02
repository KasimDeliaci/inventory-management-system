package com.petek.inventoryService.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.petek.inventoryService.entity.Supplier;

public interface SupplierRepository extends JpaRepository<Supplier, Long>{
    
}
