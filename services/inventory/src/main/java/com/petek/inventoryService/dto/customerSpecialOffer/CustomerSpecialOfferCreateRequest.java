package com.petek.inventoryService.dto.customerSpecialOffer;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
public class CustomerSpecialOfferCreateRequest {
    @NotNull(message = "Customer ID is required")
    @Positive(message = "Customer ID should be positive")
    private Long customerId;

    @NotNull(message = "Percent off is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Percent off must be at least 0")
    @DecimalMax(value = "100.0", inclusive = true, message = "Percent off cannot exceed 100")
    private BigDecimal percentOff;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;
}