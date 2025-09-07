package com.petek.inventoryService.dto.supplier;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierResponse {
    private Long supplierId;
    private String supplierName;
    private String email;
    private String phone;
    private String city;
    private Instant createdAt;
    private Instant updatedAt;
}
