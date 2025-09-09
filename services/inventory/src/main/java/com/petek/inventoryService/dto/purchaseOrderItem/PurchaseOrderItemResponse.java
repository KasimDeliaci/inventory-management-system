package com.petek.inventoryService.dto.purchaseOrderItem;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderItemResponse {
    private Long purchaseOrderItemId;
    private Long purchaseOrderId;
    private Long productId;
    private BigDecimal quantityOrdered;
    private BigDecimal quantityReceived;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
    private BigDecimal lineTotalReceived;
    private Instant createdAt;
}
