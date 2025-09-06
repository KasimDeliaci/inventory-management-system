package com.petek.inventoryService.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.petek.inventoryService.entity.ProductSupplier;

public interface ProductSupplierRepository extends JpaRepository<ProductSupplier, Long>, JpaSpecificationExecutor<ProductSupplier> {

    @Query("SELECT ps FROM ProductSupplier ps WHERE ps.product.productId = :productId AND ps.isPreferred = true")
    ProductSupplier findPreferredSupplierByProductId(@Param("productId") Long productId);
    
}
