package com.petek.inventoryService.mapper;

import org.springframework.stereotype.Service;

import com.petek.inventoryService.dto.stock.CurrentStockResponse;
import com.petek.inventoryService.entity.CurrentStock;

@Service
public class CurrentStockMapper {
    
    /**
     * Map CurrentStock to CurrentStockResponse.
     */
    public CurrentStockResponse toCurrentStockResponse(CurrentStock currentStock) {
        return CurrentStockResponse.builder()
            .productId(currentStock.getProductId())
            .quantityOnHand(currentStock.getQuantityOnHand())
            .quantityReserved(currentStock.getQuantityReserved())
            .quantityAvailable(currentStock.getQuantityAvailable())
            .lastMovementId(currentStock.getLastMovement() != null ? currentStock.getLastMovement().getMovementId() : null)
            .lastUpdated(currentStock.getLastUpdated())
            .build();
    }

}
