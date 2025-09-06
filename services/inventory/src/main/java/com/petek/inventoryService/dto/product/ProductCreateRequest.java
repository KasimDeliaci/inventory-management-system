package com.petek.inventoryService.dto.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreateRequest {
    @NotNull(message = "Product name is required")
    @Size(min = 2, max = 100, message = "Product name must be between 2 and 100 characters")
    private String productName;

    private String description;

    @NotNull(message = "Category is required")
    @Size(min = 2, max = 100, message = "Category must be between 2 and 100 characters")
    private String category;

    @NotNull(message = "Unit of measure is required")
    @Size(min = 2, max = 100, message = "Unit of measure must be between 2 and 100 characters")
    private String unitOfMeasure;

    @NotNull(message = "Safety stock is required")
    @Positive(message = "Safety stock should be positive")
    private BigDecimal safetyStock;

    @NotNull(message = "Reorder point is required")
    @Positive(message = "Reorder point should be positive")
    private BigDecimal reorderPoint;

    @NotNull(message = "Current price is required")
    @Positive(message = "Current price should be positive")
    private BigDecimal currentPrice;
}
