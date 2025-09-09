package com.petek.inventoryService.mapper;

import org.springframework.stereotype.Service;

import com.petek.inventoryService.dto.purchaseOrderItem.PurchaseOrderItemCreateRequest;
import com.petek.inventoryService.dto.purchaseOrderItem.PurchaseOrderItemResponse;
import com.petek.inventoryService.entity.Product;
import com.petek.inventoryService.entity.PurchaseOrderItem;

@Service
public class PurchaseOrderItemMapper {
 
    /**
     * Map PurchaseOrderItemCreateRequest to PurchaseOrderItem entity.
     */
    public PurchaseOrderItem toPurchaseOrderItem(PurchaseOrderItemCreateRequest request, Product product) {
        return PurchaseOrderItem.builder()
            .product(product)
            .quantityOrdered(request.getQuantityOrdered())
            .unitPrice(request.getUnitPrice())
            .build();
    }

    /**
     * Map
     */
    public PurchaseOrderItemResponse toPurchaseOrderItemItemResponse(PurchaseOrderItem purchaseOrderItem) {
        return PurchaseOrderItemResponse.builder()
            .purchaseOrderItemId(purchaseOrderItem.getPurchaseOrderItemId())
            .purchaseOrderId(purchaseOrderItem.getPurchaseOrder().getPurchaseOrderId())
            .productId(purchaseOrderItem.getProduct().getProductId())
            .quantityOrdered(purchaseOrderItem.getQuantityOrdered())
            .quantityReceived(purchaseOrderItem.getQuantityReceived())
            .unitPrice(purchaseOrderItem.getUnitPrice())
            .lineTotal(purchaseOrderItem.getLineTotal())
            .lineTotalReceived(purchaseOrderItem.getLineTotalReceived())
            .createdAt(purchaseOrderItem.getCreatedAt())
            .build();
    }

}
