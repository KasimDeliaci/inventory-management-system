package com.petek.inventoryService.dto;

import java.time.Instant;

public record SupplierResponse(
    Long supplierId,
    String supplierName,
    String email,
    String phone,
    String city,
    Instant createdAt
) {}
