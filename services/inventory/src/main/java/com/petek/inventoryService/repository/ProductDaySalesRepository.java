package com.petek.inventoryService.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.petek.inventoryService.entity.ProductDaySales;
import com.petek.inventoryService.entity.ProductDaySalesId;

public interface ProductDaySalesRepository extends JpaRepository<ProductDaySales, ProductDaySalesId>, JpaSpecificationExecutor<ProductDaySales> {
    
}
