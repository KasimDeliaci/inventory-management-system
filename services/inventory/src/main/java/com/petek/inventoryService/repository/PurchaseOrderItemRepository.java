package com.petek.inventoryService.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.petek.inventoryService.entity.PurchaseOrderItem;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {
    
    Page<PurchaseOrderItem> findByPurchaseOrderPurchaseOrderId(Long purchaseOrderId, Pageable pageable);

}
