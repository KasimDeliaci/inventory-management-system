package com.petek.inventoryService.dto.purchaseOrderItem;

import java.math.BigDecimal;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderItemUpdateRequest {
    @PositiveOrZero(message = "Quantity received cannot be negative")
    private BigDecimal quantityReceived;

    @PositiveOrZero(message = "Unit price cannot be negative")
    private BigDecimal unitPrice;
}
