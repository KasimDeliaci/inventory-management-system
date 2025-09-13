package com.petek.inventoryService.dto.salesOrder;

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
public class SalesOrderItemResponse {
    private Long salesOrderItemId;
    private Long salesOrderId;
    private Long productId;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountPercentage;
    private Long campaignId;
    private BigDecimal discountAmount;
    private BigDecimal lineTotal;
    private Instant createdAt;
}
