package com.petek.inventoryService.dto.product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.petek.inventoryService.dto.supplier.SupplierResponse;

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
    private List<SupplierResponse> activeSuppliers;
    private SupplierResponse preferredSupplier;
    private Instant createdAt;
    private Instant updatedAt;
}
