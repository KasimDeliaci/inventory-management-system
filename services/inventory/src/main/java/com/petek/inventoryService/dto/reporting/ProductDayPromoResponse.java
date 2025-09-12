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
public class ProductDayPromoResponse {
    private LocalDate date;
    private Long productId;
    private BigDecimal promoPct;
}
