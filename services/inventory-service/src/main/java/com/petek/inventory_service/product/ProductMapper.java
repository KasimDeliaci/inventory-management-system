package com.petek.inventory_service.product;

import org.springframework.stereotype.Service;

@Service
public class ProductMapper {
    public Product toProduct(ProductRequest request) {
        return Product.builder()
            .productId(request.productId())
            .productName(request.productName())
            .description(request.description())
            .category(request.category())
            .unitOfMeasure(request.unitOfMeasure())
            .safetyStock(request.safetyStock())
            .reorderPoint(request.reorderPoint())
            .currentPrice(request.currentPrice())
            .build();
    }
}
