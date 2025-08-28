package com.petek.inventory_service.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProductUpdateRequest (
    Long productId,

    @Size(min = 2, max = 100, message = "Product name must be between 2 and 100 characters")
    String productName,

    String description,

    @Size(min = 2, max = 100, message = "Category must be between 2 and 100 characters")
    String category,

    @Size(min = 2, max = 100, message = "Unit of measure must be between 2 and 100 characters")
    String unitOfMeasure,

    @Positive(message = "Safety stock should be positive")
    BigDecimal safetyStock,

    @Positive(message = "Reorder point should be positive")
    BigDecimal reorderPoint,

    @Positive(message = "Current price should be positive")
    BigDecimal currentPrice
) {

}
