package com.petek.inventoryService.dto.customerSpecialOffer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSpecialOfferFilterRequest {
    @NotNull
    @Min(0)
    @Builder.Default
    private Integer page = 0;

    @NotNull
    @Min(1)
    @Max(200)
    @Builder.Default
    private Integer size = 20;

    @Builder.Default
    private List<String> sort = List.of("specialOfferId");
    
    private List<Long> customerId;

    @DecimalMin(value = "0.0", inclusive = true)
    @DecimalMax(value = "100.0", inclusive = true)
    private BigDecimal percentGte;

    @DecimalMin(value = "0.0", inclusive = true)
    @DecimalMax(value = "100.0", inclusive = true)
    private BigDecimal percentLte;

    private LocalDate startGte;
    private LocalDate startLte;
    private LocalDate endGte;
    private LocalDate endLte;
    private LocalDate activeOn;
    private Instant updatedAfter;
}
