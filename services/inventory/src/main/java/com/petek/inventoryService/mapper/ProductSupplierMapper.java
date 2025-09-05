package com.petek.inventoryService.mapper;

import org.springframework.stereotype.Service;

import com.petek.inventoryService.dto.ProductSupplierCreateRequest;
import com.petek.inventoryService.dto.ProductSupplierResponse;
import com.petek.inventoryService.entity.Product;
import com.petek.inventoryService.entity.ProductSupplier;
import com.petek.inventoryService.entity.Supplier;

@Service
public class ProductSupplierMapper {

    /**
     * Map ProductSupplierCreateRequest to ProductSupplier entity.
     */
    public ProductSupplier toProductSupplier(ProductSupplierCreateRequest request) {
        return ProductSupplier.builder()
            .product(Product.builder().productId(request.getProductId()).build())
            .supplier(Supplier.builder().supplierId(request.getSupplierId()).build())
            .minOrderQuantity(request.getMinOrderQuantity())
            .isPreferred(request.getIsPreferred())
            .active(request.getActive())
            .build();
    }

    /**
     * Map ProductSupplier entity to ProductSupplierResponse.
     */
    public ProductSupplierResponse toProductSupplierResponse(ProductSupplier productSupplier) {
        return ProductSupplierResponse.builder()
            .productSupplierId(productSupplier.getProductSupplierId())
            .productId(productSupplier.getProduct().getProductId())
            .supplierId(productSupplier.getSupplier().getSupplierId())
            .minOrderQuantity(productSupplier.getMinOrderQuantity())
            .isPreferred(productSupplier.getIsPreferred())
            .active(productSupplier.getActive())
            .avgDelayDays(productSupplier.getAvgDelayDays())
            .totalOrdersCount(productSupplier.getTotalOrdersCount())
            .delayedOrdersCount(productSupplier.getDelayedOrdersCount())
            .lastDeliveryDate(productSupplier.getLastDeliveryDate())
            .createdAt(productSupplier.getCreatedAt())
            .updatedAt(productSupplier.getUpdatedAt())
            .build();
    }

}
