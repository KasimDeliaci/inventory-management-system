package com.petek.inventoryService.dto.customer;

import java.time.Instant;

import com.petek.inventoryService.entity.Customer.CustomerSegment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {
    private Long customerId;
    private String customerName;
    private CustomerSegment customerSegment;
    private String email;
    private String phone;
    private String city;
    private Instant createdAt;
    private Instant updatedAt;
}
