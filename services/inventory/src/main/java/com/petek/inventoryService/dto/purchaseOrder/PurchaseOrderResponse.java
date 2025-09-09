package com.petek.inventoryService.dto.purchaseOrder;

import java.time.Instant;
import java.time.LocalDate;

import com.petek.inventoryService.entity.PurchaseOrder.PurchaseOrderStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderResponse {
    private Long purchaseOrderId;
    private Long supplierId;
    private LocalDate orderDate;
    private LocalDate expectedDelivery;
    private Instant actualDelivery;
    private PurchaseOrderStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
