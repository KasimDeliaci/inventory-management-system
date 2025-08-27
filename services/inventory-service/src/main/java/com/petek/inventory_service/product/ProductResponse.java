package com.petek.inventory_service.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
    Long productId,
    String productName,
    String description,
    String category,
    String unitOfMeasure,
    BigDecimal safetyStock,
    BigDecimal reorderPoint,
    BigDecimal currentPrice,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    
}
