package com.petek.inventoryService.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.petek.inventoryService.dto.PageResponse;
import com.petek.inventoryService.dto.ProductFilterRequest;
import com.petek.inventoryService.dto.ProductRequest;
import com.petek.inventoryService.dto.ProductResponse;
import com.petek.inventoryService.dto.ProductUpdateRequest;
import com.petek.inventoryService.exception.UnknownQueryParamsException;
import com.petek.inventoryService.service.ProductService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;
    private static final Set<String> ALLOWED_PARAMS = Set.of(
        "page", "size", "sort", "q", "category", "uom", 
        "price_gte", "price_lte", "safety_gte", "safety_lte", 
        "reorder_gte", "reorder_lte", "updated_after"
    );

    /**
     * Utils
     */
    private void validateParameters(HttpServletRequest request) {
        Map<String, String[]> paramMap = request.getParameterMap();
        List<String> unknownParams = paramMap.keySet().stream()
            .filter(param -> !ALLOWED_PARAMS.contains(param))
            .collect(Collectors.toList());
            
        if (!unknownParams.isEmpty()) {
            throw new UnknownQueryParamsException(unknownParams);
        }
    }
    
    /**
     * Get all products.
     */
    @GetMapping
    public ResponseEntity<?> getProducts(
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "20") Integer size,
        @RequestParam(required = false) List<String> sort,
        @RequestParam(required = false) String q,
        @RequestParam(required = false) List<String> category,
        @RequestParam(required = false) List<String> uom,
        @RequestParam(name = "price_gte", required = false) BigDecimal priceGte,
        @RequestParam(name = "price_lte", required = false) BigDecimal priceLte,
        @RequestParam(name = "safety_gte", required = false) Integer safetyGte,
        @RequestParam(name = "safety_lte", required = false) Integer safetyLte,
        @RequestParam(name = "reorder_gte", required = false) Integer reorderGte,
        @RequestParam(name = "reorder_lte", required = false) Integer reorderLte,
        @RequestParam(name = "updated_after", required = false) LocalDateTime updatedAfter,
        HttpServletRequest request
    ) {
        // Validate unknown parameters
        validateParameters(request);
        
        ProductFilterRequest filterRequest = new ProductFilterRequest(
            page, size, sort, q, category, uom, priceGte, priceLte,
            safetyGte, safetyLte, reorderGte, reorderLte, updatedAfter
        );
        
        PageResponse<ProductResponse> response = service.getAllProducts(filterRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Create a new product.
     */
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
        @RequestBody @Valid ProductRequest request
    ) {
        return ResponseEntity.status(201).body(service.createProduct(request));
    }

    /**
     * Get a product by ID.
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProductById(
        @PathVariable Long productId
    ) {
        return ResponseEntity.ok(service.getProductById(productId));
    }

    /**
     * Update a product.
     */
    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponse> updateProduct(
        @PathVariable Long productId,
        @RequestBody @Valid ProductUpdateRequest request
    ) {
        return ResponseEntity.ok(service.updateProduct(productId, request));
    }

    /**
     * Delete a product.
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(
        @PathVariable Long productId
    ) {
        service.deleteProduct(productId);
        return ResponseEntity.noContent().header("X-Delete-Description", "Deleted").build();
    }

}
