package com.petek.inventoryService.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petek.inventoryService.dto.saleOrder.SalesOrderCreateRequest;
import com.petek.inventoryService.dto.saleOrder.SalesOrderResponse;
import com.petek.inventoryService.service.SalesOrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/sales-orders")
@RequiredArgsConstructor
public class SalesOrderController {
    
    private final SalesOrderService service;

    /**
     * Create a sales order.
     */
    @PostMapping
    public ResponseEntity<SalesOrderResponse> createSalesOrder(
        @RequestBody @Valid SalesOrderCreateRequest request
    ) {
        return ResponseEntity.status(201).body(service.createSalesOrder(request));
    }

}
