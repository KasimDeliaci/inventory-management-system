package com.petek.inventoryService.dto.stock;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    
    @JsonProperty("product_id")
    private List<Long> productId;

    @JsonProperty("movement_source")
    private MovementSource movementSource;

    @JsonProperty("movement_kind")
    private MovementKind movementKind;

    @JsonProperty("movement_date_gte")
    private Instant movementDateGte;

    @JsonProperty("movement_date_lte")
    private Instant movementDateLte;

    @JsonProperty("source_id")
    private Long sourceId;

    @JsonProperty("source_item_id")
    private Long sourceItemId;

    @JsonProperty("updated_after")
    private Instant updatedAfter;
}
