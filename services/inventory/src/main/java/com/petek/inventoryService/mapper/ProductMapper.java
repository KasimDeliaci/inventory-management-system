package com.petek.inventoryService.mapper;

import java.math.BigDecimal;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.petek.inventoryService.dto.product.ProductCreateRequest;
import com.petek.inventoryService.dto.product.ProductItemResponse;
import com.petek.inventoryService.dto.product.ProductResponse;
import com.petek.inventoryService.entity.Product;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductMapper {

    private SupplierMapper supplierMapper;

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
            .activeSuppliers(product.getProductSuppliers() != null ? 
                product.getProductSuppliers().stream()
                    .filter(ps -> ps.getActive())
                    .map(ps -> {
                        try {
                            return supplierMapper.toSupplierResponse(ps.getSupplier());
                        } catch (EntityNotFoundException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList() : null)
            .preferredSupplier(product.getProductSuppliers() != null ?
                product.getProductSuppliers().stream()
                    .filter(ps -> ps.getIsPreferred() && ps.getActive())
                    .findFirst()
                    .map(ps -> {
                        try {
                            return supplierMapper.toSupplierResponse(ps.getSupplier());
                        } catch (EntityNotFoundException e) {
                            return null;
                        }
                    })
                    .orElse(null) : null)
            .createdAt(product.getCreatedAt())
            .updatedAt(product.getUpdatedAt())
            .build();
    }

    /**
     * Map Product entity to ProductItemResponse.
     */
    public ProductItemResponse toProductItemResponse(Product product) {
        return ProductItemResponse.builder()
            .productId(product.getProductId())
            .productName(product.getProductName())
            .category(product.getCategory())
            .unitOfMeasure(product.getUnitOfMeasure())
            .quantityAvailable(BigDecimal.ZERO) // Placeholder, replace with actual available quantity
            .activeSuppliers(product.getProductSuppliers() != null ? 
                product.getProductSuppliers().stream()
                    .filter(ps -> ps.getActive())
                    .map(ps -> {
                        try {
                            var supplier = ps.getSupplier();
                            return ProductItemResponse.SupplierItem.builder()
                                .supplierId(supplier.getSupplierId())
                                .supplierName(supplier.getSupplierName())
                                .build();
                        } catch (EntityNotFoundException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList() : null)
            .preferredSupplier(product.getProductSuppliers() != null ?
                product.getProductSuppliers().stream()
                    .filter(ps -> ps.getIsPreferred() && ps.getActive())
                    .findFirst()
                    .map(ps -> {
                        try {
                            var supplier = ps.getSupplier();
                            return ProductItemResponse.SupplierItem.builder()
                                .supplierId(supplier.getSupplierId())
                                .supplierName(supplier.getSupplierName())
                                .build();
                        } catch (EntityNotFoundException e) {
                            return null;
                        }
                    })
                    .orElse(null) : null)
            .inventoryStatus(ProductItemResponse.InventoryStatus.GREEN) // Placeholder, replace with actual logic
            .build();
    }

}
