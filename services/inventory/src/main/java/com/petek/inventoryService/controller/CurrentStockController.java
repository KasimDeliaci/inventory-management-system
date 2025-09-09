package com.petek.inventoryService.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petek.inventoryService.dto.PageResponse;
import com.petek.inventoryService.dto.stock.CurrentStockFilterRequest;
import com.petek.inventoryService.dto.stock.CurrentStockResponse;
import com.petek.inventoryService.service.CurrentStockService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/current-stock")
@RequiredArgsConstructor
public class CurrentStockController {
    
    private final CurrentStockService service;

    /**
     * Get all current stocks.
     */
    @GetMapping
    public ResponseEntity<PageResponse<CurrentStockResponse>> getAllCurrentStock(
        @ModelAttribute @Valid CurrentStockFilterRequest request
    ) {
        return ResponseEntity.ok(service.getAllCurrentStocks(request));
    }

}
