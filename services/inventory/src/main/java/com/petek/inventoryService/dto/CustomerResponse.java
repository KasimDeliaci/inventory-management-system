package com.petek.inventoryService.dto;

import java.time.Instant;

import com.petek.inventoryService.entity.CustomerSegment;

public record CustomerResponse(
    Long customerId,
    String customerName,
    CustomerSegment customerSegment,
    String email,
    String phone,
    String city,
    Instant createdAt
) {}
