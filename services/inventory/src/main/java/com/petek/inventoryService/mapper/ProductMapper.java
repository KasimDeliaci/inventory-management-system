package com.petek.inventoryService.mapper;

import org.springframework.stereotype.Service;

import com.petek.inventoryService.dto.ProductCreateRequest;
import com.petek.inventoryService.dto.ProductResponse;
import com.petek.inventoryService.entity.Product;

@Service
public class ProductMapper {

    /**
     * Map ProductRequest to Product entity.
     */
    public Product toProduct(ProductCreateRequest request) {
        return Product.builder()
            .productName(request.productName())
            .description(request.description())
            .category(request.category())
            .unitOfMeasure(request.unitOfMeasure())
            .safetyStock(request.safetyStock())
            .reorderPoint(request.reorderPoint())
            .currentPrice(request.currentPrice())
            .build();
    }

    /**
     * Map Product entity to ProductResponse.
     */
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
