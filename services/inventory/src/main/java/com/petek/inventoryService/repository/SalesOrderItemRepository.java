package com.petek.inventoryService.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.petek.inventoryService.entity.SalesOrderItem;

public interface SalesOrderItemRepository extends JpaRepository<SalesOrderItem, Long> {
    
}
