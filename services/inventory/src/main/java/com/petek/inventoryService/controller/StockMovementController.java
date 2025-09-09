package com.petek.inventoryService.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petek.inventoryService.dto.PageResponse;
import com.petek.inventoryService.dto.stock.StockMovementCreateRequest;
import com.petek.inventoryService.dto.stock.StockMovementFilterRequest;
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
     * Get all stock movements.
     */
    @GetMapping
    public ResponseEntity<PageResponse<StockMovementResponse>> getAllStockMovements(
        @ModelAttribute @Valid StockMovementFilterRequest request
    ) {
        return ResponseEntity.ok(service.getAllStockMovements(request));
    }

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
