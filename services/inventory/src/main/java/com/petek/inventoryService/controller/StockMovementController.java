package com.petek.inventoryService.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petek.inventoryService.dto.stock.StockMovementCreateRequest;
import com.petek.inventoryService.dto.stock.StockMovementResponse;
import com.petek.inventoryService.service.StockMovementService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/stock-movements")
@RequiredArgsConstructor
public class StockMovementController {
    
    private final StockMovementService service;

    /**
     * Create a new stock movement.
     */
    @PostMapping
    public ResponseEntity<StockMovementResponse> createStockMovement(
        @RequestBody @Valid StockMovementCreateRequest request
    ) {
        return ResponseEntity.ok(service.createStockMovement(request));
    }

}
