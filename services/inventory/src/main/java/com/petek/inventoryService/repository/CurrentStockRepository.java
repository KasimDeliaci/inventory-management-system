package com.petek.inventoryService.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.petek.inventoryService.entity.CurrentStock;

public interface CurrentStockRepository extends JpaRepository<CurrentStock, Long>, JpaSpecificationExecutor<CurrentStock> {
    
}
