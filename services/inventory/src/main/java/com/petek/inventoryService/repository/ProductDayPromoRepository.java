package com.petek.inventoryService.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.petek.inventoryService.entity.ProductDayPromo;
import com.petek.inventoryService.entity.ReportingId;

public interface ProductDayPromoRepository extends JpaRepository<ProductDayPromo, ReportingId>, JpaSpecificationExecutor<ProductDayPromo> {
    
}
