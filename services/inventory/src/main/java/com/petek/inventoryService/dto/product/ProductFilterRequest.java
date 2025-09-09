package com.petek.inventoryService.dto.product;

import java.math.BigDecimal;
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
public class ProductFilterRequest {
    @NotNull
    @Min(0)
    @Builder.Default
    private Integer page = 0;

    @NotNull
    @Min(1)
    @Builder.Default
    private Integer size = 20;

    @Builder.Default
    private List<String> sort = List.of("productId");
    
    private String q;
    private List<String> category;
    private List<String> uom;

    @JsonProperty("price_gte")
    private BigDecimal priceGte;

    @JsonProperty("price_lte")
    private BigDecimal priceLte;

    @JsonProperty("safety_gte")
    private BigDecimal safetyGte;

    @JsonProperty("safety_lte")
    private BigDecimal safetyLte;

    @JsonProperty("reorder_gte")
    private BigDecimal reorderGte;

    @JsonProperty("reorder_lte")
    private BigDecimal reorderLte;

    @JsonProperty("updated_after")
    private Instant updatedAfter;
}
