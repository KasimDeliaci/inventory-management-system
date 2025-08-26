package com.petek.inventory_service.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProductRequest (
    Long productId,

    @NotNull(message = "Product name is required")
    String productName,

    String description,

    @NotNull(message = "Category is required")
    String category,

    @NotNull(message = "Unit of measure is required")
    String unitOfMeasure,

    @NotNull(message = "Safety stock is required")
    @Positive(message = "Safety stock should be positive")
    BigDecimal safetyStock,

    @NotNull(message = "Reorder point is required")
    @Positive(message = "Reorder point should be positive")
    BigDecimal reorderPoint,

    @NotNull(message = "Current price is required")
    @Positive(message = "Current price should be positive")
    BigDecimal currentPrice
) {

}
