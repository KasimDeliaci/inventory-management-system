package com.petek.inventoryService.dto;

import com.petek.inventoryService.entity.CustomerSegment;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerUpdateRequest (
    @Size(min = 2, max = 100, message = "Customer name must be between 2 and 100 characters")
    String customerName,

    CustomerSegment customerSegment,

    @Email(message = "Email must be valid")
    String email,

    @Pattern(regexp = "^(\\+90|0)?[1-9][0-9]{9}$", message = "Phone number must be valid")
    String phone,

    @Size(min = 2, max = 100, message = "City must be between 2 and 100 characters")
    String city
) {}