package com.petek.inventoryService.dto.customerSpecialOffer;

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
public class CustomerSpecialOfferResponse {
    private Long specialOfferId;
    private Long customerId;
    private BigDecimal percentOff;
    private LocalDate startDate;
    private LocalDate endDate;
    private Instant createdAt;
    private Instant updatedAt;
}