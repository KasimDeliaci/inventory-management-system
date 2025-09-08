package com.petek.inventoryService.dto.stock;

import java.math.BigDecimal;
import java.time.Instant;

import com.petek.inventoryService.entity.StockMovement.MovementKind;
import com.petek.inventoryService.entity.StockMovement.MovementSource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementResponse {
    private Long movementId;
    private Long productId;
    private MovementKind movementKind;
    private MovementSource movementSource;
    private Long sourceId;
    private Long sourceItemId;
    private BigDecimal quantity;
    private Instant movementDate;
    private Instant createdAt;
}
