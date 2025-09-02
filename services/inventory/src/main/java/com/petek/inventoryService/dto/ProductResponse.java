package com.petek.inventoryService.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
    Long productId,
    String productName,
    String description,
    String category,
    String unitOfMeasure,
    BigDecimal safetyStock,
    BigDecimal reorderPoint,
    BigDecimal currentPrice,
    Instant createdAt,
    Instant updatedAt
) {}
