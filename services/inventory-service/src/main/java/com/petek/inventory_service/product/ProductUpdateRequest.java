package com.petek.inventory_service.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.Positive;

public record ProductUpdateRequest (
    Long productId,

    String productName,
    String description,
    String category,
    String unitOfMeasure,

    @Positive(message = "Safety stock should be positive")
    BigDecimal safetyStock,

    @Positive(message = "Reorder point should be positive")
    BigDecimal reorderPoint,

    @Positive(message = "Current price should be positive")
    BigDecimal currentPrice
) {

}
