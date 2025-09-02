package com.petek.inventoryService.dto;

import com.petek.inventoryService.entity.CustomerSegment;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerCreateRequest(
    @NotNull(message = "Customer name is required")
    @NotBlank(message = "Customer name must not be blank")
    @Size(min = 2, max = 100, message = "Customer name must be between 2 and 100 characters")
    String customerName,

    @NotNull(message = "Customer segment is required")
    CustomerSegment customerSegment,

    @NotNull(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,

    @NotNull(message = "Phone is required")
    @Pattern(regexp = "^(\\+90|0)?[1-9][0-9]{9}$", message = "Phone number must be valid")
    String phone,

    @NotNull(message = "City is required")
    @Size(min = 2, max = 100, message = "City must be between 2 and 100 characters")
    String city
) {}
