package com.petek.inventoryService.mapper;

import org.springframework.stereotype.Service;

import com.petek.inventoryService.dto.stock.StockMovementCreateRequest;
import com.petek.inventoryService.dto.stock.StockMovementResponse;
import com.petek.inventoryService.entity.Product;
import com.petek.inventoryService.entity.StockMovement;
import com.petek.inventoryService.repository.ProductRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockMovementMapper {
    
    private final ProductRepository productRepository;

    /**
     * Map StockMovementCreateRequest to StockMovement entity.
     */
    public StockMovement toStockMovement(StockMovementCreateRequest request) {
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + request.getProductId()));

        return StockMovement.builder()
            .product(product)
            .movementKind(request.getMovementKind())
            .quantity(request.getQuantity())
            .build();
    }

    /**
     * Map ProductSupplier entity to ProductSupplierResponse.
     */
    public StockMovementResponse toStockMovementResponse(StockMovement stockMovement) {
        return StockMovementResponse.builder()
            .movementId(stockMovement.getMovementId())
            .productId(stockMovement.getProduct().getProductId())
            .movementKind(stockMovement.getMovementKind())
            .movementSource(stockMovement.getMovementSource())
            .sourceId(stockMovement.getSourceId())
            .sourceItemId(stockMovement.getSourceItemId())
            .quantity(stockMovement.getQuantity())
            .movementDate(stockMovement.getMovementDate())
            .createdAt(stockMovement.getCreatedAt())
            .build();
    }

}   
