package com.petek.inventoryService.dto.productSupplier;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSupplierResponse {
    private Long productSupplierId;
    private Long productId;
    private Long supplierId;
    private BigDecimal minOrderQuantity;
    private Boolean isPreferred;
    private Boolean active;
    private BigDecimal avgLeadTimeDays;
    private BigDecimal avgDelayDays;
    private Integer totalOrdersCount;
    private Integer delayedOrdersCount;
    private LocalDate lastDeliveryDate;
    private Instant createdAt;
    private Instant updatedAt;
}
