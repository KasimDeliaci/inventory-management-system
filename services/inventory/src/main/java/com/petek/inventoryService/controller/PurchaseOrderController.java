package com.petek.inventoryService.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petek.inventoryService.dto.PageResponse;
import com.petek.inventoryService.dto.purchaseOrder.PurchaseOrderCreateRequest;
import com.petek.inventoryService.dto.purchaseOrder.PurchaseOrderFilterRequest;
import com.petek.inventoryService.dto.purchaseOrder.PurchaseOrderResponse;
import com.petek.inventoryService.dto.purchaseOrder.PurchaseOrderUpdateRequest;
import com.petek.inventoryService.dto.purchaseOrderItem.PurchaseOrderItemCreateRequest;
import com.petek.inventoryService.dto.purchaseOrderItem.PurchaseOrderItemResponse;
import com.petek.inventoryService.service.PurchaseOrderItemService;
import com.petek.inventoryService.service.PurchaseOrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {
    
    private final PurchaseOrderService service;

    private final PurchaseOrderItemService purchaseOrderItemService;

    /**
     * Get all purchase order.
     */
    @GetMapping
    public ResponseEntity<PageResponse<PurchaseOrderResponse>> getAllPurchaseOrder(
        @ModelAttribute @Valid PurchaseOrderFilterRequest request
    ) {
        return ResponseEntity.ok(service.getAllPurchaseOrder(request));
    }

    /**
     * Create a new purchase order.
     */
    @PostMapping
    public ResponseEntity<PurchaseOrderResponse> createPurchaseOrder(
        @RequestBody @Valid PurchaseOrderCreateRequest request
    ) {
        return ResponseEntity.ok(service.createPurchaseOrder(request));
    }

    /**
     * Create a new purchase order item.
     */
    @PostMapping("/{purchaseOrderId}/items")
    public ResponseEntity<PurchaseOrderItemResponse> createPurchaseOrderItem(
        @PathVariable Long purchaseOrderId,
        @RequestBody @Valid PurchaseOrderItemCreateRequest request
    ) {
        return ResponseEntity.ok(purchaseOrderItemService.createPurchaseOrderItem(purchaseOrderId, request));
    }

    /**
     * Get a purchase order by id.
     */
    @GetMapping("/{purchaseOrderId}")
    public ResponseEntity<PurchaseOrderResponse> getPurchaseOrderById(
        @PathVariable Long purchaseOrderId
    ) {
        return ResponseEntity.ok(service.getPurchaseOrderById(purchaseOrderId));
    }

    /**
     * Update a purchase order.
     */
    @PutMapping("/{purchaseOrderId}")
    public ResponseEntity<PurchaseOrderResponse> updatePurchaseOrderById(
        @PathVariable Long purchaseOrderId,
        @RequestBody @Valid PurchaseOrderUpdateRequest request
    ) {
        return ResponseEntity.ok(service.updatePurchaseOrder(purchaseOrderId, request));
    }

    /**
     * Delete a purchase order.
     */
    @DeleteMapping("/{purchaseOrderId}")
    public ResponseEntity<Void> deletePurchaseOrderById(
        @PathVariable Long purchaseOrderId
    ) {
        service.deletePurchaseOrder(purchaseOrderId);
        return ResponseEntity.noContent().header("X-Delete-Description", "Deleted").build();
    }

}
