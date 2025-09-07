package com.petek.inventoryService.dto.product;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

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
public class ProductGetSuppliersFilterRequest {
    @NotNull
    @Min(0)
    @Builder.Default
    private Integer page = 0;

    @NotNull
    @Min(1)
    @Builder.Default
    private Integer size = 20;

    @Builder.Default
    private List<String> sort = List.of("supplier");

    private Boolean active;
    private Boolean preferred;

    @JsonProperty("last_delivery_since")
    private Instant lastDeliverySince;

    @JsonProperty("updated_after")
    private Instant updatedAfter;
}
