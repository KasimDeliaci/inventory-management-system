package com.petek.inventoryService.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSupplierUpdateRequest {
    private BigDecimal minOrderQuantity;
    private Boolean isPreferred;
    private Boolean active;
}
