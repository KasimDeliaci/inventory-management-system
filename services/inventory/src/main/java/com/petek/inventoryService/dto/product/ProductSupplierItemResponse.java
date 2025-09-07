package com.petek.inventoryService.dto.product;

import com.petek.inventoryService.dto.productSupplier.ProductSupplierResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSupplierItemResponse {
    private SupplierItem supplier;
    private ProductSupplierResponse link;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierItem {
        private Long supplierId;
        private String supplierName;
        private String city;
    }
}
