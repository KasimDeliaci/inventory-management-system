package com.petek.inventory_service.mapper;

import org.springframework.stereotype.Service;

import com.petek.inventory_service.dto.ProductRequest;
import com.petek.inventory_service.dto.ProductResponse;
import com.petek.inventory_service.entity.Product;

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

    public ProductResponse toProductResponse(Product product) {
        return new ProductResponse(
            product.getProductId(),
            product.getProductName(),
            product.getDescription(),
            product.getCategory(),
            product.getUnitOfMeasure(),
            product.getSafetyStock(),
            product.getReorderPoint(),
            product.getCurrentPrice(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }
}
