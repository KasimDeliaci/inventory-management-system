package com.petek.inventoryService.mapper;

import org.springframework.stereotype.Service;

import com.petek.inventoryService.dto.product.ProductSupplierItemResponse;
import com.petek.inventoryService.dto.product.ProductSupplierItemResponse.SupplierItem;
import com.petek.inventoryService.dto.productSupplier.ProductSupplierCreateRequest;
import com.petek.inventoryService.dto.productSupplier.ProductSupplierResponse;
import com.petek.inventoryService.dto.supplier.SupplierProductItemResponse;
import com.petek.inventoryService.dto.supplier.SupplierProductItemResponse.ProductItem;
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

    /**
     * Map ProductSupplier entity to ProductSupplierItemResponse.
     */
    public ProductSupplierItemResponse toProductSupplierItemResponse(ProductSupplier productSupplier) {
        return ProductSupplierItemResponse.builder()
            .supplier(SupplierItem.builder()
                .supplierId(productSupplier.getSupplier().getSupplierId())
                .supplierName(productSupplier.getSupplier().getSupplierName())
                .city(productSupplier.getSupplier().getCity())
                .build())
            .link(toProductSupplierResponse(productSupplier))
            .build();
    }

    /**
     * Map ProductSupplier entity to SupplierProductItemResponse.
     */
    public SupplierProductItemResponse toSupplierProductItemResponse(ProductSupplier productSupplier) {
        return SupplierProductItemResponse.builder()
            .product(ProductItem.builder()
                .productId(productSupplier.getProduct().getProductId())
                .productName(productSupplier.getProduct().getProductName())
                .category(productSupplier.getProduct().getCategory())
                .build())   
            .link(toProductSupplierResponse(productSupplier))
            .build();
    }

}
