package com.petek.inventoryService.mapper;

import org.springframework.stereotype.Service;

import com.petek.inventoryService.dto.ProductCreateRequest;
import com.petek.inventoryService.dto.ProductResponse;
import com.petek.inventoryService.entity.Product;

@Service
public class ProductMapper {

    /**
     * Map ProductCreateRequest to Product entity.
     */
    public Product toProduct(ProductCreateRequest request) {
        return Product.builder()
            .productName(request.getProductName())
            .description(request.getDescription())
            .category(request.getCategory())
            .unitOfMeasure(request.getUnitOfMeasure())
            .safetyStock(request.getSafetyStock())
            .reorderPoint(request.getReorderPoint())
            .currentPrice(request.getCurrentPrice())
            .build();
    }

    /**
     * Map Product entity to ProductResponse.
     */
    public ProductResponse toProductResponse(Product product) {
        return ProductResponse.builder()
            .productId(product.getProductId())
            .productName(product.getProductName())
            .description(product.getDescription())
            .category(product.getCategory())
            .unitOfMeasure(product.getUnitOfMeasure())
            .safetyStock(product.getSafetyStock())
            .reorderPoint(product.getReorderPoint())
            .currentPrice(product.getCurrentPrice())
            .createdAt(product.getCreatedAt())
            .updatedAt(product.getUpdatedAt())
            .build();
    }

}
