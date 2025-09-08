package com.petek.inventoryService.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.petek.inventoryService.entity.StockMovement;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    
}
