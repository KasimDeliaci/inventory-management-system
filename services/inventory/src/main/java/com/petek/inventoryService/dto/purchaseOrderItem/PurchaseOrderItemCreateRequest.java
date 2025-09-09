package com.petek.inventoryService.dto.purchaseOrderItem;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderItemCreateRequest {
    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Quantity Ordered is required")
    private BigDecimal quantityOrdered;

    @NotNull(message = "Unit Price is required")
    private BigDecimal unitPrice;
}
