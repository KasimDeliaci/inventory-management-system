package com.petek.inventoryService.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petek.inventoryService.dto.reporting.ReportingRequest;
import com.petek.inventoryService.dto.reporting.ProductDaySalesResponse;
import com.petek.inventoryService.service.ReportingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/reporting")
@RequiredArgsConstructor
public class ReportingController {
    
    private final ReportingService service;

    /**
     * Get all product day sales.
     */
    @GetMapping("/product-day-sales")
    public ResponseEntity<List<ProductDaySalesResponse>> getAllProductDaySales(
        @ModelAttribute @Valid ReportingRequest request
    ) {
        return ResponseEntity.ok(service.getAllProductDaySale(request));
    }


}
