package com.petek.inventoryService.mapper;

import org.springframework.stereotype.Service;

import com.petek.inventoryService.dto.purchaseOrder.PurchaseOrderCreateRequest;
import com.petek.inventoryService.dto.purchaseOrder.PurchaseOrderResponse;
import com.petek.inventoryService.entity.PurchaseOrder;
import com.petek.inventoryService.entity.Supplier;

@Service
public class PurchaseOrderMapper {
    
    /**
     * Map PurchaseOrderCreateRequest to PurchaseOrder entity.
     */
    public PurchaseOrder toPurchaseOrder(PurchaseOrderCreateRequest request, Supplier supplier) {
        return PurchaseOrder.builder()
            .supplier(supplier)
            .orderDate(request.getOrderDate())
            .expectedDelivery(request.getExpectedDelivery())
            .build();
    }

    /**
     * Map PurchaseOrderCreateRequest to PurchaseOrder entity.
     */
    public PurchaseOrderResponse toPurchaseOrderResponse(PurchaseOrder purchaseOrder) {
        return PurchaseOrderResponse.builder()
            .purchaseOrderId(purchaseOrder.getPurchaseOrderId())
            .supplierId(purchaseOrder.getSupplier().getSupplierId())
            .orderDate(purchaseOrder.getOrderDate())
            .expectedDelivery(purchaseOrder.getExpectedDelivery())
            .actualDelivery(purchaseOrder.getActualDelivery())
            .status(purchaseOrder.getStatus())
            .createdAt(purchaseOrder.getCreatedAt())
            .updatedAt(purchaseOrder.getUpdatedAt())
            .build();
    }

}
