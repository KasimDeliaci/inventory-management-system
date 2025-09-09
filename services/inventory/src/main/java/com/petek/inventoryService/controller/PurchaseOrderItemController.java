package com.petek.inventoryService.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petek.inventoryService.dto.purchaseOrderItem.PurchaseOrderItemResponse;
import com.petek.inventoryService.service.PurchaseOrderItemService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/purchase-order-items")
@RequiredArgsConstructor
public class PurchaseOrderItemController {
    
    private final PurchaseOrderItemService service;

    /**
     * Get purchase order item by id.
     */
    @GetMapping("/{purchaseOrderItemId}")
    public ResponseEntity<PurchaseOrderItemResponse> getPurchaseOrderItemById(
        @PathVariable Long purchaseOrderItemId
    ) {
        return ResponseEntity.ok(service.getPurchaseOrderItemById(purchaseOrderItemId));
    }

}
