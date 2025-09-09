package com.petek.inventoryService.dto.product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

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

    private BigDecimal priceGte;
    private BigDecimal priceLte;
    private BigDecimal safetyGte;
    private BigDecimal safetyLte;
    private BigDecimal reorderGte;
    private BigDecimal reorderLte;
    private Instant updatedAfter;
}
