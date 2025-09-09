package com.petek.inventoryService.dto.product;

import java.math.BigDecimal;

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
public class ProductUpdateRequest {
    @Size(min = 2, max = 100, message = "Product name must be between 2 and 100 characters")
    private String productName;

    private String description;

    @Size(min = 2, max = 100, message = "Category must be between 2 and 100 characters")
    private String category;

    @Size(min = 2, max = 100, message = "Unit of measure must be between 2 and 100 characters")
    private String unitOfMeasure;

    @Positive(message = "Safety stock should be positive")
    private BigDecimal safetyStock;

    @Positive(message = "Reorder point should be positive")
    private BigDecimal reorderPoint;

    @Positive(message = "Current price should be positive")
    private BigDecimal currentPrice;
}
