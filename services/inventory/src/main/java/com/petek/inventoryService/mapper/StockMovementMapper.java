package com.petek.inventoryService.mapper;

import org.springframework.stereotype.Service;

import com.petek.inventoryService.dto.stock.StockMovementCreateRequest;
import com.petek.inventoryService.dto.stock.StockMovementResponse;
import com.petek.inventoryService.entity.Product;
import com.petek.inventoryService.entity.StockMovement;

@Service
public class StockMovementMapper {

    /**
     * Map StockMovementCreateRequest to StockMovement entity.
     */
    public StockMovement toStockMovement(StockMovementCreateRequest request, Product product) {
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
