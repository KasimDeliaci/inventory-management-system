package com.petek.inventoryService.dto.purchaseOrder;

import java.time.Instant;

import com.petek.inventoryService.entity.PurchaseOrder.PurchaseOrderStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderUpdateRequest {
    private Instant actualDelivery;
    private PurchaseOrderStatus status;
}
