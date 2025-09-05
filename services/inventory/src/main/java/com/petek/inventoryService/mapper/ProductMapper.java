package com.petek.inventoryService.mapper;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.petek.inventoryService.dto.ProductCreateRequest;
import com.petek.inventoryService.dto.ProductResponse;
import com.petek.inventoryService.entity.Product;

import jakarta.persistence.EntityNotFoundException;

@Service
public class ProductMapper {


    private SupplierMapper supplierMapper = new SupplierMapper();

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
            .suppliers(product.getProductSuppliers() != null ? 
                product.getProductSuppliers().stream()
                    .map(ps -> {
                        try {
                        return supplierMapper.toResponse(ps.getSupplier());
                        } catch (EntityNotFoundException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList() : null)
            .createdAt(product.getCreatedAt())
            .updatedAt(product.getUpdatedAt())
            .build();
    }

}
