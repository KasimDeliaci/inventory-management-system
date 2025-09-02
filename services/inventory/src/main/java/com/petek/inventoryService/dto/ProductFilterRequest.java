package com.petek.inventoryService.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

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

    @JsonProperty("price_gte")
    BigDecimal priceGte,

    @JsonProperty("price_lte")
    BigDecimal priceLte,

    @JsonProperty("safety_gte")
    BigDecimal safetyGte,

    @JsonProperty("safety_lte")
    BigDecimal safetyLte,

    @JsonProperty("reorder_gte")
    BigDecimal reorderGte,

    @JsonProperty("reorder_lte")
    BigDecimal reorderLte,

    Instant updatedAfter
) {
    public ProductFilterRequest {
        if (page == null) page = 0;
        if (size == null) size = 20;
        if (sort == null) sort = List.of("productId");
    }
}
