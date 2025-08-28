package com.petek.inventory_service.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProductRequest (
    Long productId,

    @NotNull(message = "Product name is required")
    @Size(min = 2, max = 100, message = "Product name must be between 2 and 100 characters")
    String productName,

    String description,

    @NotNull(message = "Category is required")
    @Size(min = 2, max = 100, message = "Category must be between 2 and 100 characters")
    String category,

    @NotNull(message = "Unit of measure is required")
    @Size(min = 2, max = 100, message = "Unit of measure must be between 2 and 100 characters")
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

