package com.petek.inventoryService.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductItemResponse {
    private Long productId;
    private String productName;
    private String category;
    private String unitOfMeasure;
    private BigDecimal quantityAvailable;
    private List<SupplierItem> activeSuppliers;
    private SupplierItem preferredSupplier;
    private InventoryStatus inventoryStatus;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierItem {
        private Long supplierId;
        private String supplierName;
    }

    public enum InventoryStatus {
        RED, YELLOW, GREEN
    }
}
