package com.petek.inventoryService.dto.productSupplier;

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
public class ProductSupplierFilterRequest {
    @NotNull
    @Min(0)
    @Builder.Default
    private Integer page = 0;

    @NotNull
    @Min(1)
    @Builder.Default
    private Integer size = 20;

    @Builder.Default
    private List<String> sort = List.of("productSupplierId");

    private List<BigDecimal> productId;
    private List<BigDecimal> supplierId;
    private Boolean active;
    private Boolean preferred;
    private Instant lastDeliverySince;
    private Instant updatedAfter;
}
