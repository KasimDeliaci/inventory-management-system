package com.petek.inventoryService.dto.stock;

import java.math.BigDecimal;

import com.petek.inventoryService.entity.StockMovement.MovementKind;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementCreateRequest {
    @NotNull(message = "Product ID cannot be null")
    @Positive(message = "Product ID must be a positive number")
    private Long productId;

    @NotNull(message = "Movement kind cannot be null")
    private MovementKind movementKind;

    @NotNull(message = "Quantity cannot be null")
    @DecimalMin(value = "0.001", message = "Quantity must be greater than 0")
    @Digits(integer = 9, fraction = 3, message = "Quantity must have maximum 9 integer digits and 3 decimal places")
    private BigDecimal quantity;
}
