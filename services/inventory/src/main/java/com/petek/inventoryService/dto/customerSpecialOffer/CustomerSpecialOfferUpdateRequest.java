package com.petek.inventoryService.dto.customerSpecialOffer;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSpecialOfferUpdateRequest {
    @Positive(message = "Customer ID should be positive")
    private Long customerId;

    @DecimalMin(value = "0.0", inclusive = true, message = "Percent off must be at least 0")
    @DecimalMax(value = "100.0", inclusive = true, message = "Percent off cannot exceed 100")
    private BigDecimal percentOff;

    private LocalDate startDate;

    private LocalDate endDate;
}