package com.petek.inventoryService.dto.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DayOfferStatsResponse {
    private LocalDate date;
    private Integer activeOffersCount;
    private BigDecimal offerAvgPct;
    private BigDecimal offerMaxPct;
}
