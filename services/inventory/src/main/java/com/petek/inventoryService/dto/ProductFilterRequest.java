package com.petek.inventoryService.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProductFilterRequest(
    @NotNull
    @Min(0)
    Integer page,

    @NotNull
    @Min(1)
    Integer size,

    List<String> sort,
    String q,
    List<String> category,
    List<String> uom,
    BigDecimal priceGte,
    BigDecimal priceLte,
    Integer safetyGte,
    Integer safetyLte,
    Integer reorderGte,
    Integer reorderLte,
    LocalDateTime updatedAfter
) {
    public ProductFilterRequest {
        if (page == null) page = 0;
        if (size == null) size = 20;
        if (sort == null) sort = List.of("productId");
    }
}
