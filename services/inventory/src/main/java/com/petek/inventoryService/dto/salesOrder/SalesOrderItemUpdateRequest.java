package com.petek.inventoryService.dto.salesOrder;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesOrderItemUpdateRequest {
    @DecimalMin(value = "0.001", message = "Quantity must be greater than 0")
    @Digits(integer = 9, fraction = 3, message = "Quantity must have at most 9 integer digits and 3 decimal places")
    private BigDecimal quantity;
}
