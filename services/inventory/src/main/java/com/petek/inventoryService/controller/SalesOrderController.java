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
import com.petek.inventoryService.dto.salesOrder.SalesOrderCreateRequest;
import com.petek.inventoryService.dto.salesOrder.SalesOrderFilterRequest;
import com.petek.inventoryService.dto.salesOrder.SalesOrderItemCreateRequest;
import com.petek.inventoryService.dto.salesOrder.SalesOrderItemResponse;
import com.petek.inventoryService.dto.salesOrder.SalesOrderResponse;
import com.petek.inventoryService.dto.salesOrder.SalesOrderUpdateRequest;
import com.petek.inventoryService.service.SalesOrderItemService;
import com.petek.inventoryService.service.SalesOrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/sales-orders")
@RequiredArgsConstructor
public class SalesOrderController {
    
    private final SalesOrderService service;
    
    private final SalesOrderItemService salesOrderItemService;

    /**
     * Get all sales order.
     */
    @GetMapping
    public ResponseEntity<PageResponse<SalesOrderResponse>> getAllSalesOrder(
        @ModelAttribute @Valid SalesOrderFilterRequest request
    ) {
        return ResponseEntity.ok(service.getAllSalesOrders(request));
    }

    /**
     * Create a sales order.
     */
    @PostMapping
    public ResponseEntity<SalesOrderResponse> createSalesOrder(
        @RequestBody @Valid SalesOrderCreateRequest request
    ) {
        return ResponseEntity.status(201).body(service.createSalesOrder(request));
    }

    /**
     * Get a sales order by id.
     */
    @GetMapping("/{salesOrderId}")
    public ResponseEntity<SalesOrderResponse> getSalesOrderById(
        @PathVariable Long salesOrderId
    ) {
        return ResponseEntity.ok(service.getSalesOrderById(salesOrderId));
    }

    /**
     * Update a sales order.
     */
    @PutMapping("/{salesOrderId}")
    public ResponseEntity<SalesOrderResponse> updateSalesOrder(
        @PathVariable Long salesOrderId,
        @RequestBody @Valid SalesOrderUpdateRequest request
    ) {
        return ResponseEntity.ok(service.updateSalesOrder(salesOrderId, request));
    }

    /**
     * Delete a sales order.
     */
    @DeleteMapping("/{salesOrderId}")
    public ResponseEntity<Void> deleteSalesOrder(
        @PathVariable Long salesOrderId
    ) {
        service.deleteSalesOrder(salesOrderId);
        return ResponseEntity.noContent().header("X-Delete-Description", "Deleted").build();
    }

    /**
     * Create a sales order item.
     */
    @PostMapping("/{salesOrderId}/items")
    public ResponseEntity<SalesOrderItemResponse> createSalesOrderItem(
        @PathVariable Long salesOrderId,
        @RequestBody @Valid SalesOrderItemCreateRequest request
    ) {
        return ResponseEntity.status(201).body(salesOrderItemService.createSalesOrderItem(salesOrderId, request));
    }

}
