package com.petek.inventoryService.dto.saleOrder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.petek.inventoryService.entity.SalesOrder.SalesOrderStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesOrderResponse {
    private Long salesOrderId;
    private Long customerId;
    private LocalDate orderDate;
    private LocalDate deliveryDate;
    private Instant deliveredAt;
    private SalesOrderStatus status;
    private Long customerSpecialOfferId;
    private BigDecimal customerDiscountPctApplied;
    private Instant createdAt;
    private Instant updatedAt;
}
