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
import com.petek.inventoryService.dto.ProductFilterRequest;
import com.petek.inventoryService.dto.ProductItemResponse;
import com.petek.inventoryService.dto.ProductCreateRequest;
import com.petek.inventoryService.dto.ProductResponse;
import com.petek.inventoryService.dto.ProductUpdateRequest;
import com.petek.inventoryService.service.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    /**
     * Get all products.
     */
    @GetMapping
    public ResponseEntity<PageResponse<ProductItemResponse>> getProducts(
        @ModelAttribute @Valid ProductFilterRequest request
    ) {
        return ResponseEntity.ok(service.getAllProducts(request));
    }

    /**
     * Create a new product.
     */
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
        @RequestBody @Valid ProductCreateRequest request
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
