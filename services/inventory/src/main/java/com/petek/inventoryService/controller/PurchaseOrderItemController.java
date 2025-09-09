package com.petek.inventoryService.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petek.inventoryService.dto.purchaseOrderItem.PurchaseOrderItemResponse;
import com.petek.inventoryService.dto.purchaseOrderItem.PurchaseOrderItemUpdateRequest;
import com.petek.inventoryService.service.PurchaseOrderItemService;

import jakarta.validation.Valid;
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

    /**
     * Update purchase order item.
     */
    @PutMapping("/{purchaseOrderItemId}")
    public ResponseEntity<PurchaseOrderItemResponse> updatePurchaseOrderItemById(
        @PathVariable Long purchaseOrderItemId,
        @RequestBody @Valid PurchaseOrderItemUpdateRequest request
    ) {
        return ResponseEntity.ok(service.updatePurchaseOrderItem(purchaseOrderItemId, request));
    }

    /**
     * Delete purchase order item.
     */
    @DeleteMapping("/{purchaseOrderItemId}")
    public ResponseEntity<Void> deletePurchaseOrderItemById(
        @PathVariable Long purchaseOrderItemId
    ) {
        service.deletePurchaseOrderItem(purchaseOrderItemId);
        return ResponseEntity.noContent().header("X-Delete-Description", "Deleted").build();
    }

}
