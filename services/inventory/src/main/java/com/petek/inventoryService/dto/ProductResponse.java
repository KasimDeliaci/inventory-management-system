package com.petek.inventoryService.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long productId;
    private String productName;
    private String description;
    private String category;
    private String unitOfMeasure;
    private BigDecimal safetyStock;
    private BigDecimal reorderPoint;
    private BigDecimal currentPrice;
    private List<SupplierResponse> suppliers;
    private Instant createdAt;
    private Instant updatedAt;
}
