package com.petek.inventoryService.dto.stock;

import java.time.Instant;
import java.util.List;

import com.petek.inventoryService.entity.StockMovement.MovementKind;
import com.petek.inventoryService.entity.StockMovement.MovementSource;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementFilterRequest {
    @NotNull
    @Min(0)
    @Builder.Default
    private Integer page = 0;

    @NotNull
    @Min(1)
    @Builder.Default
    private Integer size = 20;

    @Builder.Default
    private List<String> sort = List.of("movementId");
    
    private List<Long> productId;
    private MovementSource movementSource;
    private MovementKind movementKind;
    private Instant movementDateGte;
    private Instant movementDateLte;
    private Long sourceId;
    private Long sourceItemId;
    private Instant updatedAfter;
}
