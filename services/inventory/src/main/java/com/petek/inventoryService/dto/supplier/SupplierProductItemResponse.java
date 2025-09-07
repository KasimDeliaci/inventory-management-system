package com.petek.inventoryService.dto.supplier;

import com.petek.inventoryService.dto.productSupplier.ProductSupplierResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierProductItemResponse {
    private ProductItem product;
    private ProductSupplierResponse link;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductItem {
        private Long productId;
        private String productName;
        private String category;
    }
}
