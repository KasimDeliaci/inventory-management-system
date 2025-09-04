package com.petek.inventoryService.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSupplierCreateRequest {
    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Supplier ID is required")
    private Long supplierId;

    @NotNull(message = "Min order quantity is required")
    @Positive(message = "Min order quantity should be positive")
    @Builder.Default
    private BigDecimal minOrderQuantity = BigDecimal.ONE;

    @NotNull(message = "Is preferred flag is required")
    @Builder.Default
    private Boolean isPreferred = false;

    @NotNull(message = "Active status is required")
    @Builder.Default
    private Boolean active = true;
}
